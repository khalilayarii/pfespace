package com.reservation.pfeespace.dto;

public record EvaluationRequest(
        Integer noteProprete,
        Integer noteEquipement,
        Integer noteFaciliteReservation,
        Boolean capaciteAdaptee,
        String problemesRencontres,
        String suggestionsAmelioration


) {}