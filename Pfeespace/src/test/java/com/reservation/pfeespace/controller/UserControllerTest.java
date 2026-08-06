package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.dto.ProfilResponse;
import com.reservation.pfeespace.dto.UserDTO;
import com.reservation.pfeespace.repository.UserRepository;
import com.reservation.pfeespace.security.JwtUtil;
import com.reservation.pfeespace.service.IUserService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // désactive les filtres de sécurité (JWT) pour isoler le contrôleur
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUserService userService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserRepository userRepository;

    private static final String TOKEN_HEADER = "Bearer fake-jwt-token";
    private static final String EMAIL = "jean.dupont@test.com";

    @Test
    void getProfil_shouldReturnOk() throws Exception {
        doReturn(EMAIL).when(jwtUtil).extractEmail("fake-jwt-token");
        ProfilResponse profil = mock(ProfilResponse.class);
        doReturn(profil).when(userService).getProfil(EMAIL);

        mockMvc.perform(get("/api/users/profil")
                        .header("Authorization", TOKEN_HEADER))
                .andExpect(status().isOk());

        verify(userService).getProfil(EMAIL);
    }

    @Test
    void updateProfil_shouldReturnOk() throws Exception {
        doReturn(EMAIL).when(jwtUtil).extractEmail("fake-jwt-token");
        ProfilResponse profil = mock(ProfilResponse.class);
        doReturn(profil).when(userService).updateProfil(eq(EMAIL), any());

        String json = """
            {
              "nom": "Dupont",
              "prenom": "Jean",
              "telephone": "12345678"
            }
            """;

        mockMvc.perform(put("/api/users/profil")
                        .header("Authorization", TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(userService).updateProfil(eq(EMAIL), any());
    }

    @Test
    void changePassword_shouldReturnOk() throws Exception {
        doReturn(EMAIL).when(jwtUtil).extractEmail("fake-jwt-token");
        doNothing().when(userService).changePassword(eq(EMAIL), any());

        String json = """
            {
              "ancienMotDePasse": "ancienMdp123",
              "nouveauMotDePasse": "nouveauMdp456"
            }
            """;

        mockMvc.perform(put("/api/users/change-password")
                        .header("Authorization", TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(userService).changePassword(eq(EMAIL), any());
    }

    @Test
    void getAllUsers_shouldReturnOk() throws Exception {
        List<UserDTO> users = Collections.singletonList(mock(UserDTO.class));
        doReturn(users).when(userService).getAllUsers();

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());

        verify(userService).getAllUsers();
    }

    @Test
    void activer_shouldReturnOk() throws Exception {
        doNothing().when(userService).activerUser(1L);

        mockMvc.perform(put("/api/users/1/activer"))
                .andExpect(status().isOk());

        verify(userService).activerUser(1L);
    }

    @Test
    void desactiver_shouldReturnOk() throws Exception {
        doNothing().when(userService).desactiverUser(1L);

        mockMvc.perform(put("/api/users/1/desactiver"))
                .andExpect(status().isOk());

        verify(userService).desactiverUser(1L);
    }
}