package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.repository.UserRepository;
import com.reservation.pfeespace.security.JwtUtil;
import com.reservation.pfeespace.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false) // désactive les filtres de sécurité (JWT) pour isoler le contrôleur
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getAll_sansStatut_shouldReturnAll() throws Exception {
        List<Reservation> reservations = Collections.singletonList(new Reservation());
        doReturn(reservations).when(reservationService).getAll();

        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk());

        verify(reservationService).getAll();
    }

    @Test
    void getAll_avecStatut_shouldReturnFiltered() throws Exception {
        List<Reservation> reservations = Collections.singletonList(new Reservation());
        doReturn(reservations).when(reservationService).getByStatut("CONFIRMEE");

        mockMvc.perform(get("/api/reservations").param("statut", "CONFIRMEE"))
                .andExpect(status().isOk());

        verify(reservationService).getByStatut("CONFIRMEE");
    }

    @Test
    void create_shouldReturnOk() throws Exception {
        doReturn("jean.dupont@test.com").when(jwtUtil).extractEmail("fake-jwt-token");

        Reservation resultat = new Reservation();
        resultat.setId(1L);
        resultat.setStatut("EN_ATTENTE");
        doReturn(resultat).when(reservationService)
                .create(eq(1L), any(Reservation.class), eq("jean.dupont@test.com"));

        String json = """
            {
              "date": "2026-08-10",
              "heureDebut": "09:00:00",
              "heureFin": "10:00:00",
              "mail": "jean.dupont@test.com",
              "nomComplet": "Jean Dupont",
              "societe": "ACME",
              "telephone": "12345678",
              "description": "Réunion équipe",
              "typeUtilisateur": "MANAGER",
              "natureManifestation": "REUNION"
            }
            """;

        mockMvc.perform(post("/api/reservations/1")
                        .header("Authorization", "Bearer fake-jwt-token")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(reservationService).create(eq(1L), any(Reservation.class), eq("jean.dupont@test.com"));
    }

    @Test
    void createByAdmin_shouldReturnOk() throws Exception {
        Reservation resultat = new Reservation();
        resultat.setId(1L);
        resultat.setStatut("CONFIRMEE");
        doReturn(resultat).when(reservationService)
                .createByAdmin(eq(1L), any(Reservation.class));

        String json = """
            {
              "date": "2026-08-10",
              "heureDebut": "09:00:00",
              "heureFin": "10:00:00",
              "mail": "jean.dupont@test.com",
              "nomComplet": "Jean Dupont",
              "societe": "ACME",
              "telephone": "12345678",
              "description": "Réunion équipe",
              "typeUtilisateur": "MANAGER",
              "natureManifestation": "REUNION"
            }
            """;

        mockMvc.perform(post("/api/reservations/admin/1")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(reservationService).createByAdmin(eq(1L), any(Reservation.class));
    }

    @Test
    void confirmer_shouldReturnOk() throws Exception {
        doReturn(new Reservation()).when(reservationService).confirmer(1L);

        mockMvc.perform(put("/api/reservations/1/confirmer"))
                .andExpect(status().isOk());

        verify(reservationService).confirmer(1L);
    }

    @Test
    void refuser_shouldReturnOk() throws Exception {
        doReturn(new Reservation()).when(reservationService).refuser(1L);

        mockMvc.perform(put("/api/reservations/1/refuser"))
                .andExpect(status().isOk());

        verify(reservationService).refuser(1L);
    }

    @Test
    void attente_shouldReturnOk() throws Exception {
        doReturn(new Reservation()).when(reservationService).mettreEnAttente(1L);

        mockMvc.perform(put("/api/reservations/1/attente"))
                .andExpect(status().isOk());

        verify(reservationService).mettreEnAttente(1L);
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        doNothing().when(reservationService).delete(1L);

        mockMvc.perform(delete("/api/reservations/1"))
                .andExpect(status().isNoContent());

        verify(reservationService).delete(1L);
    }

    @Test
    void getMesReservations_shouldReturnOk() throws Exception {
        doReturn("jean.dupont@test.com").when(jwtUtil).extractEmail("fake-jwt-token");
        List<Reservation> reservations = Collections.singletonList(new Reservation());
        doReturn(reservations).when(reservationService).getMesReservations("jean.dupont@test.com");

        mockMvc.perform(get("/api/reservations/mes-reservations")
                        .header("Authorization", "Bearer fake-jwt-token"))
                .andExpect(status().isOk());

        verify(reservationService).getMesReservations("jean.dupont@test.com");
    }

    @Test
    void verifierDisponibilite_shouldReturnOk() throws Exception {
        doReturn(true).when(reservationService)
                .verifierDisponibilite(1L, "2026-08-10", "09:00", "10:00");

        mockMvc.perform(get("/api/reservations/disponibilite")
                        .param("salleId", "1")
                        .param("date", "2026-08-10")
                        .param("heureDebut", "09:00")
                        .param("heureFin", "10:00"))
                .andExpect(status().isOk());

        verify(reservationService).verifierDisponibilite(1L, "2026-08-10", "09:00", "10:00");
    }
}