package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.dto.ForgotPasswordRequest;
import com.reservation.pfeespace.dto.LoginRequest;
import com.reservation.pfeespace.dto.RegisterRequest;
import com.reservation.pfeespace.dto.ResetPasswordRequest;
import com.reservation.pfeespace.dto.SocialLoginRequest;
import com.reservation.pfeespace.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Authentification", description = "Login, Register, Reset Password")
public class AuthController {

    private final IUserService userService;

    public AuthController(IUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Inscription d'un nouvel utilisateur")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @Operation(summary = "Connexion et récupération du token JWT")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    // ✅ NOUVEAU
    @Operation(summary = "Connexion via Google ou Facebook")
    @PostMapping("/social-login")
    public ResponseEntity<?> socialLogin(@RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(userService.socialLogin(request));
    }

    @Operation(summary = "Demander un lien de réinitialisation par email")
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        userService.demanderResetPassword(request.getEmail());
        return ResponseEntity.ok(
                "Si votre email est valide, un lien de réinitialisation vous a été envoyé."
        );
    }

    @Operation(summary = "Réinitialiser le mot de passe avec le token reçu par email")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.reinitialiserMotDePasse(request.getToken(), request.getNouveauMotDePasse());
        return ResponseEntity.ok("Mot de passe réinitialisé avec succès.");
    }
}