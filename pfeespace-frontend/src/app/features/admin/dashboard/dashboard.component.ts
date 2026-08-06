import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AuthService } from '../../../core/services/auth.service';
import { ProfilSharedService } from '../../../core/services/profil-shared.service';
import { ReservationService } from '../../../core/services/reservation.service';
import {
  DashboardService,
  DashboardKpis,
  MoisData,
  StatutData,
  SalleData,
  AnneeData,
  CreneauData,
  GratuitPayantData
} from '../../../core/services/dashboard.service';
import {
  ApexAxisChartSeries,
  ApexChart,
  ApexXAxis,
  ApexDataLabels,
  ApexNonAxisChartSeries,
  ApexResponsive,
  ApexTitleSubtitle,
  ApexPlotOptions
} from 'ng-apexcharts';

export type BarChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  dataLabels: ApexDataLabels;
  title: ApexTitleSubtitle;
  plotOptions: ApexPlotOptions;
};

export type PieChartOptions = {
  series: ApexNonAxisChartSeries;
  chart: ApexChart;
  labels: string[];
  responsive: ApexResponsive[];
  title: ApexTitleSubtitle;
  colors: string[];
};

// Regroupe les statuts bruts du backend en libellés lisibles
const STATUT_LABELS: { [key: string]: string } = {
  CONFIRMEE: 'Confirmée',
  EN_ATTENTE: 'En attente',
  REFUSEE: 'Annulée',
  NO_SHOW: 'Annulée',
  EN_COURS: 'En cours',
  TERMINEE: 'Terminée'
};

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  profil: any = null;
  sidebarOpen: boolean = false;
  activeTab: string = 'EN_ATTENTE';
  today: Date = new Date();
  powerbiUrl: SafeResourceUrl;

  // KPIs (cartes statistiques)
  totalReservations: number = 0;
  totalSalles: number = 0;
  totalConfirmees: number = 0;
  totalEntreprises: number = 0;
  dureeMoyenneHeures: number = 0;
  reservantsActifs: number = 0;
  tauxOccupation: number = 0;

  // Graphique 1 : réservations par mois
  moisChart: BarChartOptions = {
    series: [{ name: 'Réservations', data: [] }],
    chart: { type: 'bar', height: 300, toolbar: { show: false } },
    xaxis: { categories: [] },
    dataLabels: { enabled: false },
    plotOptions: {},
    title: { text: 'Total Réservations par Mois' }
  };

  // Graphique 2 : statut des réservations
  statutChart: PieChartOptions = {
    series: [],
    chart: { type: 'pie', height: 300 },
    labels: [],
    colors: [],
    responsive: [{ breakpoint: 480, options: { chart: { width: 250 } } }],
    title: { text: 'Statut Réservation par Statut' }
  };

  // Graphique 3 : réservations par salle
  salleChart: BarChartOptions = {
    series: [{ name: 'Réservations', data: [] }],
    chart: { type: 'bar', height: 300, toolbar: { show: false } },
    xaxis: { categories: [] },
    dataLabels: { enabled: false },
    plotOptions: {},
    title: { text: 'Total Réservations par Salle' }
  };

  // Graphique 4 (nouveau) : réservations par année
  anneeChart: BarChartOptions = {
    series: [{ name: 'Total Réservations', data: [] }],
    chart: { type: 'bar', height: 300, toolbar: { show: false } },
    xaxis: { categories: [] },
    plotOptions: { bar: { horizontal: true } },
    dataLabels: { enabled: false },
    title: { text: 'Total Réservations par Année' }
  };

  // Graphique 5 (nouveau) : créneaux horaires les plus réservés
  creneauChart: BarChartOptions = {
    series: [{ name: 'Nb Réservations', data: [] }],
    chart: { type: 'bar', height: 300, toolbar: { show: false } },
    xaxis: { categories: [] },
    dataLabels: { enabled: false },
    plotOptions: {},
    title: { text: 'Nb Réservations par Créneau Horaire' }
  };

  // Graphique 6 (nouveau) : gratuit vs payant
  gratuitPayantChart: PieChartOptions = {
    series: [],
    chart: { type: 'pie', height: 300 },
    labels: ['Gratuit', 'Payant'],
    colors: ['#9ca3af', '#111827'],
    responsive: [{ breakpoint: 480, options: { chart: { width: 250 } } }],
    title: { text: 'Gratuit vs Payant' }
  };

  constructor(
    private auth: AuthService,
    private profilShared: ProfilSharedService,
    private reservationService: ReservationService,
    private dashboardService: DashboardService,
    private sanitizer: DomSanitizer
  ) {
    this.powerbiUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
      'https://app.powerbi.com/reportEmbed?reportId=93fe9c3e-8ff8-4e7e-a78b-ed43ed9fa572&autoAuth=true&ctid=604f1a96-cbe8-43f8-abbf-f8eaf5d85730&filterPaneEnabled=false'
    );
  }

  ngOnInit(): void {
    this.profilShared.loadProfil();
    this.profilShared.profil$.subscribe((p: any) => this.profil = p);

    this.loadConfirmees();
    this.loadKpis();
    this.loadReservationsParMois();
    this.loadReservationsParStatut();
    this.loadReservationsParSalle();
    this.loadReservationsParAnnee();
    this.loadCreneauxHoraires();
    this.loadGratuitVsPayant();
  }

  private loadConfirmees(): void {
    this.reservationService.getAll().subscribe({
      next: (data: any[]) => {
        this.totalConfirmees = data.filter((r: any) => r.statut === 'CONFIRMEE').length;
      },
      error: (err: any) => console.error('Erreur chargement réservations', err)
    });
  }

  private loadKpis(): void {
    this.dashboardService.getKpis().subscribe({
      next: (data: DashboardKpis) => {
        this.totalReservations = data.totalReservations;
        this.totalSalles = data.totalSalles;
        this.totalEntreprises = data.totalEntreprises;
        this.dureeMoyenneHeures = data.dureeMoyenneHeures;
        this.reservantsActifs = data.reservantsActifs;
        this.tauxOccupation = data.tauxOccupation;
      },
      error: (err: any) => console.error('Erreur chargement KPIs', err)
    });
  }

  private loadReservationsParMois(): void {
    this.dashboardService.getReservationsParMois().subscribe({
      next: (data: MoisData[]) => {
        this.moisChart = {
          ...this.moisChart,
          series: [{ name: 'Réservations', data: data.map((d: MoisData) => d.total) }],
          xaxis: { categories: data.map((d: MoisData) => d.mois) }
        };
      },
      error: (err: any) => console.error('Erreur chargement réservations par mois', err)
    });
  }

  private loadReservationsParStatut(): void {
    this.dashboardService.getReservationsParStatut().subscribe({
      next: (data: StatutData[]) => {
        const groupes: { [label: string]: number } = {};
        data.forEach((d: StatutData) => {
          const label = STATUT_LABELS[d.statut] || d.statut;
          groupes[label] = (groupes[label] || 0) + d.total;
        });

        this.statutChart = {
          ...this.statutChart,
          series: Object.values(groupes),
          labels: Object.keys(groupes)
        };
      },
      error: (err: any) => console.error('Erreur chargement réservations par statut', err)
    });
  }

  private loadReservationsParSalle(): void {
    this.dashboardService.getReservationsParSalle().subscribe({
      next: (data: SalleData[]) => {
        this.salleChart = {
          ...this.salleChart,
          series: [{ name: 'Réservations', data: data.map((d: SalleData) => d.total) }],
          xaxis: { categories: data.map((d: SalleData) => d.salle) }
        };
      },
      error: (err: any) => console.error('Erreur chargement réservations par salle', err)
    });
  }

  // ===== Nouveau : réservations par année =====
  private loadReservationsParAnnee(): void {
    this.dashboardService.getReservationsParAnnee().subscribe({
      next: (data: AnneeData[]) => {
        this.anneeChart = {
          ...this.anneeChart,
          series: [{ name: 'Total Réservations', data: data.map((d: AnneeData) => d.total) }],
          xaxis: { categories: data.map((d: AnneeData) => d.annee.toString()) }
        };
      },
      error: (err: any) => console.error('Erreur chargement réservations par année', err)
    });
  }

  // ===== Nouveau : créneaux horaires =====
  private loadCreneauxHoraires(): void {
    this.dashboardService.getCreneauxHoraires().subscribe({
      next: (data: CreneauData[]) => {
        this.creneauChart = {
          ...this.creneauChart,
          series: [{ name: 'Nb Réservations', data: data.map((d: CreneauData) => d.total) }],
          xaxis: { categories: data.map((d: CreneauData) => d.creneau) }
        };
      },
      error: (err: any) => console.error('Erreur chargement créneaux horaires', err)
    });
  }

  // ===== Nouveau : gratuit vs payant =====
  private loadGratuitVsPayant(): void {
    this.dashboardService.getGratuitVsPayant().subscribe({
      next: (data: GratuitPayantData) => {
        this.gratuitPayantChart = {
          ...this.gratuitPayantChart,
          series: [data.nbGratuit, data.nbPayant]
        };
      },
      error: (err: any) => console.error('Erreur chargement gratuit vs payant', err)
    });
  }

  setTab(tab: string): void { this.activeTab = tab; }
  logout(): void { this.auth.logout(); }
}