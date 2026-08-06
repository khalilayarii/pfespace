package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.dto.SalleAvisDTO;
import com.reservation.pfeespace.entity.Salle;
import com.reservation.pfeespace.repository.UserRepository;
import com.reservation.pfeespace.security.JwtUtil;
import com.reservation.pfeespace.service.SalleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalleController.class)
@AutoConfigureMockMvc(addFilters = false) // désactive les filtres de sécurité (JWT) pour isoler le contrôleur
class SalleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SalleService salleService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void getAll_shouldReturnOk() throws Exception {
        List<Salle> salles = Collections.singletonList(new Salle());
        doReturn(salles).when(salleService).getAll();

        mockMvc.perform(get("/api/salles"))
                .andExpect(status().isOk());

        verify(salleService).getAll();
    }

    @Test
    void getById_shouldReturnOk() throws Exception {
        Salle salle = new Salle();
        salle.setId(1L);
        salle.setNom("Salle A");
        doReturn(salle).when(salleService).getById(1L);

        mockMvc.perform(get("/api/salles/1"))
                .andExpect(status().isOk());

        verify(salleService).getById(1L);
    }

    @Test
    void create_shouldReturnOk() throws Exception {
        Salle resultat = new Salle();
        resultat.setId(1L);
        resultat.setNom("Salle A");
        doReturn(resultat).when(salleService).create(any(Salle.class));

        String json = """
            {
              "nom": "Salle A",
              "description": "Grande salle de réunion",
              "capacite": 10,
              "equipement": "Projecteur, Wifi",
              "prix": 50.0,
              "disponible": true
            }
            """;

        mockMvc.perform(post("/api/salles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(salleService).create(any(Salle.class));
    }

    @Test
    void update_shouldReturnOk() throws Exception {
        Salle resultat = new Salle();
        resultat.setId(1L);
        resultat.setNom("Salle A modifiée");
        doReturn(resultat).when(salleService).update(eq(1L), any(Salle.class));

        String json = """
            {
              "nom": "Salle A modifiée",
              "description": "Grande salle de réunion rénovée",
              "capacite": 12,
              "equipement": "Projecteur, Wifi, Tableau",
              "prix": 60.0,
              "disponible": true
            }
            """;

        mockMvc.perform(put("/api/salles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(salleService).update(eq(1L), any(Salle.class));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        doNothing().when(salleService).delete(1L);

        mockMvc.perform(delete("/api/salles/1"))
                .andExpect(status().isNoContent());

        verify(salleService).delete(1L);
    }

    @Test
    void delete_avecReservationsLiees_shouldReturnConflict() throws Exception {
        doThrow(new RuntimeException("Impossible de supprimer la salle \"Salle A\" : 2 réservation(s) y sont encore liée(s)."))
                .when(salleService).delete(1L);

        mockMvc.perform(delete("/api/salles/1"))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Impossible de supprimer")));

        verify(salleService).delete(1L);
    }

    @Test
    void getAvis_shouldReturnOk() throws Exception {
        SalleAvisDTO dto = mock(SalleAvisDTO.class);
        doReturn(dto).when(salleService).getAvisEtScore(1L);

        mockMvc.perform(get("/api/salles/1/avis"))
                .andExpect(status().isOk());

        verify(salleService).getAvisEtScore(1L);
    }
}