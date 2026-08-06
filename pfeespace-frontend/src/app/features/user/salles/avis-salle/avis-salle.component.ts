import { Component, Input, OnInit } from '@angular/core';
import { AvisService } from 'src/app/core/services/avis.service';

@Component({
  selector: 'app-avis-salle',
  templateUrl: './avis-salle.component.html',
  styleUrls: ['./avis-salle.component.css']
})
export class AvisSalleComponent implements OnInit {
  @Input() salleId!: number;

  scoreMoyen = 0;
  nombreAvis = 0;
  avis: any[] = [];
  chargementTermine = false; // ✅ AJOUT

  constructor(private avisService: AvisService) {}

  ngOnInit(): void {
    this.avisService.getAvisSalle(this.salleId).subscribe(res => {
      this.scoreMoyen = res.scoreMoyen;
      this.nombreAvis = res.nombreAvis;
      this.avis = res.avis;
      this.chargementTermine = true; // ✅ AJOUT
    });
  }
}