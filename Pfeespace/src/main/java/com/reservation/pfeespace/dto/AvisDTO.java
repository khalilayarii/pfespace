package com.reservation.pfeespace.dto;

import java.time.LocalDateTime;

public record AvisDTO(
        String auteur,
        double note,
        String commentaire,
        LocalDateTime date
) {}
