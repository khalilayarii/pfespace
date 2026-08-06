package com.reservation.pfeespace.repository;

import com.reservation.pfeespace.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByToken(String token);
    // ✅ AJOUT
    List<Evaluation> findByReservation_Salle_IdAndRemplieTrue(Long salleId);
}