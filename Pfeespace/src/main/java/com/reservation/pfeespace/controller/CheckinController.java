package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.entity.Reservation;
import com.reservation.pfeespace.service.CheckinService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkin")
@CrossOrigin(origins = {"http://localhost:4200", "http://192.168.0.247:4200"})
public class CheckinController {

    private final CheckinService checkinService;

    public CheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    // Appelé automatiquement par la page Angular /checkin/:token à l'ouverture
    @PostMapping("/{token}")
    public ResponseEntity<Reservation> checkin(@PathVariable String token) {
        return ResponseEntity.ok(checkinService.checkIn(token));
    }
}