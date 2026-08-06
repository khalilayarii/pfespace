package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.dto.*;
import com.reservation.pfeespace.dto.UserDTO;
import com.reservation.pfeespace.security.JwtUtil;
import com.reservation.pfeespace.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
@Tag(name = "Utilisateurs", description = "Gestion du profil utilisateur")
public class UserController {

    private final IUserService userService;
    private final JwtUtil jwtUtil;

    public UserController(IUserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    private String getEmailFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7);
        return jwtUtil.extractEmail(token);
    }

    @Operation(summary = "Voir mon profil")
    @GetMapping("/profil")
    public ResponseEntity<ProfilResponse> getProfil(HttpServletRequest request) {
        String email = getEmailFromRequest(request);
        return ResponseEntity.ok(userService.getProfil(email));
    }

    @Operation(summary = "Modifier mon profil")
    @PutMapping("/profil")
    public ResponseEntity<ProfilResponse> updateProfil(
            HttpServletRequest request,
            @RequestBody UpdateProfilRequest body) {
        String email = getEmailFromRequest(request);
        return ResponseEntity.ok(userService.updateProfil(email, body));
    }

    @Operation(summary = "Changer mon mot de passe")
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            HttpServletRequest request,
            @RequestBody ChangePasswordRequest body) {
        String email = getEmailFromRequest(request);
        userService.changePassword(email, body);
        return ResponseEntity.ok("Mot de passe modifié avec succès.");
    }

    // ✅ 3 nouveaux endpoints admin
    @Operation(summary = "Lister tous les utilisateurs")
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(summary = "Activer un utilisateur")
    @PutMapping("/{id}/activer")
    public ResponseEntity<Void> activer(@PathVariable Long id) {
        userService.activerUser(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Désactiver un utilisateur")
    @PutMapping("/{id}/desactiver")
    public ResponseEntity<Void> desactiver(@PathVariable Long id) {
        userService.desactiverUser(id);
        return ResponseEntity.ok().build();
    }
}