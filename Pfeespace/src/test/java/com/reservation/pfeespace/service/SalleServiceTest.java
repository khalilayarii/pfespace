package com.reservation.pfeespace.service;

import com.reservation.pfeespace.dto.SalleAvisDTO;
import com.reservation.pfeespace.entity.Salle;
import com.reservation.pfeespace.repository.EvaluationRepository;
import com.reservation.pfeespace.repository.ReservationRepository;
import com.reservation.pfeespace.repository.SalleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalleServiceTest {

    @Mock
    private SalleRepository salleRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private EvaluationRepository evaluationRepository;

    @InjectMocks
    private SalleService salleService;

    private Salle salle;

    @BeforeEach
    void setUp() {
        salle = new Salle();
        salle.setId(1L);
        salle.setNom("Salle A");
        salle.setCapacite(10);
        salle.setPrix(50.0);
        salle.setDisponible(true);
    }

    @Test
    void getAll_shouldReturnAllSalles() {
        doReturn(List.of(salle)).when(salleRepository).findAll();

        List<Salle> result = salleService.getAll();

        assertThat(result).hasSize(1);
        verify(salleRepository).findAll();
    }

    @Test
    void getById_shouldReturnSalle_whenExists() {
        doReturn(Optional.of(salle)).when(salleRepository).findById(1L);

        Salle result = salleService.getById(1L);

        assertThat(result.getNom()).isEqualTo("Salle A");
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        doReturn(Optional.empty()).when(salleRepository).findById(99L);

        assertThatThrownBy(() -> salleService.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Salle non trouvée");
    }

    @Test
    void create_shouldSaveAndNotifyClients() {
        doReturn(salle).when(salleRepository).save(any(Salle.class));

        Salle result = salleService.create(salle);

        assertThat(result.getNom()).isEqualTo("Salle A");
        verify(salleRepository).save(salle);
        verify(notificationService).notifierTousLesClients(
                anyString(), anyString(), eq("NOUVELLE_SALLE"), eq("/salles"));
    }

    @Test
    void update_shouldModifyAndSaveExistingSalle() {
        Salle nouvellesDonnees = new Salle();
        nouvellesDonnees.setNom("Salle A rénovée");
        nouvellesDonnees.setDescription("Nouvelle description");
        nouvellesDonnees.setCapacite(15);
        nouvellesDonnees.setEquipement("Wifi, Projecteur");
        nouvellesDonnees.setPrix(70.0);
        nouvellesDonnees.setDisponible(false);
        nouvellesDonnees.setImage("image.png");

        doReturn(Optional.of(salle)).when(salleRepository).findById(1L);
        doReturn(salle).when(salleRepository).save(any(Salle.class));

        Salle result = salleService.update(1L, nouvellesDonnees);

        assertThat(result.getNom()).isEqualTo("Salle A rénovée");
        assertThat(result.getCapacite()).isEqualTo(15);
        assertThat(result.isDisponible()).isFalse();
        verify(salleRepository).save(salle);
    }

    @Test
    void delete_shouldSucceed_whenNoReservationsLinked() {
        doReturn(Optional.of(salle)).when(salleRepository).findById(1L);
        doReturn(0L).when(reservationRepository).countBySalleId(1L);

        salleService.delete(1L);

        verify(salleRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrow_whenReservationsLinked() {
        doReturn(Optional.of(salle)).when(salleRepository).findById(1L);
        doReturn(2L).when(reservationRepository).countBySalleId(1L);

        assertThatThrownBy(() -> salleService.delete(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Impossible de supprimer la salle");

        verify(salleRepository, never()).deleteById(anyLong());
    }

    @Test
    void getAvisEtScore_shouldReturnResult_whenNoEvaluations() {
        doReturn(Collections.emptyList()).when(evaluationRepository)
                .findByReservation_Salle_IdAndRemplieTrue(1L);

        SalleAvisDTO result = salleService.getAvisEtScore(1L);

        assertThat(result).isNotNull();
        verify(evaluationRepository).findByReservation_Salle_IdAndRemplieTrue(1L);
    }
}