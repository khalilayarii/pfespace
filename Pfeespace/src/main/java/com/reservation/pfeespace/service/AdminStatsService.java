package com.reservation.pfeespace.service;

import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.entity.Role;
import com.reservation.pfeespace.entity.Salle;
import com.reservation.pfeespace.repository.ReservationRepository;
import com.reservation.pfeespace.repository.SalleRepository;
import com.reservation.pfeespace.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminStatsService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SalleRepository salleRepository;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

    private static final String[] MOIS_FR = {
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    // ==========================================
    //  NOUVEAU : TAUX DE RÉSERVATIONS (CONFIRMÉE / REFUSÉE / EN ATTENTE) EN %
    // ==========================================

    public Map<String, Object> tauxReservations(String dateDebut, String dateFin) {
        Map<String, Object> result = new HashMap<>();

        boolean periodeFournie = dateDebut != null && !dateDebut.isBlank()
                && dateFin != null && !dateFin.isBlank();
        LocalDate debut = null;
        LocalDate fin = null;
        if (periodeFournie) {
            try {
                debut = LocalDate.parse(dateDebut, ISO);
                fin = LocalDate.parse(dateFin, ISO);
            } catch (Exception e) {
                result.put("erreur", "Format de date invalide. Utilise le format AAAA-MM-JJ (ex: 2026-06-01).");
                return result;
            }
        }

        long total;
        long confirmees;
        long enAttente;
        long refusees;

        if (periodeFournie) {
            total = reservationRepository.countByDateBetween(debut, fin);
            confirmees = reservationRepository.countByStatutAndDateBetween("CONFIRMEE", debut, fin);
            enAttente = reservationRepository.countByStatutAndDateBetween("EN_ATTENTE", debut, fin);
            refusees = reservationRepository.countByStatutAndDateBetween("REFUSEE", debut, fin);
        } else {
            total = reservationRepository.count();
            confirmees = reservationRepository.countByStatut("CONFIRMEE");
            enAttente = reservationRepository.countByStatut("EN_ATTENTE");
            refusees = reservationRepository.countByStatut("REFUSEE");
        }

        result.put("total", total);
        result.put("confirmees", confirmees);
        result.put("enAttente", enAttente);
        result.put("refusees", refusees);
        result.put("tauxConfirmation", pourcentage(confirmees, total));
        result.put("tauxRefus", pourcentage(refusees, total));
        result.put("tauxEnAttente", pourcentage(enAttente, total));

        if (periodeFournie) {
            result.put("dateDebut", dateDebut);
            result.put("dateFin", dateFin);
        }
        return result;
    }

    // ==========================================
    //  NOUVEAU : RÉPARTITION PAR NATURE DE MANIFESTATION
    // ==========================================

    public Map<String, Object> statsParNatureManifestation(String dateDebut, String dateFin, String statut) {
        Map<String, Object> result = new HashMap<>();

        String s = normaliserStatut(statut);
        if (statut != null && !statut.isBlank() && s == null) {
            result.put("erreur", "Statut inconnu. Valeurs possibles : CONFIRMEE, EN_ATTENTE, REFUSEE.");
            return result;
        }

        boolean periodeFournie = dateDebut != null && !dateDebut.isBlank()
                && dateFin != null && !dateFin.isBlank();
        List<Reservation> reservations;

        if (periodeFournie) {
            try {
                LocalDate debut = LocalDate.parse(dateDebut, ISO);
                LocalDate fin = LocalDate.parse(dateFin, ISO);
                reservations = reservationRepository.findByDateBetween(debut, fin);
            } catch (Exception e) {
                result.put("erreur", "Format de date invalide. Utilise le format AAAA-MM-JJ (ex: 2026-06-01).");
                return result;
            }
        } else {
            reservations = reservationRepository.findAll();
        }

        if (s != null) {
            reservations = reservations.stream()
                    .filter(r -> s.equals(r.getStatut()))
                    .collect(Collectors.toList());
        }

        Map<String, Long> repartition = reservations.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getNatureManifestation() != null ? r.getNatureManifestation() : "NON_RENSEIGNE",
                        Collectors.counting()
                ));

        result.put("repartition", repartition);
        result.put("total", (long) reservations.size());
        if (s != null) result.put("statutFiltre", s);
        if (periodeFournie) {
            result.put("dateDebut", dateDebut);
            result.put("dateFin", dateFin);
        }
        return result;
    }

    // ==========================================
    //  NOUVEAU : STATISTIQUES PAR MOIS
    // ==========================================

    public Map<String, Object> statsParMois(String annee) {
        Map<String, Object> result = new HashMap<>();

        int anneeVoulue;
        try {
            anneeVoulue = (annee != null && !annee.isBlank())
                    ? Integer.parseInt(annee.trim())
                    : LocalDate.now().getYear();
        } catch (NumberFormatException e) {
            result.put("erreur", "Année invalide. Utilise le format AAAA (ex: 2026).");
            return result;
        }

        LocalDate debutAnnee = LocalDate.of(anneeVoulue, 1, 1);
        LocalDate finAnnee = LocalDate.of(anneeVoulue, 12, 31);

        List<Reservation> reservations = reservationRepository.findByDateBetween(debutAnnee, finAnnee);

        Map<String, Long> parMois = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            parMois.put(MOIS_FR[i], 0L);
        }

        for (Reservation r : reservations) {
            if (r.getDate() != null) {
                String nomMois = MOIS_FR[r.getDate().getMonthValue() - 1];
                parMois.merge(nomMois, 1L, Long::sum);
            }
        }

        result.put("annee", anneeVoulue);
        result.put("parMois", parMois);
        result.put("total", (long) reservations.size());
        return result;
    }

    // ==========================================
    //  NOUVEAU : OUTIL GÉNÉRAL RÉSERVATIONS
    //  (statut ET/OU période, tous deux optionnels, sans dépendre d'une salle)
    // ==========================================

    public Map<String, Object> countReservations(String statut, String dateDebut, String dateFin) {
        Map<String, Object> result = new HashMap<>();

        String s = normaliserStatut(statut);
        if (statut != null && !statut.isBlank() && s == null) {
            result.put("erreur", "Statut inconnu. Valeurs possibles : CONFIRMEE, EN_ATTENTE, REFUSEE.");
            return result;
        }

        boolean periodeFournie = dateDebut != null && !dateDebut.isBlank()
                && dateFin != null && !dateFin.isBlank();
        LocalDate debut = null;
        LocalDate fin = null;
        if (periodeFournie) {
            try {
                debut = LocalDate.parse(dateDebut, ISO);
                fin = LocalDate.parse(dateFin, ISO);
            } catch (Exception e) {
                result.put("erreur", "Format de date invalide. Utilise le format AAAA-MM-JJ (ex: 2026-06-01).");
                return result;
            }
        }

        long total;
        if (s != null && periodeFournie) {
            total = reservationRepository.countByStatutAndDateBetween(s, debut, fin);
        } else if (s != null) {
            total = reservationRepository.countByStatut(s);
        } else if (periodeFournie) {
            total = reservationRepository.countByDateBetween(debut, fin);
        } else {
            total = reservationRepository.count();
        }

        result.put("total", total);
        if (s != null) result.put("statut", s);
        if (periodeFournie) {
            result.put("dateDebut", dateDebut);
            result.put("dateFin", dateFin);
        }
        return result;
    }

    // ==========================================
    //  NOUVEAU : DISPONIBILITÉ DES SALLES
    // ==========================================

    public Map<String, Object> countSallesDisponibles() {
        Map<String, Object> result = new HashMap<>();
        long total = salleRepository.count();
        long disponibles = salleRepository.countByDisponible(true);
        long indisponibles = total - disponibles;
        result.put("total", total);
        result.put("disponibles", disponibles);
        result.put("indisponibles", indisponibles);
        return result;
    }

    // ==========================================
    //  MÉTHODES EXISTANTES (inchangées)
    // ==========================================

    public Map<String, Object> countReservationsByStatut(String statut) {
        Map<String, Object> result = new HashMap<>();
        String s = normaliserStatut(statut);
        if (s == null) {
            result.put("erreur", "Statut inconnu. Valeurs possibles : CONFIRMEE, EN_ATTENTE, REFUSEE.");
            return result;
        }
        result.put("statut", s);
        result.put("total", reservationRepository.countByStatut(s));
        return result;
    }

    public Map<String, Object> countTotalReservations() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", reservationRepository.count());
        result.put("confirmees", reservationRepository.countByStatut("CONFIRMEE"));
        result.put("enAttente", reservationRepository.countByStatut("EN_ATTENTE"));
        result.put("refusees", reservationRepository.countByStatut("REFUSEE"));
        return result;
    }

    public Map<String, Object> countUsers() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", userRepository.count());
        result.put("clients", userRepository.countByRole(Role.CLIENT));
        result.put("admins", userRepository.countByRole(Role.ADMIN));
        return result;
    }

    public Map<String, Object> countUsersByRole(String role) {
        Map<String, Object> result = new HashMap<>();
        try {
            Role r = Role.valueOf(role.trim().toUpperCase());
            result.put("role", r.name());
            result.put("total", userRepository.countByRole(r));
        } catch (Exception e) {
            result.put("erreur", "Rôle inconnu. Valeurs possibles : CLIENT, ADMIN.");
        }
        return result;
    }

    public Map<String, Object> countReservationsByPeriode(String dateDebut, String dateFin) {
        Map<String, Object> result = new HashMap<>();
        try {
            LocalDate debut = LocalDate.parse(dateDebut, ISO);
            LocalDate fin = LocalDate.parse(dateFin, ISO);
            result.put("dateDebut", dateDebut);
            result.put("dateFin", dateFin);
            result.put("total", reservationRepository.countByDateBetween(debut, fin));
        } catch (Exception e) {
            result.put("erreur", "Format de date invalide. Utilise le format AAAA-MM-JJ (ex: 2026-07-01).");
        }
        return result;
    }

    public Map<String, Object> countReservationsBySalle(String nomSalle, String statut,
                                                        String dateDebut, String dateFin) {
        Map<String, Object> result = new HashMap<>();
        if (nomSalle == null || nomSalle.isBlank()) {
            result.put("erreur", "Nom de salle manquant.");
            return result;
        }

        List<Salle> matches = salleRepository.findAll().stream()
                .filter(s -> s.getNom() != null &&
                        s.getNom().toLowerCase().contains(nomSalle.toLowerCase()))
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            result.put("erreur", "Aucune salle trouvée avec ce nom : " + nomSalle);
            return result;
        }

        String s = normaliserStatut(statut);

        LocalDate debut = null;
        LocalDate fin = null;
        boolean periodeFournie = dateDebut != null && !dateDebut.isBlank()
                && dateFin != null && !dateFin.isBlank();
        if (periodeFournie) {
            try {
                debut = LocalDate.parse(dateDebut, ISO);
                fin = LocalDate.parse(dateFin, ISO);
            } catch (Exception e) {
                result.put("erreur", "Format de date invalide. Utilise le format AAAA-MM-JJ (ex: 2026-06-01).");
                return result;
            }
        }

        for (Salle salle : matches) {
            long total;
            if (s != null && periodeFournie) {
                total = reservationRepository.countBySalleIdAndStatutAndDateBetween(salle.getId(), s, debut, fin);
            } else if (s != null) {
                total = reservationRepository.countBySalleIdAndStatut(salle.getId(), s);
            } else if (periodeFournie) {
                total = reservationRepository.countBySalleIdAndDateBetween(salle.getId(), debut, fin);
            } else {
                total = reservationRepository.findBySalleId(salle.getId()).size();
            }
            result.put(salle.getNom(), total);
        }
        if (s != null) result.put("statutFiltre", s);
        if (periodeFournie) {
            result.put("dateDebut", dateDebut);
            result.put("dateFin", dateFin);
        }
        return result;
    }

    public Map<String, Object> repartitionReservationsParSalle() {
        Map<String, Object> result = new HashMap<>();
        List<Salle> salles = salleRepository.findAll();
        for (Salle salle : salles) {
            long total = reservationRepository.findBySalleId(salle.getId()).size();
            result.put(salle.getNom(), total);
        }
        return result;
    }

    private String normaliserStatut(String statut) {
        if (statut == null || statut.isBlank()) return null;
        String s = statut.trim().toUpperCase();
        if (s.equals("CONFIRMEE") || s.equals("EN_ATTENTE") || s.equals("REFUSEE")) return s;
        return null;
    }

    // Calcule un pourcentage arrondi à 2 décimales, retourne 0.0 si total = 0
    private double pourcentage(long partie, long total) {
        if (total == 0) return 0.0;
        return Math.round((partie * 10000.0) / total) / 100.0;
    }
}