package com.reservation.pfeespace.service;

import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.entity.Salle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private com.reservation.pfeespace.repository.SalleRepository salleRepository;

    @Mock
    private com.reservation.pfeespace.repository.ReservationRepository reservationRepository;

    @Mock
    private AdminStatsService adminStatsService;

    @InjectMocks
    private RagService ragService;

    // ==========================================
    //  extractCapacite
    // ==========================================

    @Test
    void extractCapacite_detecteNombrePersonnes() {
        int result = ReflectionTestUtils.invokeMethod(ragService, "extractCapacite",
                "je cherche une salle pour 8 personnes");
        assertEquals(8, result);
    }

    @Test
    void extractCapacite_detecteAbreviationPers() {
        int result = ReflectionTestUtils.invokeMethod(ragService, "extractCapacite",
                "besoin d'une salle pour 12 pers");
        assertEquals(12, result);
    }

    @Test
    void extractCapacite_retourneZeroSiAbsent() {
        int result = ReflectionTestUtils.invokeMethod(ragService, "extractCapacite",
                "quelle salle recommandez-vous ?");
        assertEquals(0, result);
    }

    // ==========================================
    //  extractDate
    // ==========================================

    @Test
    void extractDate_detecteAujourdhui() {
        LocalDate result = ReflectionTestUtils.invokeMethod(ragService, "extractDate", "salle libre aujourd'hui");
        assertEquals(LocalDate.now(), result);
    }

    @Test
    void extractDate_detecteDemain() {
        LocalDate result = ReflectionTestUtils.invokeMethod(ragService, "extractDate", "je veux reserver demain");
        assertEquals(LocalDate.now().plusDays(1), result);
    }

    @Test
    void extractDate_detecteFormatJourMoisAnnee() {
        LocalDate result = ReflectionTestUtils.invokeMethod(ragService, "extractDate",
                "reservation pour le 25/12/2026");
        assertEquals(LocalDate.of(2026, 12, 25), result);
    }

    @Test
    void extractDate_retourneNullSiAbsente() {
        LocalDate result = ReflectionTestUtils.invokeMethod(ragService, "extractDate",
                "combien coute une salle ?");
        assertNull(result);
    }

    // ==========================================
    //  extractHeureDebut / extractHeureFin
    // ==========================================

    @Test
    void extractHeureDebut_detecteHeureAvecH() {
        LocalTime result = ReflectionTestUtils.invokeMethod(ragService, "extractHeureDebut",
                "reunion de 14h a 16h");
        assertEquals(LocalTime.of(14, 0), result);
    }

    @Test
    void extractHeureFin_detecteHeureAvecJusqua() {
        LocalTime result = ReflectionTestUtils.invokeMethod(ragService, "extractHeureFin",
                "reunion de 14h jusqu'a 16h30");
        assertEquals(LocalTime.of(16, 30), result);
    }

    // ==========================================
    //  detecterTypeEvent
    // ==========================================

    @Test
    void detecterTypeEvent_detecteReunion() {
        String result = ReflectionTestUtils.invokeMethod(ragService, "detecterTypeEvent",
                "je dois organiser une reunion importante");
        assertEquals("REUNION", result);
    }

    @Test
    void detecterTypeEvent_detecteConference() {
        String result = ReflectionTestUtils.invokeMethod(ragService, "detecterTypeEvent",
                "nous preparons une conference");
        assertEquals("CONFERENCE", result);
    }

    @Test
    void detecterTypeEvent_detecteCoworking() {
        String result = ReflectionTestUtils.invokeMethod(ragService, "detecterTypeEvent",
                "je cherche un espace de coworking");
        assertEquals("COWORKING", result);
    }

    @Test
    void detecterTypeEvent_retourneVideSiAucunMatch() {
        String result = ReflectionTestUtils.invokeMethod(ragService, "detecterTypeEvent",
                "bonjour comment allez-vous");
        assertEquals("", result);
    }

    // ==========================================
    //  scorerSalle
    // ==========================================

    @Test
    void scorerSalle_capaciteTropPetite_scoreNegatif() {
        Salle salle = new Salle();
        salle.setCapacite(5);
        salle.setPrix(40.0);

        Object result = ReflectionTestUtils.invokeMethod(ragService, "scorerSalle",
                salle, 20, null, null, null, "", "",
                Collections.emptyList(), Collections.emptyList());

        int score = (int) ReflectionTestUtils.getField(result, "score");
        assertThat(score).isEqualTo(-1);
    }

    @Test
    void scorerSalle_capaciteParfaite_bonScore() {
        Salle salle = new Salle();
        salle.setCapacite(10);
        salle.setPrix(25.0);
        salle.setEquipement("wifi, projecteur");

        Object result = ReflectionTestUtils.invokeMethod(ragService, "scorerSalle",
                salle, 8, null, null, null, "", "",
                Collections.emptyList(), Collections.emptyList());

        int score = (int) ReflectionTestUtils.getField(result, "score");
        assertThat(score).isPositive();
    }

    @Test
    void scorerSalle_conflitCreneau_scoreNegatif() {
        Salle salle = new Salle();
        salle.setId(1L);
        salle.setCapacite(10);
        salle.setPrix(30.0);

        Reservation conflit = new Reservation();
        conflit.setHeureDebut(LocalTime.of(9, 0));
        conflit.setHeureFin(LocalTime.of(11, 0));

        Object result = ReflectionTestUtils.invokeMethod(ragService, "scorerSalle",
                salle, 5, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(12, 0),
                "", "", Collections.emptyList(), List.of(conflit));

        int score = (int) ReflectionTestUtils.getField(result, "score");
        boolean disponible = (boolean) ReflectionTestUtils.getField(result, "disponibleCreneauDemande");

        assertThat(score).isEqualTo(-1);
        assertThat(disponible).isFalse();
    }

    @Test
    void scorerSalle_equipementAdapteReunion_bonusScore() {
        Salle salleAvecEquip = new Salle();
        salleAvecEquip.setCapacite(10);
        salleAvecEquip.setPrix(30.0);
        salleAvecEquip.setEquipement("projecteur, wifi, tableau");

        Salle salleSansEquip = new Salle();
        salleSansEquip.setCapacite(10);
        salleSansEquip.setPrix(30.0);
        salleSansEquip.setEquipement("");

        Object resultAvec = ReflectionTestUtils.invokeMethod(ragService, "scorerSalle",
                salleAvecEquip, 8, null, null, null, "REUNION", "",
                Collections.emptyList(), Collections.emptyList());
        Object resultSans = ReflectionTestUtils.invokeMethod(ragService, "scorerSalle",
                salleSansEquip, 8, null, null, null, "REUNION", "",
                Collections.emptyList(), Collections.emptyList());

        int scoreAvec = (int) ReflectionTestUtils.getField(resultAvec, "score");
        int scoreSans = (int) ReflectionTestUtils.getField(resultSans, "score");

        assertThat(scoreAvec).isGreaterThan(scoreSans);
    }
}