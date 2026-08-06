package com.reservation.pfeespace.controller;

import com.reservation.pfeespace.dto.EntrepriseDTO;
import com.reservation.pfeespace.service.EntrepriseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entreprises")
@CrossOrigin(origins = "http://localhost:4200")
public class EntrepriseController {

    @Autowired
    private EntrepriseService entrepriseService;

    // GET toutes les entreprises
    @GetMapping
    public List<EntrepriseDTO> getAll() {
        return entrepriseService.getAll();
    }

    // POST créer une entreprise
    @PostMapping
    public ResponseEntity<EntrepriseDTO> create(@RequestBody EntrepriseDTO dto) {
        return ResponseEntity.ok(entrepriseService.create(dto));
    }

    // PUT modifier une entreprise
    @PutMapping("/{id}")
    public ResponseEntity<EntrepriseDTO> update(@PathVariable Long id,
                                                @RequestBody EntrepriseDTO dto) {
        return ResponseEntity.ok(entrepriseService.update(id, dto));
    }

    // DELETE supprimer une entreprise
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        entrepriseService.delete(id);
        return ResponseEntity.ok().build();
    }

    // GET vérifier num fiscal (pour formulaire réservation)
    @GetMapping("/verifier/{numFiscal}")
    public ResponseEntity<EntrepriseDTO> verifier(@PathVariable String numFiscal) {
        EntrepriseDTO dto = entrepriseService.verifierNumFiscal(numFiscal);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }
}
