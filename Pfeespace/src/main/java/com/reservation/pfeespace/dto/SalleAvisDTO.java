package com.reservation.pfeespace.dto;

import java.util.List;

public record SalleAvisDTO(
        double scoreMoyen,
        int nombreAvis,
        List<AvisDTO> avis
) {}