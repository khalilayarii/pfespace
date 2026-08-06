package com.reservation.pfeespace.service;

import com.reservation.pfeespace.entity.Evaluation;
import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.entity.Salle;
import com.reservation.pfeespace.repository.EvaluationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private EvaluationService evaluationService;

    private Reservation reservation;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(evaluationService, "frontendUrl", "http://localhost:4200");

        Salle salle = new Salle();
        salle.setNom("Salle A");

        reservation = new Reservation();
        reservation.setMail("jean.dupont@test.com");
        reservation.setNomComplet("Jean Dupont");
        reservation.setSalle(salle);
    }

    @Test
    void genererEvaluationEtEnvoyer_shouldSaveEvaluationAndSendEmail() {
        doReturn(new Evaluation()).when(evaluationRepository).save(any(Evaluation.class));

        evaluationService.genererEvaluationEtEnvoyer(reservation);

        ArgumentCaptor<Evaluation> captor = ArgumentCaptor.forClass(Evaluation.class);
        verify(evaluationRepository).save(captor.capture());
        assertThat(captor.getValue().getReservation()).isEqualTo(reservation);
        assertThat(captor.getValue().getToken()).isNotBlank();
        assertThat(captor.getValue().getDateEnvoi()).isNotNull();

        verify(emailService).envoyerEmailEvaluation(
                eq("jean.dupont@test.com"),
                eq("Jean Dupont"),
                eq("Salle A"),
                contains(captor.getValue().getToken()));
    }

    @Test
    void soumettreEvaluation_shouldThrow_whenTokenNotFound() {
        doReturn(Optional.empty()).when(evaluationRepository).findByToken("token-invalide");

        assertThatThrownBy(() -> evaluationService.soumettreEvaluation(
                "token-invalide", 5, 5, 5, true, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Évaluation introuvable");
    }

    @Test
    void soumettreEvaluation_shouldThrow_whenAlreadySubmitted() {
        Evaluation evaluation = new Evaluation();
        evaluation.setRemplie(true);
        doReturn(Optional.of(evaluation)).when(evaluationRepository).findByToken("token-valide");

        assertThatThrownBy(() -> evaluationService.soumettreEvaluation(
                "token-valide", 5, 5, 5, true, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cette évaluation a déjà été soumise");
    }

    @Test
    void soumettreEvaluation_shouldSaveAllFields_whenValid() {
        Evaluation evaluation = new Evaluation();
        evaluation.setRemplie(false);
        doReturn(Optional.of(evaluation)).when(evaluationRepository).findByToken("token-valide");
        doReturn(evaluation).when(evaluationRepository).save(any(Evaluation.class));

        Evaluation result = evaluationService.soumettreEvaluation(
                "token-valide", 4, 5, 3, true,
                "Aucun problème", "Ajouter plus de prises électriques");

        assertThat(result.getNoteProprete()).isEqualTo(4);
        assertThat(result.getNoteEquipement()).isEqualTo(5);
        assertThat(result.getNoteFaciliteReservation()).isEqualTo(3);
        assertThat(result.getCapaciteAdaptee()).isTrue();
        assertThat(result.getProblemesRencontres()).isEqualTo("Aucun problème");
        assertThat(result.getSuggestionsAmelioration()).isEqualTo("Ajouter plus de prises électriques");
        assertThat(result.isRemplie()).isTrue();
        assertThat(result.getDateSoumission()).isNotNull();

        verify(evaluationRepository).save(evaluation);
    }
}