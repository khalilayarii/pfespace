package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.service.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.reservation.pfeespace.security.JwtUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.reservation.pfeespace.repository.UserRepository;
import java.util.Map;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // désactive les filtres de sécurité (JWT) pour isoler le contrôleur
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUserService userService;
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void register_shouldReturnOk() throws Exception {
        doReturn(Map.of("message", "resultat-register")).when(userService).register(any());

        String json = """
        {
          "nom": "Dupont",
          "prenom": "Jean",
          "email": "jean.dupont@test.com",
          "password": "motdepasse123"
        }
        """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(userService).register(any());
    }

    @Test
    void login_shouldReturnOk() throws Exception {
        doReturn(Map.of("token", "resultat-login")).when(userService).login(any());

        String json = """
        {
          "email": "jean.dupont@test.com",
          "password": "motdepasse123"
        }
        """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(userService).login(any());
    }

    @Test
    void socialLogin_shouldReturnOk() throws Exception {
        doReturn("resultat-social").when(userService).socialLogin(any());

        String json = """
            {
              "email": "jean.dupont@test.com",
              "provider": "google",
              "token": "fake-token"
            }
            """;

        mockMvc.perform(post("/api/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(userService).socialLogin(any());
    }

    @Test
    void forgotPassword_shouldReturnOk() throws Exception {
        String json = """
            {
              "email": "jean.dupont@test.com"
            }
            """;

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(userService).demanderResetPassword("jean.dupont@test.com");
    }

    @Test
    void resetPassword_shouldReturnOk() throws Exception {
        String json = """
            {
              "token": "reset-token-123",
              "nouveauMotDePasse": "nouveauMdp456"
            }
            """;

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(userService).reinitialiserMotDePasse("reset-token-123", "nouveauMdp456");
    }
}