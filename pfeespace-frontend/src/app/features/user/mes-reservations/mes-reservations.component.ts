import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { ReservationService } from '../../../core/services/reservation.service';
import { SalleService, Salle } from '../../../core/services/salle.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProfilSharedService } from '../../../core/services/profil-shared.service';
import { NotificationService, NotificationItem } from '../../../core/services/notification.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-mes-reservations',
  templateUrl: './mes-reservations.component.html',
  styleUrls: ['./mes-reservations.component.css']
})
export class MesReservationsComponent implements OnInit, OnDestroy {

  reservations: any[] = [];
  reservationsFiltrees: any[] = [];
  activeTab: string = 'TOUS';
  filterDateDebut: string = '';
  filterDateFin: string = '';
  error: string = '';
  profil: any = null;
  userMenuOpen: boolean = false;

  // Panneau de filtres (déclenché par le bouton "Filtres")
  showFilterPanel: boolean = false;

  // Popup de détail d'une réservation (carte cliquée)
  showDetailModal: boolean = false;
  selectedReservation: any = null;

  // Modale de confirmation d'annulation
  showConfirmModal: boolean = false;
  reservationToCancel: number | null = null;
  cancelling: boolean = false;

  // ✅ NOTIFICATIONS
  notifMenuOpen = false;
  notifications: NotificationItem[] = [];
  notifCount = 0;
  private destroy$ = new Subject<void>();

  // ✅ RECHERCHE DYNAMIQUE — cherche parmi les SALLES, pas les réservations
  salles: Salle[] = [];
  searchTerm = '';
  searchOpen = false;

  // Classes de couverture utilisées quand la salle n'a pas de photo
  private readonly coverClasses = ['cover-1', 'cover-2', 'cover-3', 'cover-4', 'cover-5'];

  constructor(
    public router: Router,
    private reservationService: ReservationService,
    private salleService: SalleService,
    private auth: AuthService,
    private profilShared: ProfilSharedService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.profilShared.loadProfil();
    this.profilShared.profil$.subscribe(p => this.profil = p);
    this.loadReservations();
    this.loadSalles();
    this.startNotificationPolling();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.notificationService.stopPolling();
  }

  toggleUserMenu(): void {
    this.userMenuOpen = !this.userMenuOpen;
  }

