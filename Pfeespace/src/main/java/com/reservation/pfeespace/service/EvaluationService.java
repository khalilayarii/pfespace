package com.reservation.pfeespace.service;

import com.reservation.pfeespace.entity.Evaluation;
import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.repository.EvaluationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EvaluationService(EvaluationRepository evaluationRepository, EmailService emailService) {
        this.evaluationRepository = evaluationRepository;
        this.emailService = emailService;
    }

    public void genererEvaluationEtEnvoyer(Reservation reservation) {
        Evaluation evaluation = new Evaluation();
        evaluation.setReservation(reservation);
        evaluation.setToken(UUID.randomUUID().toString());
        evaluation.setDateEnvoi(LocalDateTime.now());
        evaluationRepository.save(evaluation);

        String lien = frontendUrl + "/evaluation/" + evaluation.getToken();

        emailService.envoyerEmailEvaluation(
                reservation.getMail(),          // au lieu de reservation.getClient().getEmail()
                reservation.getNomComplet(),    // au lieu de reservation.getClient().getNomComplet()
                reservation.getSalle().getNom(),
                lien
        );
    }

    public Evaluation soumettreEvaluation(String token, Integer noteProprete, Integer noteEquipement,
                                          Integer noteFaciliteReservation, Boolean capaciteAdaptee,
                                          String problemesRencontres, String suggestionsAmelioration) {
        Evaluation evaluation = evaluationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Évaluation introuvable"));

        if (evaluation.isRemplie()) {
            throw new RuntimeException("Cette évaluation a déjà été soumise");
        }

        evaluation.setNoteProprete(noteProprete);
        evaluation.setNoteEquipement(noteEquipement);
        evaluation.setNoteFaciliteReservation(noteFaciliteReservation);
        evaluation.setCapaciteAdaptee(capaciteAdaptee);
        evaluation.setProblemesRencontres(problemesRencontres);
        evaluation.setSuggestionsAmelioration(suggestionsAmelioration);
        evaluation.setDateSoumission(LocalDateTime.now());
        evaluation.setRemplie(true);

        return evaluationRepository.save(evaluation);
    }
}