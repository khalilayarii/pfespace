// ✅ NOUVEAU FICHIER — Repository Facture
// Chemin : src/main/java/com/reservation/pfeespace/repository/FactureRepository.java

package com.reservation.pfeespace.repository;

import com.reservation.pfeespace.entity.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    // Trouver la facture liée à une réservation
    Optional<Facture> findByReservationId(Long reservationId);

    // Toutes les factures triées par date décroissante
    List<Facture> findAllByOrderByDateGenerationDesc();
}
