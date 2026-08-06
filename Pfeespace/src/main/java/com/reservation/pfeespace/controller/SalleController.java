package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.dto.SalleAvisDTO;
import com.reservation.pfeespace.entity.Salle;
import com.reservation.pfeespace.service.SalleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salles")
@CrossOrigin(origins = "http://localhost:4200")
public class SalleController {

    private final SalleService salleService;

    // ← constructeur manuel
    public SalleController(SalleService salleService) {
        this.salleService = salleService;
    }

    @GetMapping
    public List<Salle> getAll() {
        return salleService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Salle> getById(@PathVariable Long id) {
        return ResponseEntity.ok(salleService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Salle> create(@RequestBody Salle salle) {
        return ResponseEntity.ok(salleService.create(salle));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Salle> update(@PathVariable Long id, @RequestBody Salle salle) {
        return ResponseEntity.ok(salleService.update(id, salle));
    }

    // ✅ MODIFIÉ — renvoie le message d'erreur exact au lieu d'un 500 muet
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            salleService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
    @GetMapping("/{id}/avis")
    public ResponseEntity<SalleAvisDTO> getAvis(@PathVariable Long id) {
        return ResponseEntity.ok(salleService.getAvisEtScore(id));
    }
}