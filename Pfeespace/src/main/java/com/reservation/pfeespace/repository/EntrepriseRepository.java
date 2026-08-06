package com.reservation.pfeespace.repository;

import com.reservation.pfeespace.entity.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {

    // Vérifier num fiscal
    Optional<Entreprise> findByNumFiscal(String numFiscal);

    // Vérifier si existe
    boolean existsByNumFiscal(String numFiscal);
}