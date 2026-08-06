package com.reservation.pfeespace.service;

import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CheckinService {

    // Marge de tolérance : le client peut scanner jusqu'à X minutes après l'heure de début
    private static final long MARGE_TOLERANCE_MINUTES = 2;
    // Le client peut aussi scanner un peu en avance
    private static final long MARGE_AVANCE_MINUTES = 15;

    private final ReservationRepository reservationRepository;
    private final EvaluationService evaluationService; // AJOUT

    public CheckinService(ReservationRepository reservationRepository,
                          EvaluationService evaluationService) { // AJOUT du paramètre
        this.reservationRepository = reservationRepository;
        this.evaluationService = evaluationService; // AJOUT
    }

    // Appelée quand l'admin confirme une réservation : génère le token QR
    public String genererQrToken(Reservation reservation) {
        String token = UUID.randomUUID().toString();
        reservation.setQrToken(token);
        reservationRepository.save(reservation);
        return token;
    }

    // Appelée quand le client scanne le QR (arrivée sur /checkin/{token})
    public Reservation checkIn(String token) {
        System.out.println("=== CHECKIN APPELÉ avec token: " + token);
        Reservation reservation = reservationRepository.findByQrToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QR code invalide"));
        System.out.println("=== Réservation trouvée, statut actuel: " + reservation.getStatut());

        if (!"CONFIRMEE".equals(reservation.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cette réservation n'est plus valide (statut: " + reservation.getStatut() + ")");
        }

        LocalDateTime debut = LocalDateTime.of(reservation.getDate(), reservation.getHeureDebut());
        LocalDateTime fin = LocalDateTime.of(reservation.getDate(), reservation.getHeureFin());
        LocalDateTime maintenant = LocalDateTime.now();

        LocalDateTime debutTolere = debut.minusMinutes(MARGE_AVANCE_MINUTES);
        LocalDateTime finTolere = debut.plusMinutes(MARGE_TOLERANCE_MINUTES);

        if (maintenant.isBefore(debutTolere)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Trop tôt : le check-in n'ouvre que " + MARGE_AVANCE_MINUTES + " min avant le créneau");
        }
        if (maintenant.isAfter(finTolere)) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "Trop tard, le créneau de check-in est dépassé");
        }

        reservation.setCheckInTime(maintenant);
        reservation.setStatut("EN_COURS");
        return reservationRepository.save(reservation);
    }

    @Scheduled(fixedRate = 30 * 1000)  // toutes les 30 secondes
    public void controlerNoShowsEtCheckouts() {
        System.out.println("=== JOB NO-SHOW exécuté à " + LocalDateTime.now());

        LocalDate aujourdHui = LocalDate.now();
        LocalDateTime maintenant = LocalDateTime.now();

        // 1) Annuler les réservations confirmées non scannées à temps
        List<Reservation> confirmees = reservationRepository.findByStatutAndDate("CONFIRMEE", aujourdHui);
        System.out.println("=== " + confirmees.size() + " réservation(s) CONFIRMEE trouvée(s) pour aujourd'hui (" + aujourdHui + ")");

        for (Reservation r : confirmees) {
            LocalDateTime debut = LocalDateTime.of(r.getDate(), r.getHeureDebut());
            System.out.println("=== Résa id=" + r.getId() + " début=" + debut + " maintenant=" + maintenant + " limite=" + debut.plusMinutes(MARGE_TOLERANCE_MINUTES));

            if (maintenant.isAfter(debut.plusMinutes(MARGE_TOLERANCE_MINUTES))) {
                r.setStatut("NO_SHOW");
                reservationRepository.save(r);
                System.out.println("=== Résa id=" + r.getId() + " passée en NO_SHOW");
                // ⚠️ Pas d'évaluation ici : le client n'a pas scanné, donc pas de feedback
            }
        }

        // 2) Clôturer automatiquement les réservations en cours dont l'heure de fin est passée
        List<Reservation> enCours = reservationRepository.findByStatutAndDate("EN_COURS", aujourdHui);
        for (Reservation r : enCours) {
            LocalDateTime fin = LocalDateTime.of(r.getDate(), r.getHeureFin());
            if (maintenant.isAfter(fin)) {
                r.setCheckOutTime(maintenant);
                r.setStatut("TERMINEE");
                reservationRepository.save(r);
                evaluationService.genererEvaluationEtEnvoyer(r); // AJOUT : envoi du formulaire d'évaluation
            }
        }
    }
}