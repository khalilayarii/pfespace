package com.reservation.pfeespace.service;

import com.reservation.pfeespace.dto.AvisDTO;
import com.reservation.pfeespace.dto.SalleAvisDTO;
import com.reservation.pfeespace.entity.Evaluation;
import com.reservation.pfeespace.entity.Salle;
import com.reservation.pfeespace.repository.EvaluationRepository;
import com.reservation.pfeespace.repository.ReservationRepository;
import com.reservation.pfeespace.repository.SalleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalleService {

    private final SalleRepository salleRepository;
    private final NotificationService notificationService;
    // ✅ AJOUT — pour vérifier les réservations liées avant suppression
    private final ReservationRepository reservationRepository;
    private final EvaluationRepository evaluationRepository; // ✅ AJOUT

    public SalleService(SalleRepository salleRepository,
                        NotificationService notificationService,
                        ReservationRepository reservationRepository,
                        EvaluationRepository evaluationRepository) { // ✅ AJOUT
        this.salleRepository = salleRepository;
        this.notificationService = notificationService;
        this.reservationRepository = reservationRepository;
        this.evaluationRepository = evaluationRepository; // ✅ AJOUT
    }

    public List<Salle> getAll() {
        return salleRepository.findAll();
    }

    public Salle getById(Long id) {
        return salleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Salle non trouvée"));
    }

    public Salle create(Salle salle) {
        Salle saved = salleRepository.save(salle);

        // ✅ Notifier tous les clients actifs d'une nouvelle salle
        notificationService.notifierTousLesClients(
                "Nouvelle salle disponible",
                "La salle " + saved.getNom() + " est maintenant disponible à la réservation.",
                "NOUVELLE_SALLE",
                "/salles");

        return saved;
    }

    public Salle update(Long id, Salle salle) {
        Salle existing = getById(id);
        existing.setNom(salle.getNom());
        existing.setDescription(salle.getDescription());
        existing.setCapacite(salle.getCapacite());
        existing.setEquipement(salle.getEquipement());
        existing.setPrix(salle.getPrix());
        existing.setDisponible(salle.isDisponible());
        existing.setImage(salle.getImage());
        return salleRepository.save(existing);
    }

    // ✅ MODIFIÉ — vérifie qu'aucune réservation n'est liée avant de supprimer
    public void delete(Long id) {
        Salle salle = getById(id);

        long nbReservations = reservationRepository.countBySalleId(id);
        if (nbReservations > 0) {
            throw new RuntimeException(
                    "Impossible de supprimer la salle \"" + salle.getNom() + "\" : " +
                            nbReservations + " réservation(s) y sont encore liée(s). " +
                            "Marquez plutôt la salle comme indisponible, ou supprimez d'abord ses réservations.");
        }

        salleRepository.deleteById(id);
    }

    // ✅ AJOUT — score + avis pour une salle
    public SalleAvisDTO getAvisEtScore(Long salleId) {
        List<Evaluation> evaluations = evaluationRepository.findByReservation_Salle_IdAndRemplieTrue(salleId);
        List<AvisDTO> avisDTOs = evaluations.stream()
                .map(e -> new AvisDTO(
                        e.getReservation().getNomComplet(),
                        noteGlobale(e),
                        e.getSuggestionsAmelioration(),
                        e.getDateSoumission()
                ))
                .filter(a -> a.commentaire() != null && !a.commentaire().isBlank()) // ✅ record accessor
                .toList();
        double scoreMoyen = evaluations.stream()
                .mapToDouble(this::noteGlobale)
                .average()
                .orElse(0.0);
        return new SalleAvisDTO(scoreMoyen, evaluations.size(), avisDTOs);
    }

    private double noteGlobale(Evaluation e) {
        int somme = e.getNoteProprete() + e.getNoteEquipement() + e.getNoteFaciliteReservation();
        return somme / 3.0;
    }
}