package com.reservation.pfeespace.service;

import com.reservation.pfeespace.dto.*;
import com.reservation.pfeespace.dto.UserDTO;
import com.reservation.pfeespace.entity.PasswordResetToken;
import com.reservation.pfeespace.entity.Role;
import com.reservation.pfeespace.entity.User;
import com.reservation.pfeespace.repository.PasswordResetTokenRepository;
import com.reservation.pfeespace.repository.ReservationRepository;
import com.reservation.pfeespace.repository.UserRepository;
import com.reservation.pfeespace.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final ReservationRepository reservationRepository;

    @Value("${app.reset-password.url}")
    private String resetUrl;

    @Value("${app.reset-password.expiration-minutes}")
    private int expirationMinutes;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       PasswordResetTokenRepository tokenRepository,
                       EmailService emailService,
                       ReservationRepository reservationRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.reservationRepository = reservationRepository;
    }

    @Override
    public Map<String, String> register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }
        User user = User.builder()
                .nom(req.getNom())
                .prenom(req.getPrenom())
                .email(req.getEmail())
                .mdp(passwordEncoder.encode(req.getMdp()))
                .telephone(req.getTelephone())
                .role(Role.CLIENT)
                .actif(false) // ✅ Compte désactivé par défaut, en attente de validation admin
                .build();
        userRepository.save(user);

        // ✅ Envoi du mail "compte en attente de validation"
        try {
            String nomComplet = user.getNom() + " " + (user.getPrenom() != null ? user.getPrenom() : "");
            emailService.envoyerEmailEnAttenteValidation(user.getEmail(), nomComplet.trim());
        } catch (Exception e) {
            // On ne bloque pas l'inscription si l'email échoue, on log seulement
            System.err.println("Erreur envoi email validation en attente : " + e.getMessage());
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return Map.of("token", token, "message", "Inscription réussie, compte en attente de validation par l'administration");
    }

    @Override
    public Map<String, String> login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Email introuvable"));
        if (!passwordEncoder.matches(req.getMdp(), user.getMdp())) {
            throw new RuntimeException("Mot de passe incorrect");
        }
        if (!user.getActif()) {
            throw new RuntimeException("Votre compte est en attente de validation par l'administration");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return Map.of("token", token, "role", user.getRole().name(), "nom", user.getNom());
    }

    @Override
    @Transactional
    public void demanderResetPassword(String email) {
        if (userRepository.findByEmail(email).isEmpty()) return;
        User user = userRepository.findByEmail(email).get();
        tokenRepository.deleteByUser_Id(user.getId());
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiration(LocalDateTime.now().plusMinutes(expirationMinutes));
        resetToken.setUtilise(false);
        tokenRepository.save(resetToken);
        emailService.envoyerEmailReset(email, token, resetUrl);
    }

    @Override
    @Transactional
    public void reinitialiserMotDePasse(String token, String nouveauMotDePasse) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token invalide"));
        if (resetToken.isExpire()) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("Token expiré, veuillez refaire la demande");
        }
        if (resetToken.isUtilise()) {
            throw new RuntimeException("Ce lien a déjà été utilisé");
        }
        User user = resetToken.getUser();
        user.setMdp(passwordEncoder.encode(nouveauMotDePasse));
        userRepository.save(user);
        resetToken.setUtilise(true);
        tokenRepository.save(resetToken);
    }

    @Override
    public ProfilResponse getProfil(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        ProfilResponse response = new ProfilResponse();
        response.setId(user.getId());
        response.setNom(user.getNom());
        response.setPrenom(user.getPrenom());
        response.setEmail(user.getEmail());
        response.setTelephone(user.getTelephone());
        response.setRole(user.getRole().name());
        response.setActif(user.getActif());
        return response;
    }

    @Override
    public ProfilResponse updateProfil(String email, UpdateProfilRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        if (request.getNom() != null) user.setNom(request.getNom());
        if (request.getPrenom() != null) user.setPrenom(request.getPrenom());
        if (request.getTelephone() != null) user.setTelephone(request.getTelephone());
        userRepository.save(user);
        return getProfil(email);
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        if (!passwordEncoder.matches(request.getAncienMotDePasse(), user.getMdp())) {
            throw new RuntimeException("Ancien mot de passe incorrect");
        }
        user.setMdp(passwordEncoder.encode(request.getNouveauMotDePasse()));
        userRepository.save(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(user -> {
            UserDTO dto = new UserDTO();
            dto.setId(user.getId());
            dto.setNom(user.getNom());
            dto.setPrenom(user.getPrenom());
            dto.setEmail(user.getEmail());
            dto.setTelephone(user.getTelephone());
            dto.setRole(user.getRole().name());
            dto.setActif(user.getActif());
            dto.setNombreReservations(
                    reservationRepository.countByUserId(user.getId())
            );
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public void activerUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User non trouvé"));
        user.setActif(true);
        userRepository.save(user);

        // ✅ Envoi du mail "compte validé"
        try {
            String nomComplet = user.getNom() + " " + (user.getPrenom() != null ? user.getPrenom() : "");
            emailService.envoyerEmailCompteValide(user.getEmail(), nomComplet.trim());
        } catch (Exception e) {
            System.err.println("Erreur envoi email compte validé : " + e.getMessage());
        }
    }

    @Override
    public void desactiverUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User non trouvé"));
        user.setActif(false);
        userRepository.save(user);
    }

    // ✅ Social Login Google / Facebook
    @Override
    public Object socialLogin(SocialLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            // Compte inexistant → on le crée automatiquement
            // Note : réseau social = email déjà vérifié par Google/Facebook,
            // donc on garde actif=true pour ne pas bloquer ce flux
            user = User.builder()
                    .nom(request.getNom())
                    .prenom("")
                    .email(request.getEmail())
                    .mdp(passwordEncoder.encode(UUID.randomUUID().toString())) // mdp aléatoire
                    .telephone("")
                    .role(Role.CLIENT)
                    .actif(true)
                    .build();
            userRepository.save(user);
        }

        if (!user.getActif()) {
            throw new RuntimeException("Votre compte est en attente de validation par l'administration");
        }

        String token = jwtUtil.generateToken(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole().name());
        response.put("nom", user.getNom());
        return response;
    }
}