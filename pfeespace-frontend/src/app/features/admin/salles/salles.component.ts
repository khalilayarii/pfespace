import { Component, OnInit } from '@angular/core';
import { SalleService, Salle } from '../../../core/services/salle.service';
import { AuthService } from '../../../core/services/auth.service';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-salles',
  templateUrl: './salles.component.html',
  styleUrls: ['./salles.component.css']
})
export class SallesComponent implements OnInit {

  salles: Salle[] = [];
  showModal = false;
  isEdit = false;
  loading = false;
  uploading = false;      // ✅ état upload
  message = '';
  messageType = '';
  sidebarOpen = false;

  // ✅ MODAL DE CONFIRMATION SUPPRESSION
  showDeleteModal = false;
  salleToDeleteId: number | null = null;

  salle: Salle = {
    nom: '', description: '', capacite: 0,
    equipement: '', prix: 0, disponible: true,
    image: ''
  };

  constructor(
    private salleService: SalleService,
    private auth: AuthService,
    private http: HttpClient    // ✅ pour upload
  ) {}

  ngOnInit() { this.loadSalles(); }

  loadSalles() {
    this.salleService.getAll().subscribe({
      next: (data) => this.salles = data,
      error: () => this.showMessage('Erreur lors du chargement', 'error')
    });
  }

  openAdd() {
    this.isEdit = false;
    this.salle = {
      nom: '', description: '', capacite: 0,
      equipement: '', prix: 0, disponible: true, image: ''
    };
    this.showModal = true;
  }

  openEdit(s: Salle) {
    this.isEdit = true;
    this.salle = { ...s };
    this.showModal = true;
  }

  closeModal() { this.showModal = false; }

  // ✅ Upload image vers Spring Boot
  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (!file) return;

    this.uploading = true;
    const formData = new FormData();
    formData.append('file', file);

    this.http.post<{ url: string }>('http://localhost:8083/api/upload', formData)
      .subscribe({
        next: (res) => {
          this.salle.image = res.url;   // ✅ on stocke l'URL HTTP
          this.uploading = false;
        },
        error: () => {
          this.showMessage('Erreur upload image', 'error');
          this.uploading = false;
        }
      });
  }

  save() {
    this.loading = true;
    if (this.isEdit && this.salle.id) {
      this.salleService.update(this.salle.id, this.salle).subscribe({
        next: () => {
          this.showMessage('Salle modifiée !', 'success');
          this.loadSalles(); this.closeModal(); this.loading = false;
        },
        error: () => {
          this.showMessage('Erreur modification', 'error');
          this.loading = false;
        }
      });
    } else {
      this.salleService.create(this.salle).subscribe({
        next: () => {
          this.showMessage('Salle ajoutée !', 'success');
          this.loadSalles(); this.closeModal(); this.loading = false;
        },
        error: () => {
          this.showMessage("Erreur ajout", 'error');
          this.loading = false;
        }
      });
    }
  }

  // ✅ Ouvre le modal de confirmation au lieu de confirm()
  askDelete(id: number) {
    this.salleToDeleteId = id;
    this.showDeleteModal = true;
  }

  cancelDelete() {
    this.showDeleteModal = false;
    this.salleToDeleteId = null;
  }

  confirmDelete() {
    if (this.salleToDeleteId === null) return;

    this.salleService.delete(this.salleToDeleteId).subscribe({
      next: () => {
        this.showMessage('Salle supprimée !', 'success');
        this.loadSalles();
        this.cancelDelete();
      },
      error: (err) => {
        // ✅ Affiche le vrai message renvoyé par le backend
        // (ex: "Impossible de supprimer... X réservation(s) liée(s)")
        const msg = typeof err.error === 'string' && err.error.length > 0
          ? err.error
          : 'Erreur lors de la suppression';
        this.showMessage(msg, 'error');
        this.cancelDelete();
      }
    });
  }

  showMessage(msg: string, type: string) {
    this.message = msg; this.messageType = type;
    setTimeout(() => this.message = '', 5000);
  }

  logout() { this.auth.logout(); }
}