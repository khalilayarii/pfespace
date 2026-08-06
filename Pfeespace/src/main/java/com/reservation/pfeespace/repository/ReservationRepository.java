package com.reservation.pfeespace.repository;

import com.reservation.pfeespace.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByStatut(String statut);

    // Réservations d'un utilisateur
    List<Reservation> findByUserEmail(String email);

    // Réservations d'une salle
    List<Reservation> findBySalleId(Long salleId);

    // ✅ AJOUT — Nombre de réservations liées à une salle (utilisé avant suppression)
    long countBySalleId(Long salleId);

    // Nombre de réservations d'un utilisateur
    long countByUserId(Long userId);

    // Réservations d'une salle à une date donnée sauf statut refusé
    List<Reservation> findBySalleIdAndDateAndStatutNot(
            Long salleId,
            LocalDate date,
            String statut
    );

    // ================= AJOUTS POUR LE CHATBOT ADMIN =================

    // Compte les réservations par statut (CONFIRMEE, EN_ATTENTE, REFUSEE)
    long countByStatut(String statut);

    // Compte les réservations sur une période donnée
    long countByDateBetween(LocalDate dateDebut, LocalDate dateFin);

    // Compte les réservations par statut ET par période, indépendamment de la salle
    long countByStatutAndDateBetween(String statut, LocalDate dateDebut, LocalDate dateFin);

    // Compte les réservations d'une salle, éventuellement filtrées par statut
    long countBySalleIdAndStatut(Long salleId, String statut);

    // Compte les réservations d'une salle sur une période (tous statuts)
    long countBySalleIdAndDateBetween(Long salleId, LocalDate dateDebut, LocalDate dateFin);

    // Compte les réservations d'une salle, sur une période, avec un statut précis
    long countBySalleIdAndStatutAndDateBetween(
            Long salleId, String statut, LocalDate dateDebut, LocalDate dateFin
    );
    // Liste des réservations sur une période (pour regroupement par type/mois)
    List<Reservation> findByDateBetween(LocalDate dateDebut, LocalDate dateFin);
    // Retrouver une réservation à partir de son QR token
    Optional<Reservation> findByQrToken(String qrToken);

    // Réservations confirmées d'une date donnée (pour le job de contrôle no-show)
    List<Reservation> findByStatutAndDate(String statut, LocalDate date);
    List<Reservation> findBySalleIdAndDateAndStatutNotIn(
            Long salleId,
            LocalDate date,
            List<String> statuts
    );

    // ================= AJOUTS PERFORMANCE (anti N+1) =================

    // Toutes les réservations pour un ensemble de salles en UNE requête
    // (remplace les appels findBySalleId() en boucle dans RagService)
    List<Reservation> findBySalleIdIn(List<Long> salleIds);

    // Réservations sur un ensemble de salles, à une date donnée, hors un statut
    // (remplace findBySalleIdAndDateAndStatutNot() appelé en boucle dans le scoring)
    List<Reservation> findBySalleIdInAndDateAndStatutNot(
            List<Long> salleIds,
            LocalDate date,
            String statut
    );

    // Nombre de réservations à une date précise (remplace un findAll() + filter en mémoire)
    long countByDate(LocalDate date);
}