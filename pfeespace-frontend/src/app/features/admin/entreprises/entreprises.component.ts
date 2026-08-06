import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { EntrepriseService, Entreprise } from '../../../core/services/entreprise.service';

@Component({
  selector: 'app-entreprises',
  templateUrl: './entreprises.component.html',
  styleUrls: ['./entreprises.component.css']
})
export class EntreprisesComponent implements OnInit {

  entreprises: Entreprise[] = [];
  showForm = false;
  isEditing = false;
  loading = false;
  message = '';
  messageType = 'success';
  selectedId?: number;

  form: Entreprise = {
    nom: '',
    numFiscal: '',
    adresse: '',
    email: '',
    telephone: '',
    estMembre: false
  };

  constructor(
    private entrepriseService: EntrepriseService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadEntreprises();
  }

  loadEntreprises(): void {
    this.loading = true;
    this.entrepriseService.getAll().subscribe({
      next: (data) => {
        this.entreprises = data;
        this.loading = false;
      },
      error: () => {
        this.showMessage('Erreur lors du chargement', 'danger');
        this.loading = false;
      }
    });
  }

  openAdd(): void {
    this.isEditing = false;
    this.selectedId = undefined;
    this.form = {
      nom: '',
      numFiscal: '',
      adresse: '',
      email: '',
      telephone: '',
      estMembre: false
    };
    this.showForm = true;
    this.message = '';
  }

  openEdit(e: Entreprise): void {
    this.isEditing = true;
    this.selectedId = e.id;
    this.form = { ...e };
    this.showForm = true;
    this.message = '';
  }

  save(): void {
    // Validation
    if (!this.form.nom || !this.form.numFiscal) {
      this.showMessage('Nom et numéro fiscal sont obligatoires', 'danger');
      return;
    }

    this.loading = true;

    if (this.isEditing && this.selectedId) {
      this.entrepriseService.update(this.selectedId, this.form).subscribe({
        next: () => {
          this.showMessage('✅ Entreprise modifiée avec succès', 'success');
          this.showForm = false;
          this.loadEntreprises();
        },
        error: () => {
          this.showMessage('❌ Erreur lors de la modification', 'danger');
          this.loading = false;
        }
      });
    } else {
      this.entrepriseService.create(this.form).subscribe({
        next: () => {
          this.showMessage('✅ Entreprise ajoutée avec succès', 'success');
          this.showForm = false;
          this.loadEntreprises();
        },
        error: () => {
          this.showMessage('❌ Erreur lors de l\'ajout', 'danger');
          this.loading = false;
        }
      });
    }
  }

  delete(id: number): void {
    if (confirm('Voulez-vous vraiment supprimer cette entreprise ?')) {
      this.entrepriseService.delete(id).subscribe({
        next: () => {
          this.showMessage('✅ Entreprise supprimée', 'success');
          this.loadEntreprises();
        },
        error: () => {
          this.showMessage('❌ Erreur lors de la suppression', 'danger');
        }
      });
    }
  }

  cancel(): void {
    this.showForm = false;
    this.message = '';
  }

  logout(): void {
    localStorage.clear();
    this.router.navigate(['/admin/login']);
  }

  private showMessage(msg: string, type: string): void {
    this.message = msg;
    this.messageType = type;
    setTimeout(() => this.message = '', 4000);
  }
}