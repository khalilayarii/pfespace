import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { ReservationService } from '../../../core/services/reservation.service';
import { SalleService } from '../../../core/services/salle.service';

@Component({
  selector: 'app-calendrier',
  templateUrl: './calendrier.component.html',
  styleUrls: ['./calendrier.component.css']
})
export class CalendrierComponent implements OnInit {

  sidebarOpen = false;
  reservations: any[] = [];
  salles: any[] = [];

  // Vue actuelle
  vue: 'mois' | 'semaine' | 'jour' = 'mois';

  // Navigation
  currentDate = new Date();
  currentMonth = new Date().getMonth();
  currentYear = new Date().getFullYear();
  currentDay = new Date().getDate();

  // Calendrier mois
  days: any[] = [];

  // Panel détails
  selectedDate: string = '';
  selectedReservations: any[] = [];
  showPanel = false;

  // Formulaire
  showForm = false;
  loading = false;
  success = '';
  error = '';

  nouvelleReservation = {
    salleId: '',
    typeUtilisateur: '',
    natureManifestation: '',
    date: '',
    heureDebut: '',
    heureFin: '',
    nomComplet: '',
    mail: '',
    societe: '',
    telephone: '',
    description: ''
  };

  moisNoms = ['Janvier','Février','Mars','Avril','Mai','Juin',
              'Juillet','Août','Septembre','Octobre','Novembre','Décembre'];
  joursNoms = ['Dim','Lun','Mar','Mer','Jeu','Ven','Sam'];
  heures = Array.from({length: 14}, (_, i) => i + 7); // 7h → 20h

  constructor(
    private reservationService: ReservationService,
    private salleService: SalleService,
    private auth: AuthService
  ) {}

  ngOnInit() {
    this.loadReservations();
    this.loadSalles();
  }

  loadSalles() {
    this.salleService.getAll().subscribe({ next: (data) => this.salles = data });
  }

 loadReservations() {
    this.reservationService.getAll().subscribe({
      next: (data: any[]) => {
        this.reservations = data.filter(r =>
          ['CONFIRMEE', 'EN_COURS', 'TERMINEE'].includes(r.statut)
        );
        this.buildCalendar();
      },
      error: () => this.buildCalendar()
    });
  }

  // ═══════════════ VUE MOIS ═══════════════
  buildCalendar() {
    this.days = [];
    const firstDay = new Date(this.currentYear, this.currentMonth, 1);
    const lastDay = new Date(this.currentYear, this.currentMonth + 1, 0);
    let startDay = firstDay.getDay();
    startDay = startDay === 0 ? 6 : startDay - 1;

    for (let i = 0; i < startDay; i++) {
      this.days.push({ day: null, reservations: [] });
    }

    for (let d = 1; d <= lastDay.getDate(); d++) {
      const dateStr = this.formatDate(this.currentYear, this.currentMonth + 1, d);
      const resaDuJour = this.reservations.filter(r => r.date === dateStr);
      const today = new Date();
      const isToday = d === today.getDate() &&
                      this.currentMonth === today.getMonth() &&
                      this.currentYear === today.getFullYear();
      this.days.push({ day: d, date: dateStr, reservations: resaDuJour, isToday });
    }
  }

  // ═══════════════ VUE SEMAINE ═══════════════
  get semaineJours(): any[] {
    const days = [];
    const startOfWeek = new Date(this.currentDate);
    const day = startOfWeek.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    startOfWeek.setDate(startOfWeek.getDate() + diff);

    for (let i = 0; i < 7; i++) {
      const d = new Date(startOfWeek);
      d.setDate(startOfWeek.getDate() + i);
      const dateStr = this.formatDate(d.getFullYear(), d.getMonth() + 1, d.getDate());
      const resaDuJour = this.reservations.filter(r => r.date === dateStr);
      const isToday = d.toDateString() === new Date().toDateString();
      days.push({ date: d, dateStr, reservations: resaDuJour, isToday, day: d.getDate() });
    }
    return days;
  }

  // ═══════════════ VUE JOUR ═══════════════
  get jourReservations(): any[] {
    const dateStr = this.formatDate(
      this.currentDate.getFullYear(),
      this.currentDate.getMonth() + 1,
      this.currentDate.getDate()
    );
    return this.reservations.filter(r => r.date === dateStr);
  }

