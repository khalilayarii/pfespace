package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.security.JwtUtil;
import com.reservation.pfeespace.service.ReservationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "http://localhost:4200")
public class ReservationController {

    private final ReservationService reservationService;
    private final JwtUtil jwtUtil;

    public ReservationController(ReservationService reservationService,
                                 JwtUtil jwtUtil) {
        this.reservationService = reservationService;
        this.jwtUtil = jwtUtil;
    }

    // Récupérer toutes les réservations ou filtrer par statut
    @GetMapping
    public List<Reservation> getAll(@RequestParam(required = false) String statut) {
        if (statut != null) {
            return reservationService.getByStatut(statut);
        }
        return reservationService.getAll();
    }

    // Créer une réservation par un client (statut par défaut = EN_ATTENTE)
    @PostMapping("/{salleId}")
    public ResponseEntity<Reservation> create(
            @PathVariable Long salleId,
            @RequestBody Reservation reservation,
            HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        String email = jwtUtil.extractEmail(token);

        // Forcer le statut par défaut à EN_ATTENTE
        reservation.setStatut("EN_ATTENTE");

        return ResponseEntity.ok(reservationService.create(salleId, reservation, email));
    }

    // Confirmer une réservation
    @PutMapping("/{id}/confirmer")
    public ResponseEntity<Reservation> confirmer(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirmer(id));
    }

    // Refuser une réservation
    @PutMapping("/{id}/refuser")
    public ResponseEntity<Reservation> refuser(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.refuser(id));
    }

    // Mettre une réservation en attente
    @PutMapping("/{id}/attente")
    public ResponseEntity<Reservation> attente(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.mettreEnAttente(id));
    }

    // Supprimer une réservation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Récupérer les réservations du client connecté
    @GetMapping("/mes-reservations")
    public List<Reservation> getMesReservations(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        String email = jwtUtil.extractEmail(token);
        return reservationService.getMesReservations(email);
    }

    // Créer une réservation par un admin
    @PostMapping("/admin/{salleId}")
    public ResponseEntity<Reservation> createByAdmin(
            @PathVariable Long salleId,
            @RequestBody Reservation reservation) {
        return ResponseEntity.ok(reservationService.createByAdmin(salleId, reservation));
    }

    // Vérifier la disponibilité d'une salle
    @GetMapping("/disponibilite")
    public ResponseEntity<Boolean> verifierDisponibilite(
            @RequestParam Long salleId,
            @RequestParam String date,
            @RequestParam String heureDebut,
            @RequestParam String heureFin) {
        boolean disponible = reservationService.verifierDisponibilite(
                salleId, date, heureDebut, heureFin);
        return ResponseEntity.ok(disponible);
    }
}
