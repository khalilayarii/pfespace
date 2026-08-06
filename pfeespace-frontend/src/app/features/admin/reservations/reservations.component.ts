import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ReservationService, Reservation } from '../../../core/services/reservation.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reservations',
  templateUrl: './reservations.component.html',
  styleUrls: ['./reservations.component.css']
})
export class ReservationsComponent implements OnInit {

  profil: any = null;
  statut = 'TOUS';
  reservations: Reservation[] = [];
  loading = false;
  message = '';
  messageType = '';
  hideTabs = false;

  // Image par défaut (Placeholder propre si pas d'image enregistrée)
  private defaultImage = 'https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=600&q=80';

  constructor(
    private reservationService: ReservationService,
    private auth: AuthService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.loadUserProfile();

    const statutForce = this.route.snapshot.data['statutForce'];

    if (statutForce) {
      this.statut = statutForce;
      this.hideTabs = true;
      this.loadReservations();
    } else {
      this.route.queryParams.subscribe(params => {
        this.statut = params['statut'] || 'TOUS';
        this.loadReservations();
      });
    }
  }

  loadUserProfile() {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        this.profil = JSON.parse(userStr);
      } catch (e) {
        console.error('Erreur lecture profil user', e);
      }
    }
  }

  loadReservations() {
    this.loading = true;

    const obs = this.statut === 'TOUS'
      ? this.reservationService.getAll()
      : this.reservationService.getByStatut(this.statut);

    obs.subscribe({
      next: (data) => {
        this.reservations = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement réservations', err);
        this.loading = false;
      }
    });
  }

  /**
   * ✅ GESTION DU CHARGEMENT ET DU FORMATAGE DE L'IMAGE
   * Reconstitue l'URL complète si Spring Boot renvoie un chemin relatif.
   */
  getSalleImage(salle: any): string {
    if (!salle) {
      return this.defaultImage;
    }

    // Récupération de l'image (salle.image dans votre cas)
    const imgPath = salle.image || salle.imageUrl || salle.photo;

    if (!imgPath || imgPath.trim() === '') {
      return this.defaultImage;
    }

    // Si c'est déjà une URL HTTP/HTTPS complète ou une chaîne base64
    if (imgPath.startsWith('http://') || imgPath.startsWith('https://') || imgPath.startsWith('data:image')) {
      return imgPath;
    }

    // Si c'est un chemin relatif envoyé par Spring Boot (ex: /uploads/image.jpg)
    const cleanPath = imgPath.startsWith('/') ? imgPath : '/' + imgPath;
    return `http://localhost:8083${cleanPath}`;
  }

  /**
   * ✅ FALLBACK EN CAS D'ERREUR DE CHARGEMENT (404)
   */
  onImgError(event: any) {
    event.target.src = this.defaultImage;
  }

  confirmer(id: number) {
    this.reservationService.confirmer(id).subscribe({
      next: () => {
        this.showMessage('Réservation confirmée !', 'success');
        this.loadReservations();
      },
      error: () => this.showMessage('Erreur lors de la confirmation', 'error')
    });
  }

  refuser(id: number) {
    this.reservationService.refuser(id).subscribe({
      next: () => {
        this.showMessage('Réservation refusée !', 'success');
        this.loadReservations();
      },
      error: () => this.showMessage('Erreur lors du refus', 'error')
    });
  }

  attente(id: number) {
    this.reservationService.attente(id).subscribe({
      next: () => {
        this.showMessage('Réservation mise en attente !', 'success');
        this.loadReservations();
      },
      error: () => this.showMessage('Erreur', 'error')
    });
  }

  showMessage(msg: string, type: string) {
    this.message = msg;
    this.messageType = type;
    setTimeout(() => this.message = '', 3000);
  }

  logout() {
    this.auth.logout();
  }
}