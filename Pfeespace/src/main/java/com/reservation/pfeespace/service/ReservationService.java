package com.reservation.pfeespace.service;

import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.entity.Salle;
import com.reservation.pfeespace.entity.User;
import com.reservation.pfeespace.repository.ReservationRepository;
import com.reservation.pfeespace.repository.SalleRepository;
import com.reservation.pfeespace.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SalleRepository salleRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final FactureService factureService;
    private final NotificationService notificationService;
    private final CheckinService checkinService;

    public ReservationService(ReservationRepository reservationRepository,
                              SalleRepository salleRepository,
                              EmailService emailService,
                              UserRepository userRepository,
                              @Lazy FactureService factureService,
                              NotificationService notificationService,
                              CheckinService checkinService) {
        this.reservationRepository = reservationRepository;
        this.salleRepository = salleRepository;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.factureService = factureService;
        this.notificationService = notificationService;
        this.checkinService = checkinService;
    }

    public List<Reservation> getAll() {
        return reservationRepository.findAll();
    }

    public List<Reservation> getByStatut(String statut) {
        return reservationRepository.findByStatut(statut);
    }

    public boolean verifierDisponibilite(Long salleId, String date,
                                         String heureDebut, String heureFin) {
        LocalDate d = LocalDate.parse(date);
        LocalTime debut = LocalTime.parse(heureDebut);
        LocalTime fin = LocalTime.parse(heureFin);

        List<Reservation> reservations =
                reservationRepository.findBySalleIdAndDateAndStatutNotIn(
                        salleId, d, List.of("REFUSEE", "NO_SHOW"));

        for (Reservation r : reservations) {
            boolean chevauche = debut.isBefore(r.getHeureFin()) && fin.isAfter(r.getHeureDebut());
            if (chevauche) return false;
        }
        return true;
    }

    public Reservation create(Long salleId, Reservation reservation, String email) {
        Salle salle = salleRepository.findById(salleId)
                .orElseThrow(() -> new RuntimeException("Salle non trouvée"));

        boolean disponible = verifierDisponibilite(salleId,
                reservation.getDate().toString(),
                reservation.getHeureDebut().toString(),
                reservation.getHeureFin().toString());

        if (!disponible) throw new RuntimeException("Créneau indisponible !");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User non trouvé"));

        reservation.setSalle(salle);
        reservation.setUser(user);
        reservation.setStatut("EN_ATTENTE");

        Reservation saved = reservationRepository.save(reservation);

        emailService.envoyerEmailAttente(
                reservation.getMail(), reservation.getNomComplet(),
                salle.getNom(), reservation.getDate().toString());

        notificationService.creerNotification(
                user,
                "Réservation en attente",
                "Votre demande de réservation pour la salle " + salle.getNom() + " le " + reservation.getDate() + " est en attente de traitement.",
                "RESERVATION_ATTENTE",
                "/mes-reservations");

        // ✅ AJOUT : notifier les admins en temps réel (WebSocket)
        notificationService.notifierAdmins(
                "Nouvelle réservation",
                reservation.getNomComplet() + " a demandé la salle " + salle.getNom()
                        + " le " + reservation.getDate() + ".",
                "NOUVELLE_RESERVATION",
                "/admin/pre-reservations");

        return saved;
    }

    public Reservation confirmer(Long id) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        r.setStatut("CONFIRMEE");

        // ✅ AJOUT : génère le token QR pour le check-in avant la sauvegarde finale
        checkinService.genererQrToken(r);

        Reservation saved = reservationRepository.save(r);

        try {
            factureService.genererFacture(saved);
        } catch (IOException e) {
            System.err.println("Erreur génération facture : " + e.getMessage());
        }

        emailService.envoyerEmailConfirmation(
                r.getMail(), r.getNomComplet(), r.getSalle().getNom(),
                r.getDate().toString(), r.getHeureDebut().toString(),
                r.getHeureFin().toString());

        notificationService.creerNotification(
                r.getUser(),
                "Réservation confirmée",
                "Votre réservation pour la salle " + r.getSalle().getNom() + " le " + r.getDate() + " a été confirmée.",
                "RESERVATION_CONFIRMEE",
                "/mes-reservations");

        return saved;
    }

    public Reservation refuser(Long id) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        r.setStatut("REFUSEE");
        Reservation saved = reservationRepository.save(r);

        emailService.envoyerEmailRefus(
                r.getMail(), r.getNomComplet(),
                r.getSalle().getNom(), r.getDate().toString());

        notificationService.creerNotification(
                r.getUser(),
                "Réservation refusée",
                "Votre réservation pour la salle " + r.getSalle().getNom() + " le " + r.getDate() + " a été refusée.",
                "RESERVATION_REFUSEE",
                "/mes-reservations");

        return saved;
    }

    public Reservation mettreEnAttente(Long id) {
        Reservation r = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        r.setStatut("EN_ATTENTE");
        Reservation saved = reservationRepository.save(r);

        emailService.envoyerEmailAttente(
                r.getMail(), r.getNomComplet(),
                r.getSalle().getNom(), r.getDate().toString());

        notificationService.creerNotification(
                r.getUser(),
                "Réservation en attente",
                "Votre réservation pour la salle " + r.getSalle().getNom() + " est en attente de traitement.",
                "RESERVATION_ATTENTE",
                "/mes-reservations");

        return saved;
    }

    public void delete(Long id) {
        reservationRepository.deleteById(id);
    }

    public List<Reservation> getMesReservations(String email) {
        return reservationRepository.findByUserEmail(email);
    }

    public Reservation createByAdmin(Long salleId, Reservation reservation) {
        Salle salle = salleRepository.findById(salleId)
                .orElseThrow(() -> new RuntimeException("Salle non trouvée"));

        boolean disponible = verifierDisponibilite(salleId,
                reservation.getDate().toString(),
                reservation.getHeureDebut().toString(),
                reservation.getHeureFin().toString());

        if (!disponible) throw new RuntimeException("Créneau indisponible !");

        reservation.setSalle(salle);
        reservation.setStatut("CONFIRMEE");

        Reservation saved = reservationRepository.save(reservation);

        try {
            factureService.genererFacture(saved);
        } catch (IOException e) {
            System.err.println("Erreur génération facture admin : " + e.getMessage());
        }

        if (reservation.getMail() != null && !reservation.getMail().isEmpty()) {
            emailService.envoyerEmailConfirmation(
                    reservation.getMail(), reservation.getNomComplet(), salle.getNom(),
                    reservation.getDate().toString(),
                    reservation.getHeureDebut().toString(),
                    reservation.getHeureFin().toString());
        }

        if (reservation.getUser() != null) {
            notificationService.creerNotification(
                    reservation.getUser(),
                    "Réservation confirmée",
                    "Votre réservation pour la salle " + salle.getNom() + " le " + reservation.getDate() + " a été confirmée.",
                    "RESERVATION_CONFIRMEE",
                    "/mes-reservations");
        }

        return saved;
    }
}