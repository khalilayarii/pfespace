package com.reservation.pfeespace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservation.pfeespace.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatbotService {

    @Autowired
    private RagService ragService;

    @Autowired
    private AdminStatsService adminStatsService;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model}")
    private String groqModel;

    // === Config Mistral (fallback) ===
    @Value("${mistral.api.key}")
    private String mistralApiKey;

    @Value("${mistral.api.url}")
    private String mistralApiUrl;

    @Value("${mistral.model}")
    private String mistralModel;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, List<Map<String, Object>>> conversationHistory = new ConcurrentHashMap<>();

    private static final int MAX_TOOL_ITERATIONS = 5;
    private static final int MAX_RETRIES_429 = 2; // retries sur Groq AVANT de basculer sur Mistral
    private static final long MAX_BACKOFF_MILLIS = 3000; // jamais attendre plus de 3s avant de basculer
    // ==========================================
    //  POINT D'ENTRÉE PRINCIPAL — ROUTAGE
    // ==========================================

    public String ask(String userMessage, String identifiant, Role role) {
        System.out.println("[CHATBOT-DEBUG] identifiant = '" + identifiant + "' | role (JWT) = " + role);

        if (role == Role.ADMIN) {
            System.out.println("[CHATBOT-DEBUG] -> Routage vers askAdmin()");
            return askAdmin(userMessage, identifiant);
        }
        boolean isLoggedIn = (role == Role.CLIENT);
        System.out.println("[CHATBOT-DEBUG] -> Routage vers askClientOuVisiteur() | isLoggedIn=" + isLoggedIn);
        return askClientOuVisiteur(userMessage, identifiant, isLoggedIn);
    }

    // ==========================================
    //  FLUX CLIENT / VISITEUR
    // ==========================================

    private String askClientOuVisiteur(String userMessage, String identifiant, boolean isLoggedIn) {
        if (isAdminOnlyQuestion(userMessage)) {
            return "Cette information concerne les statistiques globales de la plateforme "
                    + "et est réservée aux administrateurs. Je peux par contre t'aider à trouver "
                    + "une salle ou vérifier les disponibilités !";
        }

        String base = (identifiant != null && !identifiant.isBlank()) ? identifiant : "anonymous";
        String sessionKey = (isLoggedIn ? "client:" : "visiteur:") + base;

        List<Map<String, Object>> history = conversationHistory
                .computeIfAbsent(sessionKey, k -> new ArrayList<>());

        history.add(textMessage("user", userMessage));

        String cumulativeUserText = buildCumulativeUserText(history);

        String ragContext;
        try {
            String emailPourReservations = isLoggedIn ? identifiant : null;
            ragContext = ragService.retrieveRelevantContext(cumulativeUserText, emailPourReservations, isLoggedIn);
        } catch (Exception e) {
            ragContext = "";
        }

        String systemPrompt = buildClientSystemPrompt(ragContext, isLoggedIn);

        int maxHistory = isLoggedIn ? 24 : 16;
        if (history.size() > maxHistory) {
            List<Map<String, Object>> trimmed = new ArrayList<>(
                    history.subList(history.size() - maxHistory, history.size())
            );
            conversationHistory.put(sessionKey, trimmed);
            history = trimmed;
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(textMessage("system", systemPrompt));
        messages.addAll(history);

        Map<String, Object> body = new HashMap<>();
        body.put("model", groqModel);
        body.put("messages", messages);
        body.put("temperature", 0.6);
        body.put("max_tokens", 700);

        try {
            Map<?, ?> responseBody = callLLMWithFallback(body);
            String assistantReply = extractMessageContent(responseBody);
            if (assistantReply == null) assistantReply = "Aucune réponse générée. Veuillez réessayer.";

            history.add(textMessage("assistant", assistantReply));
            return assistantReply;

        } catch (HttpClientErrorException e) {
            return handleLLMError(e);
        } catch (Exception e) {
            return "Erreur technique : " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String buildCumulativeUserText(List<Map<String, Object>> history) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> msg : history) {
            if ("user".equals(msg.get("role"))) {
                Object content = msg.get("content");
                if (content != null) {
                    sb.append(content.toString()).append(" ");
                }
            }
        }
        return sb.toString().trim();
    }

    private boolean isAdminOnlyQuestion(String message) {
        if (message == null) return false;
        String m = message.toLowerCase()
                .replace("é", "e").replace("è", "e").replace("ê", "e")
                .replace("à", "a").replace("ç", "c");

        boolean parleDeCapacite = m.contains("personne") || m.contains("pers.")
                || m.contains("capacite") || m.contains("place");
        if (parleDeCapacite) return false;

        boolean parleUtilisateurs = m.contains("utilisateur") || m.contains("user")
                || m.contains("client inscrit") || m.contains("combien de client");

        boolean parleStatsReservations =
                (m.contains("reservation") || m.contains("reservations"))
                        && (m.contains("confirme") || m.contains("refuse") || m.contains("en attente")
                        || m.contains("total") || m.contains("combien de reservation")
                        || m.contains("nombre de reservation") || m.contains("statistique"));

        return parleUtilisateurs || parleStatsReservations;
    }

    private String buildClientSystemPrompt(String ragContext, boolean isLoggedIn) {
        StringBuilder p = new StringBuilder();

        p.append("Tu es l'assistant IA de PfeSpace ")
                .append("(plateforme de réservation de salles de Startup Village, Medianet, Tunis).\n\n");

        p.append("## STATUT DE L'UTILISATEUR\n");
        if (isLoggedIn) {
            p.append("Cet utilisateur a un compte et est connecté. Tu peux lui proposer une "
                    + "recommandation de salle personnalisée (étape 9 ci-dessous).\n\n");
        } else {
            p.append("Cet utilisateur est un VISITEUR, il n'a PAS encore de compte. Il peut poser toutes "
                    + "les mêmes questions qu'un client (salles, équipements, prix, disponibilité, comment "
                    + "réserver) mais tu NE DOIS PAS lui faire de recommandation IA personnalisée : "
                    + "si les données système ci-dessous indiquent qu'il manque des informations pour "
                    + "recommander une salle, ne pose pas ces questions une par une comme pour un client. "
                    + "Explique-lui simplement que pour recevoir une recommandation personnalisée et "
                    + "finaliser une réservation, il doit créer un compte gratuit (bouton 'Se connecter / "
                    + "S'inscrire'), et propose-lui en attendant de lui montrer les salles disponibles.\n\n");
        }

        p.append("## TON RÔLE PRINCIPAL\n")
                .append("Tu dois accompagner l'utilisateur, comme le ferait un excellent conseiller commercial : "
                        + "tu poses les bonnes questions au bon moment, tu t'adaptes à ce qu'il a déjà dit, et tu "
                        + "ne lui imposes jamais une liste de questions d'un coup.\n\n");

        p.append("## RÈGLE DE CONVERSATION LA PLUS IMPORTANTE\n")
                .append("- Pose au maximum 1 à 2 questions par message, jamais plus.\n")
                .append("- Avant de poser une question, vérifie dans l'historique de la conversation si "
                        + "l'utilisateur a déjà donné cette information. Si oui, NE LA REDEMANDE PAS.\n")
                .append("- Si l'utilisateur pose une question hors sujet (info générale, dispo d'une salle "
                        + "précise, etc.), réponds-y directement puis reviens naturellement au sujet.\n")
                .append("- Reste naturel et chaleureux, des réponses de quelques lignes, pas un pavé de texte.\n\n");

        p.append("## ÉTAPES À COUVRIR (uniquement si l'utilisateur est connecté — sinon reste sur des infos générales)\n")
                .append("1. Intention : réservation / info sur les salles / modifier ou annuler une réservation.\n")
                .append("2. Besoin : type d'événement (réunion, formation, conférence, anniversaire, etc.).\n")
                .append("3. Date et créneau : date souhaitée, heure de début, heure de fin / durée.\n")
                .append("4. Capacité : nombre de participants attendus.\n")
                .append("5. Préférences de salle : équipements importants selon le type d'événement.\n")
                .append("6. Équipements : regroupe en 1-2 questions intelligentes selon le type d'événement.\n")
                .append("7. Budget : budget approximatif, économique ou premium.\n")
                .append("8. (Visiteur : t'arrêtes ici et l'invites à créer un compte.)\n")
                .append("9. Recommandation IA (uniquement si connecté) : une fois assez d'informations réunies, "
                        + "croise-les avec les données système ci-dessous et recommande UNE salle précise en "
                        + "expliquant pourquoi (capacité, équipements, prix). Si le créneau demandé n'est pas "
                        + "disponible, propose une alternative.\n")
                .append("10. Confirmation (si connecté) : récapitule salle + date + heure + capacité + "
                        + "équipements + prix, et demande une confirmation explicite.\n\n");

        p.append("## CE QUE TU NE PEUX JAMAIS FAIRE\n")
                .append("- Tu ne peux PAS enregistrer ou confirmer toi-même une réservation dans le système.\n")
                .append("- Une fois les infos réunies (utilisateur connecté), dis-lui que sa demande va être "
                        + "finalisée sur la page **Salles / Réservation**, où il pourra valider définitivement.\n\n");

        p.append("## RÈGLES ABSOLUES SUR LA DISPONIBILITÉ — NE JAMAIS VIOLER\n")
                .append("1. Si les données montrent qu'un créneau est OCCUPÉ/CONFIRMÉ, tu DOIS répondre NON, "
                        + "c'est impossible. JAMAIS dire que c'est possible.\n")
                .append("2. Avant de dire qu'une salle est disponible, vérifie OBLIGATOIREMENT les données "
                        + "système. Si un créneau occupé existe → ❌ impossible, propose une alternative.\n")
                .append("3. Si un créneau est libre, tu peux poursuivre normalement.\n\n");

        p.append("## RÈGLE ABSOLUE ANTI-INVENTION\n")
                .append("Tu ne dois JAMAIS inventer un nom de salle. Utilise EXCLUSIVEMENT les noms de salles "
                        + "qui apparaissent explicitement dans les DONNÉES SYSTÈME ci-dessous.\n\n");

        p.append("## AUTRES RÈGLES\n")
                .append("- Réponds toujours dans la langue de l'utilisateur (français ou arabe).\n")
                .append("- Date d'aujourd'hui : ")
                .append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");

        p.append("## DONNÉES SYSTÈME EN TEMPS RÉEL (salles, équipements, réservations existantes)\n")
                .append(ragContext);

        return p.toString();
    }

    // ==========================================
    //  FLUX ADMIN — RAG (salles) + FUNCTION CALLING (stats)
    // ==========================================

    private String askAdmin(String userMessage, String userEmail) {
        String sessionKey = "admin:" + ((userEmail != null && !userEmail.isEmpty()) ? userEmail : "anonymous");

        List<Map<String, Object>> history = conversationHistory
                .computeIfAbsent(sessionKey, k -> new ArrayList<>());

        history.add(textMessage("user", userMessage));

        if (history.size() > 12) {
            List<Map<String, Object>> trimmed = new ArrayList<>(
                    history.subList(history.size() - 12, history.size())
            );
            conversationHistory.put(sessionKey, trimmed);
            history = trimmed;
        }

        // === NOUVEAU : contexte RAG des salles, comme pour le client ===
        // isLoggedIn=true pour que l'admin ait aussi le contexte complet (recommandation incluse
        // si jamais il pose ce type de question, même si ce n'est pas son usage principal).
        String cumulativeUserText = buildCumulativeUserText(history);
        String ragContext;
        try {
            ragContext = ragService.retrieveRelevantContext(cumulativeUserText, null, true);
        } catch (Exception e) {
            ragContext = "";
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(textMessage("system", buildAdminSystemPrompt(ragContext)));
        messages.addAll(history);

        try {
            boolean forceTool = seemsLikeStatQuestion(userMessage);
            String finalReply = runAdminToolLoop(messages, forceTool);
            history.add(textMessage("assistant", finalReply));
            return finalReply;
        } catch (HttpClientErrorException e) {
            return handleLLMError(e);
        } catch (Exception e) {
            return "Erreur technique : " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String runAdminToolLoop(List<Map<String, Object>> messages, boolean forceToolOnFirstCall) {
        for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
            Map<String, Object> body = new HashMap<>();
            body.put("model", groqModel);
            body.put("messages", messages);
            body.put("tools", buildAdminTools());
            body.put("tool_choice", (i == 0 && forceToolOnFirstCall) ? "required" : "auto");
            body.put("temperature", 0.3);
            body.put("max_tokens", 700);

            Map<?, ?> responseBody = callLLMWithFallback(body);
            List<?> choices = (List<?>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "Aucune réponse générée. Veuillez réessayer.";
            }

            Map<?, ?> messageMap = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
            List<?> toolCalls = (List<?>) messageMap.get("tool_calls");

            if (toolCalls == null || toolCalls.isEmpty()) {
                Object content = messageMap.get("content");
                return content != null ? content.toString() : "Je n'ai pas pu générer de réponse.";
            }

            Map<String, Object> assistantMsg = new HashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", messageMap.get("content"));
            assistantMsg.put("tool_calls", toolCalls);
            messages.add(assistantMsg);

            for (Object tc : toolCalls) {
                Map<String, Object> toolCall = (Map<String, Object>) tc;
                String toolCallId = (String) toolCall.get("id");
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String name = (String) function.get("name");
                String argsJson = (String) function.get("arguments");

                String resultJson = executeAdminTool(name, argsJson);

                Map<String, Object> toolResultMsg = new HashMap<>();
                toolResultMsg.put("role", "tool");
                toolResultMsg.put("tool_call_id", toolCallId);
                toolResultMsg.put("content", resultJson);
                messages.add(toolResultMsg);
            }
        }
        return "Je n'ai pas réussi à obtenir une réponse complète, peux-tu reformuler ta question ?";
    }

    @SuppressWarnings("unchecked")
    private String executeAdminTool(String name, String argsJson) {
        try {
            Map<String, Object> args = (argsJson == null || argsJson.isBlank())
                    ? new HashMap<>()
                    : objectMapper.readValue(argsJson, Map.class);

            Object result;
            switch (name) {
                case "countReservationsByStatut":
                    result = adminStatsService.countReservationsByStatut((String) args.get("statut"));
                    break;
                case "countTotalReservations":
                    result = adminStatsService.countTotalReservations();
                    break;
                case "countUsers":
                    result = adminStatsService.countUsers();
                    break;
                case "countUsersByRole":
                    result = adminStatsService.countUsersByRole((String) args.get("role"));
                    break;
                case "countReservationsByPeriode":
                    result = adminStatsService.countReservationsByPeriode(
                            (String) args.get("dateDebut"), (String) args.get("dateFin"));
                    break;
                case "countReservationsBySalle":
                    result = adminStatsService.countReservationsBySalle(
                            (String) args.get("nomSalle"),
                            (String) args.get("statut"),
                            (String) args.get("dateDebut"),
                            (String) args.get("dateFin"));
                    break;
                case "repartitionReservationsParSalle":
                    result = adminStatsService.repartitionReservationsParSalle();
                    break;
                case "countReservations":
                    result = adminStatsService.countReservations(
                            (String) args.get("statut"),
                            (String) args.get("dateDebut"),
                            (String) args.get("dateFin"));
                    break;
                case "countSallesDisponibles":
                    result = adminStatsService.countSallesDisponibles();
                    break;
                // === NOUVEAU : taux de confirmation/refus/attente en % ===
                case "tauxReservations":
                    result = adminStatsService.tauxReservations(
                            (String) args.get("dateDebut"),
                            (String) args.get("dateFin"));
                    break;
                // === NOUVEAU : répartition par nature de manifestation ===
                case "statsParNatureManifestation":
                    result = adminStatsService.statsParNatureManifestation(
                            (String) args.get("dateDebut"),
                            (String) args.get("dateFin"),
                            (String) args.get("statut"));
                    break;
                // === NOUVEAU : statistiques mensuelles sur une année ===
                case "statsParMois":
                    result = adminStatsService.statsParMois((String) args.get("annee"));
                    break;
                default:
                    result = Map.of("erreur", "Outil inconnu : " + name);
            }
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"erreur\": \"" + e.getMessage() + "\"}";
        }
    }

    private List<Map<String, Object>> buildAdminTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(functionTool(
                "countReservations",
                "Compte les réservations GLOBALES (toutes salles confondues) avec des filtres "
                        + "optionnels et combinables : statut (CONFIRMEE/EN_ATTENTE/REFUSEE) et/ou "
                        + "période (dateDebut/dateFin). UTILISE CET OUTIL EN PRIORITÉ pour toute "
                        + "question générale du type 'combien de réservations confirmées le mois dernier', "
                        + "'combien de refus cette semaine', 'combien de réservations en attente'. "
                        + "N'utilise countReservationsBySalle QUE si l'utilisateur mentionne explicitement "
                        + "le nom d'une salle précise.",
                Map.of(
                        "statut", Map.of(
                                "type", "string",
                                "enum", List.of("CONFIRMEE", "EN_ATTENTE", "REFUSEE"),
                                "description", "Optionnel : filtrer par statut"
                        ),
                        "dateDebut", Map.of("type", "string", "description", "Optionnel : date de début au format AAAA-MM-JJ"),
                        "dateFin", Map.of("type", "string", "description", "Optionnel : date de fin au format AAAA-MM-JJ")
                ),
                List.of()
        ));

        tools.add(functionTool(
                "countSallesDisponibles",
                "Retourne le nombre total de salles, le nombre de salles disponibles, et le nombre "
                        + "de salles indisponibles (des CHIFFRES). Utilise cet outil pour 'combien de salles "
                        + "disponibles ?'. Si l'admin demande QUELLES salles sont disponibles (la liste avec "
                        + "noms/capacité/équipements), n'utilise PAS cet outil : réponds directement à partir "
                        + "des DONNÉES SYSTÈME injectées dans le prompt.",
                Map.of(),
                List.of()
        ));

        tools.add(functionTool(
                "countReservationsBySalle",
                "Compte le nombre de réservations pour une salle donnée (recherche par nom). "
                        + "Accepte des filtres optionnels et combinables : statut, et/ou période (dateDebut/dateFin). "
                        + "Utilise cet outil pour TOUTE question portant sur une salle précise, même si elle combine "
                        + "plusieurs critères (ex: 'réservations confirmées de la salle 4 le mois dernier').",
                Map.of(
                        "nomSalle", Map.of("type", "string", "description", "Nom (ou partie du nom) de la salle"),
                        "statut", Map.of(
                                "type", "string",
                                "enum", List.of("CONFIRMEE", "EN_ATTENTE", "REFUSEE"),
                                "description", "Optionnel : filtrer par statut"
                        ),
                        "dateDebut", Map.of("type", "string", "description", "Optionnel : date de début au format AAAA-MM-JJ"),
                        "dateFin", Map.of("type", "string", "description", "Optionnel : date de fin au format AAAA-MM-JJ")
                ),
                List.of("nomSalle")
        ));

        tools.add(functionTool(
                "countTotalReservations",
                "Retourne le nombre total de réservations, avec la répartition par statut (confirmées, en attente, refusées).",
                Map.of(),
                List.of()
        ));

        tools.add(functionTool(
                "countUsers",
                "Retourne le nombre total d'utilisateurs inscrits, avec la répartition par rôle (clients, admins).",
                Map.of(),
                List.of()
        ));

        tools.add(functionTool(
                "countUsersByRole",
                "Compte les utilisateurs ayant un rôle précis.",
                Map.of(
                        "role", Map.of(
                                "type", "string",
                                "enum", List.of("CLIENT", "ADMIN"),
                                "description", "Le rôle des utilisateurs à compter"
                        )
                ),
                List.of("role")
        ));

        tools.add(functionTool(
                "countReservationsByPeriode",
                "Compte le nombre de réservations effectuées entre deux dates.",
                Map.of(
                        "dateDebut", Map.of("type", "string", "description", "Date de début au format AAAA-MM-JJ"),
                        "dateFin", Map.of("type", "string", "description", "Date de fin au format AAAA-MM-JJ")
                ),
                List.of("dateDebut", "dateFin")
        ));

        tools.add(functionTool(
                "repartitionReservationsParSalle",
                "Retourne le nombre total de réservations pour chaque salle du système.",
                Map.of(),
                List.of()
        ));

        // === NOUVEAU ===
        tools.add(functionTool(
                "tauxReservations",
                "Calcule les TAUX en pourcentage (% confirmation, % refus, % en attente) sur toutes "
                        + "les réservations, globalement ou sur une période donnée. Utilise cet outil pour "
                        + "TOUTE question contenant le mot 'taux' ou '%' (ex: 'quel est le taux de confirmation', "
                        + "'taux de refus ce mois-ci', 'pourcentage de réservations en attente').",
                Map.of(
                        "dateDebut", Map.of("type", "string", "description", "Optionnel : date de début au format AAAA-MM-JJ"),
                        "dateFin", Map.of("type", "string", "description", "Optionnel : date de fin au format AAAA-MM-JJ")
                ),
                List.of()
        ));

        tools.add(functionTool(
                "statsParNatureManifestation",
                "Retourne la répartition du nombre de réservations par nature/type de manifestation "
                        + "(réunion, conférence, formation, coworking, etc.), avec filtres optionnels de "
                        + "période et de statut. Utilise cet outil pour 'quelle est la répartition par type "
                        + "d'événement', 'quelles sont les manifestations les plus fréquentes'.",
                Map.of(
                        "dateDebut", Map.of("type", "string", "description", "Optionnel : date de début au format AAAA-MM-JJ"),
                        "dateFin", Map.of("type", "string", "description", "Optionnel : date de fin au format AAAA-MM-JJ"),
                        "statut", Map.of(
                                "type", "string",
                                "enum", List.of("CONFIRMEE", "EN_ATTENTE", "REFUSEE"),
                                "description", "Optionnel : filtrer par statut"
                        )
                ),
                List.of()
        ));

        tools.add(functionTool(
                "statsParMois",
                "Retourne le nombre de réservations pour chaque mois d'une année donnée (évolution "
                        + "mensuelle). Utilise cet outil pour 'évolution des réservations cette année', "
                        + "'combien de réservations par mois'.",
                Map.of(
                        "annee", Map.of("type", "string", "description", "Optionnel : année au format AAAA (ex: 2026). Par défaut : année en cours.")
                ),
                List.of()
        ));

        return tools;
    }

    private Map<String, Object> functionTool(String name, String description,
                                             Map<String, Object> properties, List<String> required) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);

        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);

        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        function.put("parameters", parameters);
        return tool;
    }

    private String buildAdminSystemPrompt(String ragContext) {
        return "Tu es l'assistant IA d'administration de PfeSpace "
                + "(plateforme de réservation de salles de Startup Village, Medianet, Tunis).\n\n"

                + "## TON DOUBLE RÔLE\n"
                + "1. STATISTIQUES CHIFFRÉES : nombre de réservations (confirmées, en attente, refusées), "
                + "nombre d'utilisateurs, taux de confirmation/refus, répartition par salle ou par type "
                + "d'événement, évolution mensuelle, etc. → utilise TOUJOURS les outils disponibles pour ça.\n"
                + "2. INFORMATIONS SUR LES SALLES : comme pour un client, tu peux répondre directement aux "
                + "questions sur les salles elles-mêmes (liste, capacité, équipements, prix, disponibilité "
                + "d'un créneau précis) en te basant UNIQUEMENT sur les DONNÉES SYSTÈME injectées ci-dessous "
                + "— PAS besoin d'appeler un outil pour ça, l'information est déjà là.\n\n"

                + "## CE QUE TU N'ES PAS\n"
                + "- Tu ne peux PAS enregistrer, confirmer ou annuler une réservation toi-même. Si l'admin "
                + "veut effectuer une action de ce type, indique-lui d'utiliser le module Réservations / "
                + "Calendrier / Pré-réservations.\n\n"

                + "## PRIORITÉ DES OUTILS — RÈGLE ABSOLUE (questions chiffrées uniquement)\n"
                + "- Pour toute question GLOBALE sur un nombre de réservations, utilise TOUJOURS countReservations en premier.\n"
                + "- N'utilise JAMAIS countReservationsBySalle sans un nom de salle explicitement donné.\n"
                + "- Pour COMBIEN de salles disponibles/indisponibles (un chiffre), utilise countSallesDisponibles.\n"
                + "- Pour un TAUX ou un POURCENTAGE (confirmation, refus, attente), utilise tauxReservations.\n"
                + "- Pour une répartition par TYPE/NATURE d'événement, utilise statsParNatureManifestation.\n"
                + "- Pour une évolution MENSUELLE sur une année, utilise statsParMois.\n"
                + "- Pour QUELLES salles sont disponibles (liste avec noms/détails), N'utilise AUCUN outil : "
                + "réponds directement à partir des DONNÉES SYSTÈME ci-dessous.\n\n"

                + "## RÈGLE ABSOLUE SUR LES OUTILS DE STATISTIQUES\n"
                + "- Tu n'as PAS accès direct à la base de données pour les chiffres. Pour CHAQUE question "
                + "chiffrée, tu DOIS appeler l'outil correspondant, MÊME si des chiffres similaires ont déjà "
                + "été donnés plus tôt dans la conversation.\n"
                + "- INTERDICTION ABSOLUE de réutiliser un résultat d'un outil pour répondre à une question "
                + "qui porte sur une donnée différente.\n"
                + "- Ne dis JAMAIS \"je ne peux pas te donner ce chiffre\" sans avoir essayé l'outil approprié.\n\n"

                + "## COMBINER LES CRITÈRES ENTRE PLUSIEURS MESSAGES\n"
                + "- Utilise TOUJOURS le contexte des messages précédents pour reconstituer la question "
                + "complète et appeler l'outil approprié avec TOUS les filtres pertinents.\n"
                + "- Calcule toi-même les périodes relatives (\"cette semaine\", \"le mois dernier\") en "
                + "dates exactes AAAA-MM-JJ à partir de la date du jour ci-dessous.\n\n"

                + "## RÈGLE ABSOLUE ANTI-INVENTION\n"
                + "Tu ne dois JAMAIS inventer un nom de salle. Utilise EXCLUSIVEMENT les noms de salles qui "
                + "apparaissent explicitement dans les DONNÉES SYSTÈME ci-dessous ou dans les résultats "
                + "d'outils.\n\n"

                + "## STYLE DE RÉPONSE\n"
                + "- Réponds de façon claire, concise et professionnelle, avec les chiffres exacts.\n"
                + "- Réponds dans la langue de l'administrateur (français ou arabe).\n"
                + "- Date d'aujourd'hui : "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n\n"

                + "## DONNÉES SYSTÈME EN TEMPS RÉEL (salles, équipements, réservations existantes)\n"
                + ragContext;
    }

    // ==========================================
    //  UTILITAIRES COMMUNS — LLM CALL + FALLBACK
    // ==========================================

    /**
     * Appelle Groq avec retries sur 429. Si Groq est toujours limité après
     * MAX_RETRIES_429 tentatives, bascule automatiquement sur Mistral AI
     * en réutilisant le même payload (messages, tools, temperature...).
     */
    private Map<?, ?> callLLMWithFallback(Map<String, Object> body) {
        int attempt = 0;
        while (true) {
            try {
                return callGroq(body);
            } catch (HttpClientErrorException.TooManyRequests e) {
                long waitMillis = resolveRetryAfterMillis(e, attempt + 1);

                if (waitMillis >= MAX_BACKOFF_MILLIS) {
                    // Retry-After trop long = quota epuise, pas une limite transitoire.
                    // Inutile d'attendre : on bascule tout de suite sur Mistral.
                    System.out.println("[CHATBOT-DEBUG] Groq quota probablement epuise (retry-after="
                            + waitMillis + "ms) -> bascule immediate sur Mistral AI");
                    return callMistralFallback(body, e);
                }

                attempt++;
                if (attempt > MAX_RETRIES_429) {
                    System.out.println("[CHATBOT-DEBUG] Groq toujours limité après "
                            + MAX_RETRIES_429 + " tentatives -> bascule sur Mistral AI");
                    return callMistralFallback(body, e);
                }

                System.out.println("[CHATBOT-DEBUG] 429 Groq, tentative " + attempt
                        + "/" + MAX_RETRIES_429 + " — attente " + waitMillis + "ms");
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    /**
     * Rebâtit le body avec le modèle Mistral et appelle son API.
     * Si Mistral échoue aussi, on relance l'exception Groq d'origine
     * pour que handleLLMError renvoie un message cohérent à l'utilisateur.
     */
    private Map<?, ?> callMistralFallback(Map<String, Object> body, HttpClientErrorException groqError) {
        try {
            Map<String, Object> mistralBody = new HashMap<>(body);
            mistralBody.put("model", mistralModel);
            return callMistral(mistralBody);
        } catch (HttpClientErrorException mistralError) {
            System.out.println("[CHATBOT-DEBUG] Mistral a aussi échoué : status="
                    + mistralError.getStatusCode() + " body=" + mistralError.getResponseBodyAsString());
            throw groqError;
        } catch (Exception mistralError) {
            System.out.println("[CHATBOT-DEBUG] Mistral a aussi échoué (non-HTTP) : " + mistralError.getMessage());
            throw groqError;
        }
    }

    private long resolveRetryAfterMillis(HttpClientErrorException.TooManyRequests e, int attempt) {
        try {
            String retryAfter = e.getResponseHeaders() != null
                    ? e.getResponseHeaders().getFirst("Retry-After")
                    : null;
            if (retryAfter != null) {
                return (long) (Double.parseDouble(retryAfter) * 1000);
            }
        } catch (Exception ignored) {
        }
        return (long) Math.pow(2, attempt) * 1000L;
    }

    private String handleLLMError(HttpClientErrorException e) {
        if (e instanceof HttpClientErrorException.TooManyRequests) {
            return "Le service IA est momentanément surchargé (limite de requêtes atteinte, "
                    + "y compris sur le service de secours). Merci de patienter quelques secondes "
                    + "avant de reposer ta question.";
        }
        if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
            return "Erreur d'authentification auprès du service IA : vérifiez les clés API (GROQ_API_KEY / MISTRAL_API_KEY).";
        }
        return "Erreur API (" + e.getStatusCode() + ") : veuillez réessayer dans un instant.";
    }

    private Map<?, ?> callGroq(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(groqApiUrl, entity, Map.class);

        if (response.getBody() == null) {
            throw new RuntimeException("Réponse vide de l'API Groq.");
        }
        return response.getBody();
    }

    private Map<?, ?> callMistral(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mistralApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(mistralApiUrl, entity, Map.class);

        if (response.getBody() == null) {
            throw new RuntimeException("Réponse vide de l'API Mistral.");
        }
        return response.getBody();
    }

    private String extractMessageContent(Map<?, ?> responseBody) {
        List<?> choices = (List<?>) responseBody.get("choices");
        if (choices == null || choices.isEmpty()) return null;
        Map<?, ?> messageMap = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
        Object content = messageMap.get("content");
        return content != null ? content.toString() : null;
    }

    private boolean seemsLikeStatQuestion(String message) {
        if (message == null) return false;
        String m = message.toLowerCase();
        String[] motsCles = {
                "combien", "nombre", "total", "statistique", "utilisateur", "user",
                "client", "admin", "reservation", "réservation", "confirme", "confirmé",
                "refuse", "refusé", "attente", "taux", "pourcentage", "%", "repartition",
                "répartition", "evolution", "évolution", "mois", "nature", "manifestation"
        };
        for (String mot : motsCles) {
            if (m.contains(mot)) return true;
        }
        return false;
    }

    private Map<String, Object> textMessage(String role, String content) {
        Map<String, Object> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    public void clearHistory(String identifiant) {
        String base = (identifiant != null && !identifiant.isEmpty()) ? identifiant : "anonymous";
        conversationHistory.remove("client:" + base);
        conversationHistory.remove("visiteur:" + base);
        conversationHistory.remove("admin:" + base);
    }
}