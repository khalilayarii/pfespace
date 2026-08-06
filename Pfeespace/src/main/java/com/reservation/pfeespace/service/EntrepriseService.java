package com.reservation.pfeespace.service;

import com.reservation.pfeespace.dto.EntrepriseDTO;
import com.reservation.pfeespace.entity.Entreprise;
import com.reservation.pfeespace.repository.EntrepriseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EntrepriseService {

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    // Récupérer toutes les entreprises
    public List<EntrepriseDTO> getAll() {
        return entrepriseRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Ajouter une entreprise
    public EntrepriseDTO create(EntrepriseDTO dto) {
        Entreprise e = toEntity(dto);
        return toDTO(entrepriseRepository.save(e));
    }

    // Modifier une entreprise
    public EntrepriseDTO update(Long id, EntrepriseDTO dto) {
        Entreprise e = entrepriseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));
        e.setNom(dto.getNom());
        e.setNumFiscal(dto.getNumFiscal());
        e.setAdresse(dto.getAdresse());
        e.setEmail(dto.getEmail());
        e.setTelephone(dto.getTelephone());
        e.setEstMembre(dto.getEstMembre());
        return toDTO(entrepriseRepository.save(e));
    }

    // Supprimer une entreprise
    public void delete(Long id) {
        entrepriseRepository.deleteById(id);
    }

    // Vérifier num fiscal → pour le formulaire réservation
    public EntrepriseDTO verifierNumFiscal(String numFiscal) {
        Optional<Entreprise> entreprise = entrepriseRepository.findByNumFiscal(numFiscal);
        return entreprise.map(this::toDTO).orElse(null);
    }

    // Convertir Entity → DTO
    private EntrepriseDTO toDTO(Entreprise e) {
        EntrepriseDTO dto = new EntrepriseDTO();
        dto.setId(e.getId());
        dto.setNom(e.getNom());
        dto.setNumFiscal(e.getNumFiscal());
        dto.setAdresse(e.getAdresse());
        dto.setEmail(e.getEmail());
        dto.setTelephone(e.getTelephone());
        dto.setEstMembre(e.getEstMembre());
        return dto;
    }

    // Convertir DTO → Entity
    private Entreprise toEntity(EntrepriseDTO dto) {
        Entreprise e = new Entreprise();
        e.setNom(dto.getNom());
        e.setNumFiscal(dto.getNumFiscal());
        e.setAdresse(dto.getAdresse());
        e.setEmail(dto.getEmail());
        e.setTelephone(dto.getTelephone());
        e.setEstMembre(dto.getEstMembre());
        return e;
    }
}