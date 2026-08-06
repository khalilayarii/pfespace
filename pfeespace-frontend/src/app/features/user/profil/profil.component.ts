import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { SalleService, Salle } from '../../../core/services/salle.service';
import { NotificationService, NotificationItem } from '../../../core/services/notification.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-profil',
  templateUrl: './profil.component.html',
  styleUrls: ['./profil.component.css']
})
export class ProfilComponent implements OnInit, OnDestroy {

  profil: any = null;
  editMode = false;
  error = '';
  success = '';
  loading = false;
  userMenuOpen = false;

  nom = '';
  prenom = '';
  telephone = '';

  // ✅ NOTIFICATIONS
  notifMenuOpen = false;
  notifications: NotificationItem[] = [];
  notifCount = 0;
  private destroy$ = new Subject<void>();

  // ✅ RECHERCHE DYNAMIQUE — parmi les salles
  salles: Salle[] = [];
  searchTerm = '';
  searchOpen = false;

  // Classes de couverture utilisées quand la salle n'a pas de photo
  private readonly coverClasses = ['cover-1', 'cover-2', 'cover-3', 'cover-4', 'cover-5'];

  constructor(
    private userService: UserService,
    private auth: AuthService,
    private salleService: SalleService,
    private notificationService: NotificationService,
    public router: Router
  ) {}

  ngOnInit() {
    this.loadProfil();
    this.loadSalles();
    this.startNotificationPolling();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.notificationService.stopPolling();
  }

  loadProfil() {
    this.userService.getProfil().subscribe({
      next: (data) => {
        this.profil = data;
        this.nom = data.nom;
        this.prenom = data.prenom;
        this.telephone = data.telephone;
      },
      error: () => { this.error = 'Erreur lors du chargement du profil'; }
    });
  }

  loadSalles() {
    this.salleService.getAll().subscribe({
      next: (data) => this.salles = data,
      error: () => {}
    });
  }

  toggleEdit() {
    this.editMode = !this.editMode;
    this.error = '';
    this.success = '';
  }

  toggleUserMenu() {
    this.userMenuOpen = !this.userMenuOpen;
    this.notifMenuOpen = false;
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

  selectSearchResult(s: Salle): void {
    this.searchOpen = false;
    this.searchTerm = '';
    this.router.navigate(['/reservation', s.id]);
  }

  getCoverClass(s: Salle): string {
    const key = (s.nom || '').toString();
    let hash = 0;
    for (let i = 0; i < key.length; i++) {
      hash = (hash * 31 + key.charCodeAt(i)) >>> 0;
    }
    return this.coverClasses[hash % this.coverClasses.length];
  }

  onUpdate() {
    this.loading = true;
    this.error = '';
    this.success = '';

    this.userService.updateProfil({
      nom: this.nom,
      prenom: this.prenom,
      telephone: this.telephone
    }).subscribe({
      next: (updated) => {
        this.profil = updated;
        this.success = 'Profil mis à jour avec succès !';
        this.editMode = false;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors de la mise à jour';
        this.loading = false;
      }
    });
  }

  onLogout() { this.auth.logout(); }
}