  // ✅ Polling toutes les 30s pour le compteur de notifs non lues
  startNotificationPolling() {
    this.notificationService.startPolling(30000)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => this.notifCount = res.count,
        error: (err) => console.error('Erreur chargement notifications', err)
      });
  }

  toggleNotifMenu() {
    this.notifMenuOpen = !this.notifMenuOpen;
    this.userMenuOpen = false;

    if (this.notifMenuOpen) {
      this.notificationService.getNotifications().subscribe({
        next: (data) => this.notifications = data,
        error: () => this.error = 'Erreur chargement notifications'
      });
    }
  }

  onNotificationClick(notif: NotificationItem) {
    if (!notif.lu) {
      this.notificationService.marquerCommeLue(notif.id).subscribe(() => {
        notif.lu = true;
        this.notifCount = Math.max(0, this.notifCount - 1);
      });
    }
    this.notifMenuOpen = false;
    if (notif.lien) {
      this.router.navigateByUrl(notif.lien);
    }
  }

  marquerToutesLues() {
    this.notificationService.marquerToutesCommeLues().subscribe(() => {
      this.notifications.forEach(n => n.lu = true);
      this.notifCount = 0;
    });
  }

  timeAgo(dateStr: string): string {
    const date = new Date(dateStr);
    const diffMs = Date.now() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return 'À l\'instant';
    if (diffMin < 60) return `Il y a ${diffMin} min`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `Il y a ${diffH} h`;
    const diffJ = Math.floor(diffH / 24);
    return `Il y a ${diffJ} j`;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.user-menu')) {
      this.userMenuOpen = false;
    }
    if (!target.closest('.notif-wrapper')) {
      this.notifMenuOpen = false;
    }
    if (!target.closest('.topnav-center')) {
      this.searchOpen = false;
    }
    if (!target.closest('.filter-panel') && !target.closest('.btn-filtres')) {
      this.showFilterPanel = false;
    }
  }

  loadReservations() {
    this.reservationService.getMesReservations().subscribe({
      next: (data: any[]) => {
        this.reservations = data;
        this.applyFilters();
      },
      error: () => this.error = 'Erreur lors du chargement'
    });
  }

  loadSalles() {
    this.salleService.getAll().subscribe({
      next: (data) => this.salles = data,
      error: () => {}
    });
  }

  setTab(tab: string) {
    this.activeTab = tab;
    this.applyFilters();
  }

  applyFilters() {
    let result = [...this.reservations];

    if (this.activeTab !== 'TOUS') {
      result = result.filter(r => r.statut === this.activeTab);
    }

    if (this.filterDateDebut) {
      result = result.filter(r => r.date >= this.filterDateDebut);
    }

    if (this.filterDateFin) {
      result = result.filter(r => r.date <= this.filterDateFin);
    }

    this.reservationsFiltrees = result;
  }

  resetDateFilter() {
    this.filterDateDebut = '';
    this.filterDateFin = '';
    this.applyFilters();
  }

  countByStatut(statut: string): number {
    return this.reservations.filter(r => r.statut === statut).length;
  }

  // ── Panneau de filtres ──
  toggleFilterPanel(): void {
    this.showFilterPanel = !this.showFilterPanel;
  }

  activeFilterCount(): number {
    let n = 0;
    if (this.activeTab !== 'TOUS') n++;
    if (this.filterDateDebut) n++;
    if (this.filterDateFin) n++;
    return n;
  }

  resetAllFilters(): void {
    this.activeTab = 'TOUS';
    this.filterDateDebut = '';
    this.filterDateFin = '';
    this.applyFilters();
  }

  // ── Recherche dynamique : uniquement parmi les salles ──
  get searchResults(): Salle[] {
    const q = this.searchTerm.trim().toLowerCase();
    if (!q) return [];
    return this.salles.filter(s =>
      (s.nom || '').toLowerCase().includes(q)
    );
  }

  onSearchInput(): void {
    this.searchOpen = this.searchTerm.trim().length > 0;
  }

  onSearchFocus(): void {
    if (this.searchTerm.trim().length > 0) {
      this.searchOpen = true;
    }
  }

  clearSearch(): void {
    this.searchTerm = '';
    this.searchOpen = false;
  }

  // Sélection d'une salle dans les résultats → aller réserver cette salle
  selectSearchResult(s: Salle): void {
    this.searchOpen = false;
    this.searchTerm = '';
    this.router.navigate(['/reservation', s.id]);
  }

  // ── Cartes galerie ──
  getCoverClass(r: any): string {
    const key = (r.salleNom || r.salle?.nom || r.nom || '').toString();
    let hash = 0;
    for (let i = 0; i < key.length; i++) {
      hash = (hash * 31 + key.charCodeAt(i)) >>> 0;
    }
    return this.coverClasses[hash % this.coverClasses.length];
  }

 getPhotoUrl(r: any): string | null {
  return r.salle?.image || r.salle?.photoUrl || r.salle?.photo || r.sallePhoto || r.image || null;
}

  // ── Popup de détail ──
  openDetail(r: any): void {
    this.selectedReservation = r;
    this.showDetailModal = true;
  }

  closeDetail(): void {
    this.showDetailModal = false;
    this.selectedReservation = null;
  }

  // ── Annulation avec modale personnalisée ──
  annuler(id: number) {
    this.reservationToCancel = id;
    this.showConfirmModal = true;
  }

  closeConfirmModal(): void {
    if (this.cancelling) return;
    this.showConfirmModal = false;
    this.reservationToCancel = null;
  }

  confirmerAnnulation(): void {
    if (this.reservationToCancel === null) return;

    this.cancelling = true;
    this.reservationService.supprimer(this.reservationToCancel).subscribe({
      next: () => {
        this.cancelling = false;
        this.showConfirmModal = false;
        this.reservationToCancel = null;
        this.showDetailModal = false;
        this.selectedReservation = null;
        this.loadReservations();
      },
      error: () => {
        this.cancelling = false;
        this.showConfirmModal = false;
        this.reservationToCancel = null;
        this.error = "Erreur lors de l'annulation";
      }
    });
  }

  logout(): void {
    this.auth.logout();
  }
}