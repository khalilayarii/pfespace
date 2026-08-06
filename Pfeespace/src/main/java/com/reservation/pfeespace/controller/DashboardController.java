package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.repository.ReservationRepository;
import com.reservation.pfeespace.repository.SalleRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    private final ReservationRepository reservationRepository;
    private final SalleRepository salleRepository;

    public DashboardController(ReservationRepository reservationRepository,
                               SalleRepository salleRepository) {
        this.reservationRepository = reservationRepository;
        this.salleRepository = salleRepository;
    }

    // ================= 1. KPI CARDS =================
    @GetMapping("/kpis")
    public Map<String, Object> getKpis() {
        List<Reservation> all = reservationRepository.findAll();

        long totalReservations = all.size();
        long totalSalles = salleRepository.count();

        long totalEntreprises = all.stream()
                .map(Reservation::getSociete)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .count();

        double dureeMoyenneHeures = all.stream()
                .filter(r -> r.getHeureDebut() != null && r.getHeureFin() != null)
                .mapToDouble(r -> Duration.between(r.getHeureDebut(), r.getHeureFin()).toMinutes() / 60.0)
                .average()
                .orElse(0.0);

        long reservantsActifs = all.stream()
                .map(r -> r.getUser() != null ? r.getUser().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        double heuresReservees = all.stream()
                .filter(r -> r.getHeureDebut() != null && r.getHeureFin() != null)
                .mapToDouble(r -> Duration.between(r.getHeureDebut(), r.getHeureFin()).toMinutes() / 60.0)
                .sum();
        double heuresOuverturePotentielles = totalSalles * 10.0 * 365.0;
        double tauxOccupation = heuresOuverturePotentielles > 0
                ? (heuresReservees / heuresOuverturePotentielles) * 100
                : 0.0;

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalReservations", totalReservations);
        kpis.put("totalSalles", totalSalles);
        kpis.put("totalEntreprises", totalEntreprises);
        kpis.put("dureeMoyenneHeures", Math.round(dureeMoyenneHeures * 10) / 10.0);
        kpis.put("reservantsActifs", reservantsActifs);
        kpis.put("tauxOccupation", Math.round(tauxOccupation * 10) / 10.0);
        return kpis;
    }

    // ================= 2. RÉSERVATIONS PAR MOIS (toutes années confondues par défaut) =================
    @GetMapping("/reservations-par-mois")
    public List<Map<String, Object>> getReservationsParMois(
            @RequestParam(required = false) Integer annee) {

        List<Reservation> reservations;

        if (annee != null) {
            LocalDate debut = LocalDate.of(annee, 1, 1);
            LocalDate fin = LocalDate.of(annee, 12, 31);
            reservations = reservationRepository.findByDateBetween(debut, fin);
        } else {
            reservations = reservationRepository.findAll();
        }

        Map<Month, Long> parMois = reservations.stream()
                .filter(r -> r.getDate() != null)
                .collect(Collectors.groupingBy(r -> r.getDate().getMonth(), Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Month mois : Month.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("mois", mois.getDisplayName(TextStyle.FULL, Locale.FRENCH));
            entry.put("total", parMois.getOrDefault(mois, 0L));
            result.add(entry);
        }
        return result;
    }

    // ================= 3. RÉSERVATIONS PAR STATUT =================
    @GetMapping("/reservations-par-statut")
    public List<Map<String, Object>> getReservationsParStatut() {
        List<Reservation> all = reservationRepository.findAll();

        Map<String, Long> parStatut = all.stream()
                .collect(Collectors.groupingBy(Reservation::getStatut, Collectors.counting()));

        return parStatut.entrySet().stream()
                .map(e -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("statut", e.getKey());
                    entry.put("total", e.getValue());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    // ================= 4. RÉSERVATIONS PAR SALLE =================
    @GetMapping("/reservations-par-salle")
    public List<Map<String, Object>> getReservationsParSalle() {
        List<Reservation> all = reservationRepository.findAll();

        Map<String, Long> parSalle = all.stream()
                .filter(r -> r.getSalle() != null)
                .collect(Collectors.groupingBy(r -> r.getSalle().getNom(), Collectors.counting()));

        return parSalle.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("salle", e.getKey());
                    entry.put("total", e.getValue());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    // ================= 5. RÉSERVATIONS PAR ANNÉE =================
    @GetMapping("/reservations-par-annee")
    public List<Map<String, Object>> getReservationsParAnnee() {
        List<Reservation> all = reservationRepository.findAll();

        Map<Integer, Long> parAnnee = all.stream()
                .filter(r -> r.getDate() != null)
                .collect(Collectors.groupingBy(r -> r.getDate().getYear(), Collectors.counting()));

        return new TreeMap<>(parAnnee).entrySet().stream()
                .map(e -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("annee", e.getKey());
                    entry.put("total", e.getValue());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    // ================= 6. RÉSERVATIONS PAR CRÉNEAU HORAIRE =================
    @GetMapping("/creneaux-horaires")
    public List<Map<String, Object>> getReservationsParCreneau() {
        List<Reservation> all = reservationRepository.findAll();

        LinkedHashMap<String, Long> creneaux = new LinkedHashMap<>();
        creneaux.put("08h-10h", 0L);
        creneaux.put("10h-12h", 0L);
        creneaux.put("12h-14h", 0L);
        creneaux.put("14h-16h", 0L);
        creneaux.put("16h-18h", 0L);
        creneaux.put("18h-20h", 0L);
        creneaux.put("Autre", 0L);

        for (Reservation r : all) {
            if (r.getHeureDebut() == null) continue;
            int heure = r.getHeureDebut().getHour();
            String creneau;
            if (heure >= 8 && heure < 10) creneau = "08h-10h";
            else if (heure >= 10 && heure < 12) creneau = "10h-12h";
            else if (heure >= 12 && heure < 14) creneau = "12h-14h";
            else if (heure >= 14 && heure < 16) creneau = "14h-16h";
            else if (heure >= 16 && heure < 18) creneau = "16h-18h";
            else if (heure >= 18 && heure < 20) creneau = "18h-20h";
            else creneau = "Autre";
            creneaux.merge(creneau, 1L, Long::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : creneaux.entrySet()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("creneau", e.getKey());
            entry.put("total", e.getValue());
            result.add(entry);
        }
        result.sort((a, b) -> Long.compare((Long) b.get("total"), (Long) a.get("total")));
        return result;
    }

    // ================= 7. GRATUIT VS PAYANT =================
    @GetMapping("/gratuit-vs-payant")
    public Map<String, Object> getGratuitVsPayant() {
        List<Reservation> all = reservationRepository.findAll();

        long nbGratuit = 0, nbPayant = 0;
        for (Reservation r : all) {
            String type = r.getTypeUtilisateur() != null ? r.getTypeUtilisateur().toUpperCase() : "";
            boolean estGratuit = type.equals("GRATUIT") || type.equals("INTERNE") || type.equals("ETUDIANT");
            boolean estPayant = type.equals("PAYANT") || type.equals("FREELANCE") || type.equals("SOCIETE");
            if (estGratuit) nbGratuit++;
            else if (estPayant) nbPayant++;
        }

        long total = nbGratuit + nbPayant;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nbGratuit", nbGratuit);
        result.put("nbPayant", nbPayant);
        result.put("pctGratuit", total > 0 ? Math.round((nbGratuit * 1000.0) / total) / 10.0 : 0);
        result.put("pctPayant", total > 0 ? Math.round((nbPayant * 1000.0) / total) / 10.0 : 0);
        return result;
    }
}