  get currentDateStr(): string {
    return this.formatDate(
      this.currentDate.getFullYear(),
      this.currentDate.getMonth() + 1,
      this.currentDate.getDate()
    );
  }

  getResaForHour(jour: any, heure: number): any[] {
    return jour.reservations.filter((r: any) => {
      const h = parseInt(r.heureDebut?.split(':')[0]);
      return h === heure;
    });
  }

  getResaForHourDay(heure: number): any[] {
    return this.jourReservations.filter((r: any) => {
      const h = parseInt(r.heureDebut?.split(':')[0]);
      return h === heure;
    });
  }

  // ═══════════════ NAVIGATION ═══════════════
  prev() {
    if (this.vue === 'mois') {
      if (this.currentMonth === 0) { this.currentMonth = 11; this.currentYear--; }
      else this.currentMonth--;
      this.buildCalendar();
    } else if (this.vue === 'semaine') {
      this.currentDate = new Date(this.currentDate);
      this.currentDate.setDate(this.currentDate.getDate() - 7);
    } else {
      this.currentDate = new Date(this.currentDate);
      this.currentDate.setDate(this.currentDate.getDate() - 1);
    }
    this.closePanel();
  }

  next() {
    if (this.vue === 'mois') {
      if (this.currentMonth === 11) { this.currentMonth = 0; this.currentYear++; }
      else this.currentMonth++;
      this.buildCalendar();
    } else if (this.vue === 'semaine') {
      this.currentDate = new Date(this.currentDate);
      this.currentDate.setDate(this.currentDate.getDate() + 7);
    } else {
      this.currentDate = new Date(this.currentDate);
      this.currentDate.setDate(this.currentDate.getDate() + 1);
    }
    this.closePanel();
  }

  today() {
    this.currentDate = new Date();
    this.currentMonth = new Date().getMonth();
    this.currentYear = new Date().getFullYear();
    this.buildCalendar();
  }

  setVue(v: 'mois' | 'semaine' | 'jour') {
    this.vue = v;
    this.closePanel();
  }

  // ═══════════════ PANEL ═══════════════
  selectDay(day: any) {
    if (!day.day) return;
    this.selectedDate = day.date;
    this.selectedReservations = day.reservations;
    this.showPanel = true;
    this.showForm = false;
    this.nouvelleReservation.date = day.date;
  }

  selectDayFromSemaine(jour: any) {
    this.selectedDate = jour.dateStr;
    this.selectedReservations = jour.reservations;
    this.showPanel = true;
    this.showForm = false;
    this.nouvelleReservation.date = jour.dateStr;
  }

  openForm() { this.showForm = true; this.success = ''; this.error = ''; }
  closeForm() { this.showForm = false; }
  closePanel() { this.showPanel = false; this.showForm = false; }

  onSubmitReservation() {
    if (!this.nouvelleReservation.salleId) {
      this.error = 'Veuillez choisir une salle !';
      return;
    }
    this.loading = true;
    this.error = '';
    const salleId = Number(this.nouvelleReservation.salleId);

    this.reservationService.creerParAdmin(salleId, this.nouvelleReservation).subscribe({
      next: () => {
        this.success = 'Réservation créée et confirmée !';
        this.loading = false;
        this.showForm = false;
        this.loadReservations();
      },
      error: (err: any) => {
        this.error = err.error?.message || 'Erreur lors de la création';
        this.loading = false;
      }
    });
  }

  formatDate(y: number, m: number, d: number): string {
    return `${y}-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
  }

  get titreNavigation(): string {
    if (this.vue === 'mois') return `${this.moisNoms[this.currentMonth]} ${this.currentYear}`;
    if (this.vue === 'semaine') {
      const s = this.semaineJours;
      return `${s[0].date.getDate()} - ${s[6].date.getDate()} ${this.moisNoms[s[0].date.getMonth()]} ${s[0].date.getFullYear()}`;
    }
    return `${this.currentDate.getDate()} ${this.moisNoms[this.currentDate.getMonth()]} ${this.currentDate.getFullYear()}`;
  }

  logout() { this.auth.logout(); }
}