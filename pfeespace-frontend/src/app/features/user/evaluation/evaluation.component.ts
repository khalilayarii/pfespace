import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { EvaluationService, EvaluationRequest } from 'src/app/core/services/evaluation.service';

type EtatFormulaire = 'formulaire' | 'chargement' | 'succes' | 'erreur';

@Component({
  selector: 'app-evaluation',
  templateUrl: './evaluation.component.html',
  styleUrls: ['./evaluation.component.css']
})
export class EvaluationComponent implements OnInit {

  token: string = '';
  etat: EtatFormulaire = 'formulaire';
  messageErreur: string = '';

  noteProprete: number = 0;
  noteEquipement: number = 0;
  noteFaciliteReservation: number = 0;
  capaciteAdaptee: boolean | null = null;
  problemesRencontres: string = '';
  suggestionsAmelioration: string = '';

  constructor(
    private route: ActivatedRoute,
    private evaluationService: EvaluationService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') || '';
  }

  noter(critere: 'proprete' | 'equipement' | 'facilite', valeur: number): void {
    if (critere === 'proprete') this.noteProprete = valeur;
    if (critere === 'equipement') this.noteEquipement = valeur;
    if (critere === 'facilite') this.noteFaciliteReservation = valeur;
  }

  choisirCapacite(valeur: boolean): void {
    this.capaciteAdaptee = valeur;
  }

  peutEnvoyer(): boolean {
    return this.noteProprete > 0 &&
           this.noteEquipement > 0 &&
           this.noteFaciliteReservation > 0 &&
           this.capaciteAdaptee !== null;
  }

  envoyer(): void {
    if (!this.peutEnvoyer()) {
      return;
    }

    this.etat = 'chargement';

    const data: EvaluationRequest = {
      noteProprete: this.noteProprete,
      noteEquipement: this.noteEquipement,
      noteFaciliteReservation: this.noteFaciliteReservation,
      capaciteAdaptee: this.capaciteAdaptee as boolean,
      problemesRencontres: this.problemesRencontres,
      suggestionsAmelioration: this.suggestionsAmelioration
    };

    this.evaluationService.soumettreEvaluation(this.token, data).subscribe({
      next: () => {
        this.etat = 'succes';
      },
      error: (err) => {
        this.etat = 'erreur';
        this.messageErreur = err?.error?.message || 'Une erreur est survenue. Ce lien a peut-être déjà été utilisé.';
      }
    });
  }
}