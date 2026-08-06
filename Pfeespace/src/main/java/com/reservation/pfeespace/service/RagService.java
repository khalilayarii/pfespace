package com.reservation.pfeespace.service;

import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.entity.Salle;
import com.reservation.pfeespace.repository.ReservationRepository;
import com.reservation.pfeespace.repository.SalleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Autowired
    private SalleRepository salleRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AdminStatsService adminStatsService;

    // ==========================================
    //  POINT D'ENTRÉE PRINCIPAL
    // ==========================================
    // Nouvelle signature — ajoute isLoggedIn
    public String retrieveRelevantContext(String question, String userEmail, boolean isLoggedIn) {
        if (question == null || question.isBlank()) return "";

        String q = normaliser(question);
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmtToday = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        LocalDate dateDetectee = extractDate(q);
        if (dateDetectee != null && dateDetectee.isBefore(today)) {
            return "\n## Contexte PfeSpace (date du jour : " + today.format(fmtToday) + ") :\n"
                    + "[ALERTE CRITIQUE - PRIORITE ABSOLUE SUR TOUT LE RESTE]\n"
                    + "L'utilisateur mentionne la date " + dateDetectee.format(fmtToday)
                    + ", qui est ANTERIEURE a la date d'aujourd'hui (" + today.format(fmtToday) + ").\n"
                    + "Cette date est DANS LE PASSE. Une reservation a cette date est IMPOSSIBLE.\n"
                    + "[FORMAT DE REPONSE OBLIGATOIRE]\n"
                    + "Reponds en UNE SEULE phrase courte, sans paragraphe, sans liste, sans explication.\n"
                    + "Utilise exactement ce modele :\n"
                    + "'La date du " + dateDetectee.format(fmtToday) + " est deja passee (nous sommes le "
                    + today.format(fmtToday) + "). Merci de choisir une date future.'\n";
        }

        StringBuilder ctx = new StringBuilder();

        // Priorité 1 : recommandation explicite — RÉSERVÉ AU CLIENT CONNECTÉ
        if (isRecommandationQuestion(q)) {
            if (isLoggedIn) {
                ctx.append(buildRecommandationContext(q));
            } else {
                ctx.append("=== RECOMMANDATION DEMANDÉE PAR UN VISITEUR NON CONNECTÉ ===\n");
                ctx.append("[INSTRUCTION AU LLM] Ne fais PAS de recommandation personnalisée. ");
                ctx.append("Explique brièvement que la recommandation IA sur mesure nécessite un compte ");
                ctx.append("gratuit (bouton 'Se connecter / S'inscrire'), et propose de lui montrer la liste ");
                ctx.append("générale des salles disponibles à la place.\n");
                ctx.append(buildSalleContext("disponible"));
            }
        }
        // Priorité 2 : question sur les salles / disponibilité (accessible à tous)
        else if (isSalleQuestion(q)) {
            ctx.append(buildSalleContext(q));
        }

        // Questions sur les réservations personnelles — seulement si un email existe (donc connecté)
        if (isReservationQuestion(q) && userEmail != null && !userEmail.isBlank()) {
            ctx.append(buildReservationContext(userEmail, q));
        }

        // Statistiques globales — on les laisse accessibles ici (chiffres généraux, pas sensibles),
        // mais si tu veux les réserver aussi au client connecté, ajoute `&& isLoggedIn` ci-dessous.
        if (isStatQuestion(q)) {
            ctx.append(buildStatsContext(q));
        }

        if (ctx.isEmpty()) return "";
        return "\n## Contexte PfeSpace (date du jour : " + today.format(fmtToday) + ") :\n"
                + "[IMPORTANT] La date d'aujourd'hui est le " + today.format(fmtToday)
                + ". Ne jamais dire qu'une date est 'trop lointaine' ou inventer des regles sur les delais de reservation. "
                + "Le systeme accepte les reservations a n'importe quelle date future. "
                + "Si la date demandee est dans le passe, signaler simplement que la date est passee.\n"
                + ctx;
    }

    // ==========================================
    //  NORMALISATION
    // ==========================================

    private String normaliser(String s) {
        return s.toLowerCase()
                .replace("\u2019", "'").replace("\u2018", "'").replace("'", "'")
                .replace("é", "e").replace("è", "e").replace("ê", "e").replace("ë", "e")
                .replace("à", "a").replace("â", "a")
                .replace("ù", "u").replace("û", "u")
                .replace("î", "i").replace("ï", "i")
                .replace("ô", "o").replace("ö", "o")
                .replace("ç", "c");
    }

    // ==========================================
    //  DÉTECTION D'INTENTION
    // ==========================================

    private boolean isRecommandationQuestion(String q) {
        return q.contains("recommande") || q.contains("conseil") || q.contains("suggere")
                || q.contains("sugger") || q.contains("quelle salle") || q.contains("aide moi")
                || q.contains("meilleure salle") || q.contains("salle parfaite")
                || q.contains("salle adaptee") || q.contains("salle ideale")
                || q.contains("quelle est la") || q.contains("tu peux me trouver")
                || q.contains("trouve moi") || q.contains("choisir une salle")
                || q.contains("salle pour") || q.contains("besoin d'une salle")
                || q.contains("besoin d une salle") || q.contains("salle convenable")
                || q.contains("salle compatible");
    }

    private boolean isSalleQuestion(String q) {
        return q.contains("salle") || q.contains("bureau") || q.contains("espace")
                || q.contains("capacite") || q.contains("personne") || q.contains("equipement")
                || q.contains("disponible") || q.contains("prix") || q.contains("tarif")
                || q.contains("reserver") || q.contains("louer") || q.contains("room")
                || q.contains("wifi") || q.contains("projecteur") || q.contains("climatisation")
                || q.contains("libre");
    }

    private boolean isReservationQuestion(String q) {
        return q.contains("reservation") || q.contains("reserv")
                || q.contains("statut") || q.contains("status")
                || q.contains("attente") || q.contains("confirme") || q.contains("refuse")
                || q.contains("annuler") || q.contains("historique")
                || q.contains("prochaine") || q.contains("booking");
    }

    // Élargi : couvre maintenant les taux (%), les refus courts ("et de refus"),
    // la nature de manifestation et les stats mensuelles, pour éviter les questions
    // de suivi qui ne matchaient rien et provoquaient un contexte vide (=> erreur 400).
    private boolean isStatQuestion(String q) {
        return q.contains("statistique") || q.contains("total") || q.contains("combien")
                || q.contains("nombre") || q.contains("aujourd") || q.contains("occupation")
                || q.contains("taux") || q.contains("pourcentage") || q.contains("%")
                || q.contains("refus") || q.contains("manifestation") || q.contains("nature")
                || q.contains("type") || q.contains("mois") || q.contains("evolution")
                || q.contains("repartition");
    }

    // ==========================================
    //  RECOMMANDATION — CŒUR DU SYSTÈME
    // ==========================================

    private String buildRecommandationContext(String question) {
        StringBuilder ctx = new StringBuilder();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        int capaciteMin   = extractCapacite(question);
        LocalDate date    = extractDate(question);
        LocalTime hDebut  = extractHeureDebut(question);
        LocalTime hFin    = extractHeureFin(question);
        String typeEvent  = detecterTypeEvent(question);

        List<String> manquants = new ArrayList<>();
        if (capaciteMin <= 0)    manquants.add("nombre de personnes");
        if (date == null)        manquants.add("date souhaitee");
        if (hDebut == null)      manquants.add("heure de debut");
        if (hFin == null)        manquants.add("heure de fin");
        if (typeEvent.isEmpty()) manquants.add("type d'evenement (reunion, conference, coworking, formation...)");

        boolean capaciteManque = capaciteMin <= 0;
        boolean dateManque     = date == null;
        boolean heureManque    = hDebut == null || hFin == null;

        if (capaciteManque || (dateManque && heureManque)) {
            ctx.append("=== INFORMATIONS INSUFFISANTES POUR RECOMMANDER ===\n");
            ctx.append("Parametres detectes dans la question :\n");
            if (capaciteMin > 0)      ctx.append("  ✓ Capacite : ").append(capaciteMin).append(" personnes\n");
            else                       ctx.append("  ✗ Capacite : non precisee\n");
            if (date != null)         ctx.append("  ✓ Date : ").append(date.format(fmt)).append("\n");
            else                       ctx.append("  ✗ Date : non precisee\n");
            if (hDebut != null)       ctx.append("  ✓ Heure debut : ").append(hDebut).append("\n");
            else                       ctx.append("  ✗ Heure debut : non precisee\n");
            if (hFin != null)         ctx.append("  ✓ Heure fin : ").append(hFin).append("\n");
            else                       ctx.append("  ✗ Heure fin : non precisee\n");
            if (!typeEvent.isEmpty()) ctx.append("  ✓ Type : ").append(typeEvent).append("\n");
            else                       ctx.append("  ✗ Type d'evenement : non precise\n");
            ctx.append("\n");
            ctx.append("[INSTRUCTION AU LLM] Ne fais PAS encore de recommandation.\n");
            ctx.append("Pose des questions courtes et precises a l'utilisateur pour obtenir les informations manquantes.\n");
            ctx.append("Pose UNE seule question a la fois, en commencant par la plus importante.\n");
            ctx.append("Ordre de priorite des questions :\n");
            if (capaciteManque)  ctx.append("  1. Combien de personnes seront presentes ?\n");
            if (typeEvent.isEmpty()) ctx.append("  2. Quel est le type d'evenement (reunion, conference, formation, coworking) ?\n");
            if (dateManque)      ctx.append("  3. Pour quelle date souhaitez-vous reserver ?\n");
            if (heureManque)     ctx.append("  4. Sur quel creneau horaire (heure de debut et fin) ?\n");
            ctx.append("Sois chaleureux et bref. Exemple : 'Pour vous recommander la salle ideale, j'ai besoin de quelques informations. Combien de personnes participent ?'\n");
            return ctx.toString();
        }

        ctx.append("=== ANALYSE DE VOTRE BESOIN ===\n");
        if (capaciteMin > 0)      ctx.append("Capacite demandee : ").append(capaciteMin).append(" personnes\n");
        if (date != null)         ctx.append("Date souhaitee : ").append(date.format(fmt)).append("\n");
        if (hDebut != null)       ctx.append("Heure debut : ").append(hDebut).append("\n");
        if (hFin != null)         ctx.append("Heure fin : ").append(hFin).append("\n");
        if (!typeEvent.isEmpty()) ctx.append("Type d'evenement detecte : ").append(typeEvent).append("\n");
        ctx.append("\n");

        List<Salle> salles = salleRepository.findAll()
                .stream()
                .filter(Salle::isDisponible)
                .collect(Collectors.toList());

        if (salles.isEmpty()) {
            ctx.append("=== RECOMMANDATION ===\nAucune salle disponible actuellement.\n");
            return ctx.toString();
        }

        // ⚡ PERF : une seule requête pour toutes les réservations "futures confirmées"
        // des salles concernées, au lieu d'un findBySalleId() par salle.
        List<Long> salleIds = salles.stream().map(Salle::getId).collect(Collectors.toList());
        Map<Long, List<Reservation>> resasParSalle = reservationRepository
                .findBySalleIdIn(salleIds)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getSalle().getId()));

        // ⚡ PERF : une seule requête pour le conflit de créneau (date+statut != REFUSEE)
        // au lieu d'un findBySalleIdAndDateAndStatutNot() par salle. Seulement si un
        // créneau précis est demandé.
        Map<Long, List<Reservation>> resasCreneauParSalle = new HashMap<>();
        if (date != null) {
            resasCreneauParSalle = reservationRepository
                    .findBySalleIdInAndDateAndStatutNot(salleIds, date, "REFUSEE")
                    .stream()
                    .collect(Collectors.groupingBy(r -> r.getSalle().getId()));
        }

        List<SalleScore> scores = new ArrayList<>();
        for (Salle s : salles) {
            List<Reservation> resasSalle = resasParSalle.getOrDefault(s.getId(), List.of());
            List<Reservation> resasCreneauSalle = resasCreneauParSalle.getOrDefault(s.getId(), List.of());
            ResultatScore r = scorerSalle(s, capaciteMin, date, hDebut, hFin, typeEvent, question,
                    resasSalle, resasCreneauSalle);
            if (r.score >= 0) {
                scores.add(new SalleScore(s, r.score, r.disponibleCreneauDemande, r.raisons));
            }
        }

        if (scores.isEmpty()) {
            ctx.append("=== RECOMMANDATION ===\n");
            ctx.append("Aucune salle ne correspond exactement a vos criteres.\n");
            ctx.append("Voici toutes les salles disponibles sans contrainte de creneau :\n");
            for (Salle s : salles) {
                ctx.append(String.format("• %s | %d pers. | %s | %.0f DT/h\n",
                        s.getNom(), s.getCapacite(),
                        s.getEquipement() != null ? s.getEquipement() : "standard",
                        s.getPrix()));
            }
            return ctx.toString();
        }

        scores.sort(Comparator.comparingInt(SalleScore::getScore).reversed());

        ctx.append("=== CLASSEMENT DES SALLES RECOMMANDEES ===\n");
        ctx.append("(Score sur 100 — plus le score est élevé, plus la salle est adaptée)\n\n");

        int rang = 1;
        for (SalleScore ss : scores) {
            Salle s = ss.salle;
            String medal = rang == 1 ? "★ MEILLEUR CHOIX" : rang == 2 ? "✓ BON CHOIX" : "○ Acceptable";

            ctx.append(String.format("[%d] %s — %s\n", rang, medal, s.getNom()));
            ctx.append(String.format(
                    "   Capacite: %d pers. | Equipements: %s | Prix: %.0f DT/h | Score: %d/100\n",
                    s.getCapacite(),
                    s.getEquipement() != null ? s.getEquipement() : "standard",
                    s.getPrix(),
                    ss.score));

            if (s.getDescription() != null && !s.getDescription().isBlank()) {
                ctx.append("   Description: ").append(s.getDescription()).append("\n");
            }

            if (date != null && hDebut != null && hFin != null) {
                if (ss.disponibleCreneau) {
                    ctx.append(String.format("   DISPONIBLE le %s de %s a %s ✓\n",
                            date.format(fmt), hDebut, hFin));
                } else {
                    ctx.append(String.format("   OCCUPEE le %s sur ce creneau ✗\n", date.format(fmt)));
                }
            }

            if (!ss.raisons.isEmpty()) {
                ctx.append("   Points forts : ").append(String.join(", ", ss.raisons)).append("\n");
            }

            // ⚡ PERF : réutilise la map déjà chargée au lieu de refaire une requête par salle
            List<Reservation> prochains = resasParSalle.getOrDefault(s.getId(), List.of())
                    .stream()
                    .filter(r -> "CONFIRMEE".equals(r.getStatut()))
                    .filter(r -> r.getDate() != null && !r.getDate().isBefore(today))
                    .sorted(Comparator.comparing(Reservation::getDate))
                    .limit(3)
                    .collect(Collectors.toList());

            if (!prochains.isEmpty()) {
                ctx.append("   Creneaux deja pris (a eviter) :\n");
                for (Reservation r : prochains) {
                    ctx.append(String.format("     → %s %s-%s\n",
                            r.getDate().format(fmt), r.getHeureDebut(), r.getHeureFin()));
                }
            } else {
                ctx.append("   → Aucun creneau confirme a venir, totalement libre.\n");
            }
            ctx.append("\n");
            rang++;
        }

        ctx.append("[INSTRUCTION] Presente la salle classee [1] comme recommandation principale.\n");
        ctx.append("Explique pourquoi elle est ideale pour le besoin de l'utilisateur.\n");
        ctx.append("Si tu proposes une alternative, elle doit avoir une capacite raisonnablement proche ");
        ctx.append("du nombre de personnes demande (jamais une salle 2x ou plus grande que necessaire) : ");
        ctx.append("mentionne UNIQUEMENT une 2eme salle si son score est proche ET sa capacite adaptee.\n");
        ctx.append("Sois precis sur la disponibilite au creneau demande.\n");
        ctx.append("Si aucun creneau n'est precise, invite l'utilisateur a preciser la date et l'heure.\n");
        return ctx.toString();
    }

    // ==========================================
    //  ALGORITHME DE SCORING
    // ==========================================

    // ⚡ PERF : reçoit maintenant les réservations déjà chargées en amont (batch),
    // au lieu d'interroger la DB elle-même à chaque appel.
    private ResultatScore scorerSalle(Salle salle, int capaciteMin,
                                      LocalDate date, LocalTime hDebut, LocalTime hFin,
                                      String typeEvent, String question,
                                      List<Reservation> resasSalle,
                                      List<Reservation> resasCreneauSalle) {
        int score = 0;
        boolean disponibleCreneau = true;
        List<String> raisons = new ArrayList<>();

        if (capaciteMin > 0) {
            if (salle.getCapacite() < capaciteMin) {
                return new ResultatScore(-1, false, List.of());
            }
            double ratio = (double) salle.getCapacite() / capaciteMin;
            if (ratio >= 1.0 && ratio <= 1.4) {
                score += 30;
                raisons.add("capacite parfaite (" + salle.getCapacite() + " pers.)");
            } else if (ratio > 1.4 && ratio <= 2.0) {
                score += 18;
                raisons.add("capacite suffisante");
            } else if (ratio > 2.0 && ratio <= 3.0) {
                score += 5;
            } else {
                // Salle largement surdimensionnée (ex: 40 places pour 5 pers) : on l'exclut
                // du classement des recommandations pertinentes.
                return new ResultatScore(-1, false, List.of());
            }
        } else {
            score += 15;
        }

        if (date != null && hDebut != null && hFin != null) {
            boolean conflit = resasCreneauSalle.stream()
                    .anyMatch(r -> !r.getHeureFin().isBefore(hDebut) && !r.getHeureDebut().isAfter(hFin));

            if (conflit) {
                disponibleCreneau = false;
                return new ResultatScore(-1, false, List.of());
            }
            score += 25;
            raisons.add("disponible sur le creneau");
        } else {
            long resasFutures = resasSalle.stream()
                    .filter(r -> "CONFIRMEE".equals(r.getStatut()))
                    .filter(r -> r.getDate() != null && !r.getDate().isBefore(LocalDate.now()))
                    .count();
            if (resasFutures == 0) { score += 20; raisons.add("aucune reservation future"); }
            else if (resasFutures <= 2) score += 12;
            else score += 5;
        }

        String eq = salle.getEquipement() != null ? normaliser(salle.getEquipement()) : "";
        int eqScore = 0;

        switch (typeEvent) {
            case "REUNION":
                if (eq.contains("projecteur") || eq.contains("ecran")) { eqScore += 10; }
                if (eq.contains("wifi"))                                 { eqScore += 8; }
                if (eq.contains("tableau") || eq.contains("whiteboard")) { eqScore += 7; }
                break;
            case "CONFERENCE":
            case "EVENEMENT":
                if (eq.contains("projecteur") || eq.contains("ecran"))  { eqScore += 12; }
                if (eq.contains("micro") || eq.contains("sono"))         { eqScore += 8; }
                if (eq.contains("wifi"))                                  { eqScore += 5; }
                break;
            case "COWORKING":
                if (eq.contains("wifi"))                                  { eqScore += 15; }
                if (eq.contains("clim") || eq.contains("climatisation")) { eqScore += 7; }
                if (eq.contains("prise") || eq.contains("bureau"))       { eqScore += 3; }
                break;
            case "FORMATION":
                if (eq.contains("projecteur"))                            { eqScore += 12; }
                if (eq.contains("tableau"))                               { eqScore += 10; }
                if (eq.contains("wifi"))                                  { eqScore += 3; }
                break;
            default:
                if (question.contains("wifi") && eq.contains("wifi"))           { eqScore += 10; }
                if (question.contains("projecteur") && eq.contains("projecteur")){ eqScore += 10; }
                if (question.contains("clim") && (eq.contains("clim")))         { eqScore += 8; }
                if (question.contains("micro") && eq.contains("micro"))         { eqScore += 8; }
        }

        eqScore = Math.min(eqScore, 25);
        if (eqScore >= 15) raisons.add("equipements adaptes");
        score += eqScore;

        double prix = salle.getPrix();
        if      (prix <= 30)  { score += 10; raisons.add("tarif economique"); }
        else if (prix <= 60)  { score += 6; }
        else if (prix <= 100) { score += 3; }

        if (salle.getDescription() != null) {
            String desc = normaliser(salle.getDescription());
            if (!typeEvent.isEmpty() && desc.contains(normaliser(typeEvent))) score += 10;
            else if (desc.contains("confort") || desc.contains("moderne"))    score += 5;
        }

        return new ResultatScore(score, disponibleCreneau, raisons);
    }

    // ==========================================
    //  CONTEXTE SALLES (questions générales)
    // ==========================================

    private String buildSalleContext(String question) {
        StringBuilder ctx = new StringBuilder();
        List<Salle> salles = salleRepository.findAll();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        int capaciteMin = extractCapacite(question);
        if (capaciteMin > 0) {
            salles = salles.stream()
                    .filter(s -> s.getCapacite() >= capaciteMin)
                    .collect(Collectors.toList());
            ctx.append("=== SALLES CAPACITE >= ").append(capaciteMin).append(" PERSONNES ===\n");
        } else if (question.contains("disponible") || question.contains("libre")) {
            salles = salles.stream().filter(Salle::isDisponible).collect(Collectors.toList());
            ctx.append("=== SALLES DISPONIBLES ===\n");
        } else {
            ctx.append("=== TOUTES LES SALLES ===\n");
        }

        if (salles.isEmpty()) {
            ctx.append("Aucune salle ne correspond.\n");
            return ctx.toString();
        }

        // ⚡ PERF : une seule requête pour toutes les salles filtrées, au lieu d'un
        // findBySalleId() par salle dans la boucle (avant : N requêtes pour N salles).
        List<Long> salleIds = salles.stream().map(Salle::getId).collect(Collectors.toList());
        Map<Long, List<Reservation>> resasParSalle = reservationRepository
                .findBySalleIdIn(salleIds)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getSalle().getId()));

        for (Salle s : salles) {
            ctx.append(String.format(
                    "• %s | Capacite: %d pers. | Equipements: %s | Prix: %.0f DT/h | %s\n",
                    s.getNom(), s.getCapacite(),
                    s.getEquipement() != null ? s.getEquipement() : "standard",
                    s.getPrix(),
                    s.isDisponible() ? "Disponible" : "Indisponible"));

            List<Reservation> occupes = resasParSalle.getOrDefault(s.getId(), List.of())
                    .stream()
                    .filter(r -> "CONFIRMEE".equals(r.getStatut()))
                    .filter(r -> r.getDate() != null && !r.getDate().isBefore(today))
                    .sorted(Comparator.comparing(Reservation::getDate))
                    .collect(Collectors.toList());

            if (!occupes.isEmpty()) {
                ctx.append("  [!] CRENEAUX CONFIRMES :\n");
                for (Reservation r : occupes) {
                    ctx.append(String.format("  -> %s de %s a %s : OCCUPE\n",
                            r.getDate().format(fmt), r.getHeureDebut(), r.getHeureFin()));
                }
            } else {
                ctx.append("  -> Aucun creneau confirme : salle libre.\n");
            }
        }

        ctx.append("\n[REGLE] Si le creneau est OCCUPE, reponds que c'est impossible et suggere une alternative.\n");
        return ctx.toString();
    }

    // ==========================================
    //  CONTEXTE RÉSERVATIONS UTILISATEUR
    // ==========================================

    private String buildReservationContext(String userEmail, String question) {
        StringBuilder ctx = new StringBuilder();
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<Reservation> all = reservationRepository.findByUserEmail(userEmail);

        if (all == null || all.isEmpty()) {
            ctx.append("=== VOS RESERVATIONS ===\nAucune reservation trouvee.\n");
            return ctx.toString();
        }

        List<Reservation> filtered;
        String titre;

        if (question.contains("attente")) {
            filtered = all.stream().filter(r -> "EN_ATTENTE".equals(r.getStatut())).collect(Collectors.toList());
            titre = "=== RESERVATIONS EN ATTENTE ===\n";
        } else if (question.contains("confirme")) {
            filtered = all.stream().filter(r -> "CONFIRMEE".equals(r.getStatut())).collect(Collectors.toList());
            titre = "=== RESERVATIONS CONFIRMEES ===\n";
        } else if (question.contains("refuse")) {
            filtered = all.stream().filter(r -> "REFUSEE".equals(r.getStatut())).collect(Collectors.toList());
            titre = "=== RESERVATIONS REFUSEES ===\n";
        } else if (question.contains("prochaine") || question.contains("venir")) {
            filtered = all.stream()
                    .filter(r -> r.getDate() != null && !r.getDate().isBefore(today))
                    .sorted(Comparator.comparing(Reservation::getDate))
                    .collect(Collectors.toList());
            titre = "=== RESERVATIONS A VENIR ===\n";
        } else {
            filtered = all.stream()
                    .filter(r -> r.getDate() != null)
                    .sorted(Comparator.comparing(Reservation::getDate).reversed())
                    .limit(5).collect(Collectors.toList());
            titre = "=== VOS 5 DERNIERES RESERVATIONS ===\n";
        }

        ctx.append(titre);
        appendReservations(ctx, filtered, fmt);
        return ctx.toString();
    }

    private void appendReservations(StringBuilder ctx, List<Reservation> list, DateTimeFormatter fmt) {
        if (list.isEmpty()) {
            ctx.append("Aucune reservation trouvee.\n");
        } else {
            for (Reservation r : list) {
                ctx.append(String.format("• %s | %s | %s -> %s | Statut: %s\n",
                        r.getSalle() != null ? r.getSalle().getNom() : "?",
                        r.getDate() != null ? r.getDate().format(fmt) : "?",
                        r.getHeureDebut(), r.getHeureFin(), r.getStatut()));
            }
        }
    }

    // ==========================================
    //  CONTEXTE STATS (relié à AdminStatsService)
    // ==========================================

    private String buildStatsContext(String question) {
        StringBuilder ctx = new StringBuilder();
        LocalDate today = LocalDate.now();

        long total      = reservationRepository.count();
        // ⚡ PERF : countByDate() fait le comptage en DB au lieu de charger
        // TOUTE la table réservations en mémoire (findAll()) pour filtrer ensuite en Java.
        long aujourdhui = reservationRepository.countByDate(today);
        long enAttente  = reservationRepository.countByStatut("EN_ATTENTE");
        long confirmees = reservationRepository.countByStatut("CONFIRMEE");
        long refusees   = reservationRepository.countByStatut("REFUSEE");

        ctx.append("=== STATISTIQUES GENERALES ===\n");
        ctx.append("Total reservations : ").append(total).append("\n");
        ctx.append("Aujourd'hui : ").append(aujourdhui).append("\n");
        ctx.append("En attente : ").append(enAttente).append(" (").append(pourcentage(enAttente, total)).append("%)\n");
        ctx.append("Confirmees : ").append(confirmees).append(" (").append(pourcentage(confirmees, total)).append("%)\n");
        ctx.append("Refusees : ").append(refusees).append(" (").append(pourcentage(refusees, total)).append("%)\n");

        // Répartition par nature de manifestation (toujours calculée : peu coûteux, utile en contexte)
        Map<String, Object> repartitionNature = adminStatsService.statsParNatureManifestation(null, null, null);
        Object repartitionObj = repartitionNature.get("repartition");
        if (repartitionObj instanceof Map<?, ?> repartitionMap && !repartitionMap.isEmpty()) {
            ctx.append("\n=== REPARTITION PAR NATURE DE MANIFESTATION ===\n");
            ctx.append("Nombre de types differents : ").append(repartitionMap.size()).append("\n");
            for (Map.Entry<?, ?> e : repartitionMap.entrySet()) {
                ctx.append("• ").append(e.getKey()).append(" : ").append(e.getValue()).append(" reservation(s)\n");
            }
        }

        // Stats par mois — seulement si la question évoque explicitement une notion temporelle,
        // pour ne pas alourdir inutilement le contexte a chaque question de taux/type.
        if (question.contains("mois") || question.contains("evolution") || question.contains("annee")) {
            Map<String, Object> statsMois = adminStatsService.statsParMois(String.valueOf(today.getYear()));
            Object parMoisObj = statsMois.get("parMois");
            if (parMoisObj instanceof Map<?, ?> parMoisMap) {
                ctx.append("\n=== RESERVATIONS PAR MOIS (").append(today.getYear()).append(") ===\n");
                for (Map.Entry<?, ?> e : parMoisMap.entrySet()) {
                    ctx.append("• ").append(e.getKey()).append(" : ").append(e.getValue()).append("\n");
                }
            }
        }

        ctx.append("\n[INSTRUCTION] Reponds UNIQUEMENT a ce qui est demande, de maniere courte et precise ");
        ctx.append("(1 a 2 phrases). Si la question porte sur un seul chiffre (ex: juste le taux de refus), ");
        ctx.append("ne donne que ce chiffre-la, sans reciter toutes les statistiques ci-dessus.\n");

        return ctx.toString();
    }

    private double pourcentage(long partie, long total) {
        if (total == 0) return 0.0;
        return Math.round((partie * 10000.0) / total) / 100.0;
    }

    // ==========================================
    //  EXTRACTION DES PARAMÈTRES
    // ==========================================

    private int extractCapacite(String question) {
        Pattern p = Pattern.compile("(\\d+)\\s*(personne|pers|people|pax|participant|place)");
        Matcher m = p.matcher(question);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    private LocalDate extractDate(String question) {
        LocalDate today = LocalDate.now();

        if (question.contains("aujourd"))                                         return today;
        if (question.contains("demain"))                                          return today.plusDays(1);
        if (question.contains("apres-demain") || question.contains("apres demain")) return today.plusDays(2);

        Pattern p1 = Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{4})");
        Matcher m1 = p1.matcher(question);
        if (m1.find()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(m1.group(3)),
                        Integer.parseInt(m1.group(2)),
                        Integer.parseInt(m1.group(1)));
            } catch (Exception ignored) {}
        }

        Pattern p2 = Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})(?![/\\-\\d])");
        Matcher m2 = p2.matcher(question);
        if (m2.find()) {
            try {
                int day   = Integer.parseInt(m2.group(1));
                int month = Integer.parseInt(m2.group(2));
                LocalDate candidate = LocalDate.of(today.getYear(), month, day);
                if (candidate.isBefore(today)) candidate = candidate.plusYears(1);
                return candidate;
            } catch (Exception ignored) {}
        }

        Map<String, Integer> moisFr = new LinkedHashMap<>();
        moisFr.put("janvier", 1);  moisFr.put("fevrier", 2);  moisFr.put("mars", 3);
        moisFr.put("avril", 4);    moisFr.put("mai", 5);       moisFr.put("juin", 6);
        moisFr.put("juillet", 7);  moisFr.put("aout", 8);      moisFr.put("septembre", 9);
        moisFr.put("octobre", 10); moisFr.put("novembre", 11); moisFr.put("decembre", 12);

        for (Map.Entry<String, Integer> entry : moisFr.entrySet()) {
            Pattern pMois = Pattern.compile("(\\d{1,2})\\s+" + entry.getKey() + "(?:\\s+(\\d{4}))?");
            Matcher mMois = pMois.matcher(question);
            if (mMois.find()) {
                try {
                    int day  = Integer.parseInt(mMois.group(1));
                    int year = mMois.group(2) != null
                            ? Integer.parseInt(mMois.group(2))
                            : today.getYear();
                    LocalDate candidate = LocalDate.of(year, entry.getValue(), day);
                    // Si aucune annee n'est precisee et que la date calculee est deja passee,
                    // on suppose l'annee suivante (comportement volontaire).
                    // Si une annee EST precisee explicitement (ex: "19 janvier 2025"),
                    // on la respecte telle quelle, meme si elle est passee — c'est justement
                    // ce cas que le garde-fou ci-dessus doit intercepter.
                    if (mMois.group(2) == null && candidate.isBefore(today)) {
                        candidate = candidate.plusYears(1);
                    }
                    return candidate;
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    private LocalTime extractHeureDebut(String question) {
        Pattern p = Pattern.compile("(?:de|a|partir de|dès|des)\\s*(\\d{1,2})(?:h|:)(\\d{0,2})");
        Matcher m = p.matcher(question);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            int min = m.group(2).isEmpty() ? 0 : Integer.parseInt(m.group(2));
            try { return LocalTime.of(h, min); } catch (Exception ignored) {}
        }
        return null;
    }

    private LocalTime extractHeureFin(String question) {
        Pattern p = Pattern.compile("(?:jusqu'a|jusqu a|jusqu|a|fin|—|->)\\s*(\\d{1,2})(?:h|:)(\\d{0,2})");
        Matcher m = p.matcher(question);
        LocalTime last = null;
        while (m.find()) {
            int h = Integer.parseInt(m.group(1));
            int min = m.group(2).isEmpty() ? 0 : Integer.parseInt(m.group(2));
            try { last = LocalTime.of(h, min); } catch (Exception ignored) {}
        }
        return last;
    }

    private String detecterTypeEvent(String question) {
        if (question.contains("reunion") || question.contains("meeting"))             return "REUNION";
        if (question.contains("conference") || question.contains("seminaire"))        return "CONFERENCE";
        if (question.contains("evenement") || question.contains("event")
                || question.contains("gala") || question.contains("ceremonie"))      return "EVENEMENT";
        if (question.contains("coworking") || question.contains("teletravail")
                || question.contains("bureau"))                                        return "COWORKING";
        if (question.contains("formation") || question.contains("cours")
                || question.contains("atelier") || question.contains("workshop"))    return "FORMATION";
        return "";
    }

    // ==========================================
    //  CLASSES INTERNES
    // ==========================================

    private static class SalleScore {
        Salle salle;
        int score;
        boolean disponibleCreneau;
        List<String> raisons;

        SalleScore(Salle salle, int score, boolean disponibleCreneau, List<String> raisons) {
            this.salle = salle;
            this.score = score;
            this.disponibleCreneau = disponibleCreneau;
            this.raisons = raisons;
        }

        int getScore() { return score; }
    }

    private static class ResultatScore {
        int score;
        boolean disponibleCreneauDemande;
        List<String> raisons;

        ResultatScore(int score, boolean dispo, List<String> raisons) {
            this.score = score;
            this.disponibleCreneauDemande = dispo;
            this.raisons = raisons;
        }
    }
}