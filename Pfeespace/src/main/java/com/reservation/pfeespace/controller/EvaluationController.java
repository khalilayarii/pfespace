package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.service.EvaluationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/{token}")
    public Map<String, String> soumettre(@PathVariable String token, @RequestBody EvaluationRequest body) {
        evaluationService.soumettreEvaluation(
                token,
                body.noteProprete(),
                body.noteEquipement(),
                body.noteFaciliteReservation(),
                body.capaciteAdaptee(),
                body.problemesRencontres(),
                body.suggestionsAmelioration()
        );
        return Map.of("message", "Merci pour votre retour !");
    }

    public record EvaluationRequest(
            Integer noteProprete,
            Integer noteEquipement,
            Integer noteFaciliteReservation,
            Boolean capaciteAdaptee,
            String problemesRencontres,
            String suggestionsAmelioration
    ) {}
}