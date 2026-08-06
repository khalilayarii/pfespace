package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.dto.ChatRequest;
import com.reservation.pfeespace.entity.Role;
import com.reservation.pfeespace.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "http://localhost:4200")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> ask(
            @RequestBody ChatRequest request,
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());

        if (!isAuthenticated) {
            // Visiteur non connecté : identifiant anonyme envoyé par le front (X-Visitor-Id)
            // pour isoler sa conversation. role = null.
            String reply = chatbotService.ask(request.getMessage(), visitorId, null);
            return ResponseEntity.ok(Map.of("response", reply));
        }

        // Le rôle et l'email viennent EXCLUSIVEMENT du JWT (SecurityContext),
        // jamais du body envoyé par le frontend — impossible à falsifier.
        String email = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Role role = isAdmin ? Role.ADMIN : Role.CLIENT;

        String reply = chatbotService.ask(request.getMessage(), email, role);
        return ResponseEntity.ok(Map.of("response", reply));
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(
            @RequestHeader(value = "X-Visitor-Id", required = false) String visitorId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());

        String identifiant = isAuthenticated ? auth.getName() : visitorId;
        chatbotService.clearHistory(identifiant);
        return ResponseEntity.ok().build();
    }
}