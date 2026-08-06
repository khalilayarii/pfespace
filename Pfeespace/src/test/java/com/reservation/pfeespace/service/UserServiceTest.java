package com.reservation.pfeespace.service;

import com.reservation.pfeespace.dto.LoginRequest;
import com.reservation.pfeespace.entity.Role;
import com.reservation.pfeespace.entity.User;
import com.reservation.pfeespace.repository.PasswordResetTokenRepository;
import com.reservation.pfeespace.repository.ReservationRepository;
import com.reservation.pfeespace.repository.UserRepository;
import com.reservation.pfeespace.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private UserService userService;

    private User userActif;
    private User userInactif;

    @BeforeEach
    void setUp() {
        userActif = User.builder()
                .nom("Ayari")
                .prenom("Khalil")
                .email("khalil@test.com")
                .mdp("motDePasseHashe")
                .role(Role.CLIENT)
                .actif(true)
                .build();
        userActif.setId(1L);

        userInactif = User.builder()
                .nom("Nadia")
                .prenom("Ayari")
                .email("nadia@test.com")
                .mdp("motDePasseHashe")
                .role(Role.CLIENT)
                .actif(false)
                .build();
        userInactif.setId(2L);
    }

    @Test
    void login_succes_retourneToken() {
        // Arrange : on simule un utilisateur trouvé, mot de passe correct, compte actif
        LoginRequest request = new LoginRequest();
        request.setEmail("khalil@test.com");
        request.setMdp("motDePasseClair");

        when(userRepository.findByEmail("khalil@test.com")).thenReturn(Optional.of(userActif));
        when(passwordEncoder.matches("motDePasseClair", "motDePasseHashe")).thenReturn(true);
        when(jwtUtil.generateToken("khalil@test.com")).thenReturn("fake-jwt-token");

        // Act
        Map<String, String> result = userService.login(request);

        // Assert
        assertEquals("fake-jwt-token", result.get("token"));
        assertEquals("CLIENT", result.get("role"));
        assertEquals("Ayari", result.get("nom"));
    }

    @Test
    void login_emailIntrouvable_lanceException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("inconnu@test.com");
        request.setMdp("peuImporte");

        when(userRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.login(request));
        assertEquals("Email introuvable", exception.getMessage());
    }

    @Test
    void login_mauvaisMotDePasse_lanceException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("khalil@test.com");
        request.setMdp("mauvaisMotDePasse");

        when(userRepository.findByEmail("khalil@test.com")).thenReturn(Optional.of(userActif));
        when(passwordEncoder.matches("mauvaisMotDePasse", "motDePasseHashe")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.login(request));
        assertEquals("Mot de passe incorrect", exception.getMessage());
    }

    @Test
    void login_compteInactif_lanceException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nadia@test.com");
        request.setMdp("motDePasseClair");

        when(userRepository.findByEmail("nadia@test.com")).thenReturn(Optional.of(userInactif));
        when(passwordEncoder.matches("motDePasseClair", "motDePasseHashe")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.login(request));
        assertEquals("Votre compte est en attente de validation par l'administration", exception.getMessage());
    }
}