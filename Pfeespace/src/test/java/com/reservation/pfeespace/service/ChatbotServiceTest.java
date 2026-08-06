package com.reservation.pfeespace.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private RagService ragService;

    @Mock
    private AdminStatsService adminStatsService;

    @InjectMocks
    private ChatbotService chatbotService;

    // ==========================================
    //  isAdminOnlyQuestion
    // ==========================================

    @Test
    void isAdminOnlyQuestion_detecteQuestionUtilisateurs() {
        boolean result = ReflectionTestUtils.invokeMethod(chatbotService, "isAdminOnlyQuestion",
                "combien d'utilisateurs sont inscrits ?");
        assertThat(result).isTrue();
    }

    @Test
    void isAdminOnlyQuestion_detecteStatsReservations() {
        boolean result = ReflectionTestUtils.invokeMethod(chatbotService, "isAdminOnlyQuestion",
                "combien de reservations confirmees ce mois ?");
        assertThat(result).isTrue();
    }

    @Test
    void isAdminOnlyQuestion_ignoreQuestionCapaciteSalle() {
        boolean result = ReflectionTestUtils.invokeMethod(chatbotService, "isAdminOnlyQuestion",
                "je cherche une salle pour 10 personnes");
        assertThat(result).isFalse();
    }

    @Test
    void isAdminOnlyQuestion_ignoreQuestionGenerale() {
        boolean result = ReflectionTestUtils.invokeMethod(chatbotService, "isAdminOnlyQuestion",
                "quels sont vos horaires d'ouverture ?");
        assertThat(result).isFalse();
    }

    // ==========================================
    //  seemsLikeStatQuestion
    // ==========================================

    @Test
    void seemsLikeStatQuestion_detecteMotTaux() {
        boolean result = ReflectionTestUtils.invokeMethod(chatbotService, "seemsLikeStatQuestion",
                "quel est le taux de refus ?");
        assertThat(result).isTrue();
    }

    @Test
    void seemsLikeStatQuestion_detecteMotCombien() {
        boolean result = ReflectionTestUtils.invokeMethod(chatbotService, "seemsLikeStatQuestion",
                "combien de reservations aujourd'hui ?");
        assertThat(result).isTrue();
    }

    @Test
    void seemsLikeStatQuestion_retourneFalseSiAucunMotCle() {
        boolean result = ReflectionTestUtils.invokeMethod(chatbotService, "seemsLikeStatQuestion",
                "bonjour, comment allez-vous ?");
        assertThat(result).isFalse();
    }

    // ==========================================
    //  clearHistory
    // ==========================================

    @Test
    @SuppressWarnings("unchecked")
    void clearHistory_supprimeToutesLesSessionsDeLUtilisateur() {
        Map<String, List<Map<String, Object>>> history =
                (Map<String, List<Map<String, Object>>>)
                        ReflectionTestUtils.getField(chatbotService, "conversationHistory");

        history.put("client:test@test.com", new ArrayList<>());
        history.put("admin:test@test.com", new ArrayList<>());
        history.put("visiteur:test@test.com", new ArrayList<>());

        chatbotService.clearHistory("test@test.com");

        assertThat(history).doesNotContainKeys(
                "client:test@test.com", "admin:test@test.com", "visiteur:test@test.com");
    }
}