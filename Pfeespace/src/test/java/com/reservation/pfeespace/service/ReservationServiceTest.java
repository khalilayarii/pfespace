package com.reservation.pfeespace.service;

import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.entity.Salle;
import com.reservation.pfeespace.entity.User;
import com.reservation.pfeespace.repository.ReservationRepository;
import com.reservation.pfeespace.repository.SalleRepository;
import com.reservation.pfeespace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SalleRepository salleRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FactureService factureService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CheckinService checkinService;

    @InjectMocks
    private ReservationService reservationService;

    private Salle salle;
    private User user;
    private Reservation reservationBase;

    @BeforeEach
    void setUp() {
        salle = new Salle();
        salle.setId(1L);
        salle.setNom("Salle A");

        user = new User();
        user.setId(1L);
        user.setEmail("khalil@test.com");

        reservationBase = new Reservation();
        reservationBase.setId(10L);
        reservationBase.setDate(LocalDate.of(2026, 8, 10));
        reservationBase.setHeureDebut(LocalTime.of(9, 0));
        reservationBase.setHeureFin(LocalTime.of(10, 0));
        reservationBase.setMail("khalil@test.com");
        reservationBase.setNomComplet("Khalil Ayari");
    }

    // ---------- verifierDisponibilite ----------

    @Test
    void verifierDisponibilite_aucuneReservationExistante_retourneTrue() {
        when(reservationRepository.findBySalleIdAndDateAndStatutNotIn(
                eq(1L), eq(LocalDate.of(2026, 8, 10)), anyList()))
                .thenReturn(List.of());

        boolean result = reservationService.verifierDisponibilite(
                1L, "2026-08-10", "09:00", "10:00");

        assertTrue(result);
    }

    @Test
    void verifierDisponibilite_creneauChevauche_retourneFalse() {
        Reservation existante = new Reservation();
        existante.setHeureDebut(LocalTime.of(9, 30));
        existante.setHeureFin(LocalTime.of(10, 30));

        when(reservationRepository.findBySalleIdAndDateAndStatutNotIn(
                eq(1L), eq(LocalDate.of(2026, 8, 10)), anyList()))
                .thenReturn(List.of(existante));

        boolean result = reservationService.verifierDisponibilite(
                1L, "2026-08-10", "09:00", "10:00");

        assertFalse(result);
    }

    @Test
    void verifierDisponibilite_creneauNonChevauchant_retourneTrue() {
        Reservation existante = new Reservation();
        existante.setHeureDebut(LocalTime.of(10, 0));
        existante.setHeureFin(LocalTime.of(11, 0));

        when(reservationRepository.findBySalleIdAndDateAndStatutNotIn(
                eq(1L), eq(LocalDate.of(2026, 8, 10)), anyList()))
                .thenReturn(List.of(existante));

        boolean result = reservationService.verifierDisponibilite(
                1L, "2026-08-10", "09:00", "10:00");

        assertTrue(result);
    }

    // ---------- create ----------

    @Test
    void create_succes_retourneReservationEnAttente() {
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(reservationRepository.findBySalleIdAndDateAndStatutNotIn(
                eq(1L), any(), anyList())).thenReturn(List.of());
        when(userRepository.findByEmail("khalil@test.com")).thenReturn(Optional.of(user));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.create(1L, reservationBase, "khalil@test.com");

        assertEquals("EN_ATTENTE", result.getStatut());
        assertEquals(salle, result.getSalle());
        assertEquals(user, result.getUser());
        verify(emailService).envoyerEmailAttente(anyString(), anyString(), anyString(), anyString());
        verify(notificationService).creerNotification(eq(user), anyString(), anyString(), anyString(), anyString());
        verify(notificationService).notifierAdmins(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void create_salleIntrouvable_lanceException() {
        when(salleRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.create(99L, reservationBase, "khalil@test.com"));
        assertEquals("Salle non trouvée", exception.getMessage());
    }

    @Test
    void create_creneauIndisponible_lanceException() {
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));

        Reservation conflit = new Reservation();
        conflit.setHeureDebut(LocalTime.of(9, 30));
        conflit.setHeureFin(LocalTime.of(10, 30));
        when(reservationRepository.findBySalleIdAndDateAndStatutNotIn(
                eq(1L), any(), anyList())).thenReturn(List.of(conflit));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.create(1L, reservationBase, "khalil@test.com"));
        assertEquals("Créneau indisponible !", exception.getMessage());
    }

    @Test
    void create_userIntrouvable_lanceException() {
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(reservationRepository.findBySalleIdAndDateAndStatutNotIn(
                eq(1L), any(), anyList())).thenReturn(List.of());
        when(userRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.create(1L, reservationBase, "inconnu@test.com"));
        assertEquals("User non trouvé", exception.getMessage());
    }

    // ---------- confirmer ----------

    @Test
    void confirmer_succes_statutConfirmeeEtFactureGeneree() throws Exception {
        reservationBase.setSalle(salle);
        reservationBase.setUser(user);

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservationBase));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.confirmer(10L);

        assertEquals("CONFIRMEE", result.getStatut());
        verify(checkinService).genererQrToken(reservationBase);
        verify(factureService).genererFacture(result);
        verify(emailService).envoyerEmailConfirmation(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(notificationService).creerNotification(eq(user), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void confirmer_reservationIntrouvable_lanceException() {
        when(reservationRepository.findById(404L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.confirmer(404L));
        assertEquals("Réservation non trouvée", exception.getMessage());
    }

    // ---------- refuser ----------

    @Test
    void refuser_succes_statutRefusee() {
        reservationBase.setSalle(salle);
        reservationBase.setUser(user);

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservationBase));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.refuser(10L);

        assertEquals("REFUSEE", result.getStatut());
        verify(emailService).envoyerEmailRefus(anyString(), anyString(), anyString(), anyString());
        verify(notificationService).creerNotification(eq(user), anyString(), anyString(), anyString(), anyString());
    }

    // ---------- mettreEnAttente ----------

    @Test
    void mettreEnAttente_succes_statutEnAttente() {
        reservationBase.setSalle(salle);
        reservationBase.setUser(user);

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservationBase));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.mettreEnAttente(10L);

        assertEquals("EN_ATTENTE", result.getStatut());
        verify(emailService).envoyerEmailAttente(anyString(), anyString(), anyString(), anyString());
    }

    // ---------- delete ----------

    @Test
    void delete_appelleRepositoryDeleteById() {
        reservationService.delete(10L);

        verify(reservationRepository).deleteById(10L);
    }

    // ---------- getMesReservations ----------

    @Test
    void getMesReservations_retourneListeDuUser() {
        when(reservationRepository.findByUserEmail("khalil@test.com"))
                .thenReturn(List.of(reservationBase));

        List<Reservation> result = reservationService.getMesReservations("khalil@test.com");

        assertEquals(1, result.size());
        assertEquals(reservationBase, result.get(0));
    }

    // ---------- createByAdmin ----------

    @Test
    void createByAdmin_succes_statutConfirmeeDirectement() throws Exception {
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));
        when(reservationRepository.findBySalleIdAndDateAndStatutNotIn(
                eq(1L), any(), anyList())).thenReturn(List.of());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.createByAdmin(1L, reservationBase);

        assertEquals("CONFIRMEE", result.getStatut());
        assertEquals(salle, result.getSalle());
        verify(factureService).genererFacture(result);
    }

    @Test
    void createByAdmin_salleIntrouvable_lanceException() {
        when(salleRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.createByAdmin(99L, reservationBase));
        assertEquals("Salle non trouvée", exception.getMessage());
    }

    @Test
    void createByAdmin_creneauIndisponible_lanceException() {
        when(salleRepository.findById(1L)).thenReturn(Optional.of(salle));

        Reservation conflit = new Reservation();
        conflit.setHeureDebut(LocalTime.of(9, 30));
        conflit.setHeureFin(LocalTime.of(10, 30));
        when(reservationRepository.findBySalleIdAndDateAndStatutNotIn(
                eq(1L), any(), anyList())).thenReturn(List.of(conflit));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.createByAdmin(1L, reservationBase));
        assertEquals("Créneau indisponible !", exception.getMessage());
    }
}