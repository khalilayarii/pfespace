package com.reservation.pfeespace.repository;

import com.reservation.pfeespace.entity.Salle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {

    // Compte les salles selon leur disponibilité (true = disponibles, false = indisponibles)
    long countByDisponible(boolean disponible);
}