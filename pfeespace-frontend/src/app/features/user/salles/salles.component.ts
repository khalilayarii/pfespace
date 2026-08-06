import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { SalleService, Salle } from '../../../core/services/salle.service';
import { ReservationService, Reservation } from '../../../core/services/reservation.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProfilSharedService } from '../../../core/services/profil-shared.service';
import { NotificationService, NotificationItem } from '../../../core/services/notification.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-salles-client',
  templateUrl: './salles.component.html',
  styleUrls: ['./salles.component.css']
})
export class SallesClientComponent implements OnInit, OnDestroy {

  salles: Salle[] = [];
  message = '';
  messageType = '';
  profil: any = null;
  userMenuOpen = false;

  // ✅ NOTIFICATIONS
  notifMenuOpen = false;
  notifications: NotificationItem[] = [];
  notifCount = 0;
  private destroy$ = new Subject<void>();

  showCalendar = false;
  selectedSalle: Salle | null = null;
  reservationsSalle: Reservation[] = [];
  currentMonth: number = new Date().getMonth();
  currentYear: number = new Date().getFullYear();
  selectedDay: number | null = null;
  jours = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
  daysInMonth: number[] = [];
  emptyDays: number[] = [];

  // ✅ POPUP DE DÉTAIL D'UNE SALLE
  showDetail = false;
  selectedSalleDetail: Salle | null = null;

  // ✅ RECHERCHE DYNAMIQUE (barre du haut)
  searchTerm = '';
  searchOpen = false;

  constructor(
    private salleService: SalleService,
    private reservationService: ReservationService,
    private auth: AuthService,
    private router: Router,
    private profilShared: ProfilSharedService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.profilShared.loadProfil();
    this.profilShared.profil$.subscribe(p => this.profil = p);
    this.loadSalles();
    this.startNotificationPolling();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.notificationService.stopPolling();
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
        error: () => this.showMessage('Erreur chargement notifications', 'error')
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
  onDocumentClick(event: MouseEvent) {
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
  }

  toggleUserMenu() {
    this.userMenuOpen = !this.userMenuOpen;
    this.notifMenuOpen = false;
  }

  hideImg(event: any) { event.target.style.display = 'none'; }

  loadSalles() {
    this.salleService.getAll().subscribe({
      next: (data) => this.salles = data.filter(s => s.disponible),
      error: () => this.showMessage('Erreur lors du chargement', 'error')
    });
  }

  // ✅ Popup de détail (au clic sur la carte)
  openDetail(salle: Salle) {
    this.selectedSalleDetail = salle;
    this.showDetail = true;
    this.searchOpen = false;
  }

  closeDetail() {
    this.showDetail = false;
    this.selectedSalleDetail = null;
  }

  // ✅ Recherche dynamique
  get searchResults(): Salle[] {
    const q = this.searchTerm.trim().toLowerCase();
    if (!q) return [];
    return this.salles.filter(s =>
      (s.nom || '').toLowerCase().includes(q) ||
      (s.description || '').toLowerCase().includes(q) ||
      (s.equipement || '').toLowerCase().includes(q)
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

  selectSearchResult(s: Salle): void {
    this.openDetail(s);
  }

 openCalendar(salle: Salle) {
  this.selectedSalle = salle;
  this.currentMonth = new Date().getMonth();
  this.currentYear = new Date().getFullYear();
  this.selectedDay = new Date().getDate(); // ✅ sélectionne automatiquement aujourd'hui
  this.showCalendar = true;
  this.buildCalendar();
  this.reservationService.getAll().subscribe({
    next: (data) => {
      this.reservationsSalle = data.filter(r =>
        r.salle?.id === salle.id && r.statut === 'CONFIRMEE'
      );
    }
  });
}

  closeCalendar() {
    this.showCalendar = false;
    this.selectedSalle = null;
    this.selectedDay = null;
  }

  buildCalendar() {
    const firstDay = new Date(this.currentYear, this.currentMonth, 1).getDay();
    const totalDays = new Date(this.currentYear, this.currentMonth + 1, 0).getDate();
    const startOffset = firstDay === 0 ? 6 : firstDay - 1;
    this.emptyDays = Array(startOffset).fill(0);
    this.daysInMonth = Array.from({ length: totalDays }, (_, i) => i + 1);
  }

  prevMonth() {
    if (this.currentMonth === 0) { this.currentMonth = 11; this.currentYear--; }
    else { this.currentMonth--; }
    this.selectedDay = null;
    this.buildCalendar();
  }

  nextMonth() {
    if (this.currentMonth === 11) { this.currentMonth = 0; this.currentYear++; }
    else { this.currentMonth++; }
    this.selectedDay = null;
    this.buildCalendar();
  }

  getMonthLabel(): string {
    const mois = ['Janvier','Février','Mars','Avril','Mai','Juin',
                  'Juillet','Août','Septembre','Octobre','Novembre','Décembre'];
    return `${mois[this.currentMonth]} ${this.currentYear}`;
  }

  hasReservation(day: number): boolean {
    return this.getReservations(day).length > 0;
  }

  getReservations(day: number): Reservation[] {
    const dateStr = `${this.currentYear}-${String(this.currentMonth+1).padStart(2,'0')}-${String(day).padStart(2,'0')}`;
    return this.reservationsSalle.filter(r => r.date === dateStr);
  }

  selectDay(day: number) { this.selectedDay = day; }

  isToday(day: number): boolean {
    const today = new Date();
    return day === today.getDate() &&
           this.currentMonth === today.getMonth() &&
           this.currentYear === today.getFullYear();
  }

  getDayClass(day: number): string {
    const count = this.getReservations(day).length;
    if (count === 0) return '';
    if (count >= 5) return 'day-red';
    return 'day-green';
  }

  showMessage(msg: string, type: string) {
    this.message = msg;
    this.messageType = type;
    setTimeout(() => this.message = '', 3000);
  }

  logout() { this.auth.logout(); }
  reserver(id: number) { this.router.navigate(['/reservation', id]); }
}