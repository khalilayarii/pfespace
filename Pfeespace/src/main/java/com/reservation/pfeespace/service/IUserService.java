package com.reservation.pfeespace.service;

import com.reservation.pfeespace.dto.*;

import java.util.List;
import java.util.Map;


public interface IUserService {
    Map<String, String> register(RegisterRequest request);
    Map<String, String> login(LoginRequest request);
    void demanderResetPassword(String email);
    void reinitialiserMotDePasse(String token, String nouveauMotDePasse);

    // profil
    ProfilResponse getProfil(String email);
    ProfilResponse updateProfil(String email, UpdateProfilRequest request);
    void changePassword(String email, ChangePasswordRequest request);
    // ✅ Ajouter ces 3 méthodes dans l'interface
    List<UserDTO> getAllUsers();
    void activerUser(Long id);
    void desactiverUser(Long id);
    // Ajoute cette ligne dans l'interface
    Object socialLogin(SocialLoginRequest request);
}