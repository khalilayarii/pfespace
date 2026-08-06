import { Component, OnInit, HostListener } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ReservationService, Reservation } from '../../../core/services/reservation.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProfilSharedService } from '../../../core/services/profil-shared.service';
import { EntrepriseService, Entreprise } from '../../../core/services/entreprise.service';

@Component({
  selector: 'app-reservation',
  templateUrl: './reservation.component.html',
  styleUrls: ['./reservation.component.css']
})
export class ReservationComponent implements OnInit {

  salleId: number = 0;
  error: string = '';
  loading: boolean = false;
  checkingDisponibilite: boolean = false;
  checkingFiscal: boolean = false;
  profil: any = null;
  userMenuOpen: boolean = false;
  currentStep: number = 1;
  showSuccess: boolean = false;
  today: string = new Date().toISOString().split('T')[0];
  fieldErrors: { [key: string]: string } = {};

  // Champs Nom / Prénom séparés — utilisés uniquement pour Étudiant / Freelance,
  // puis fusionnés dans reservation.nomComplet avant envoi au backend
  nomInput: string = '';
  prenomInput: string = '';

  // Vérification fiscale
  entrepriseTrouvee: Entreprise | null = null;
  fiscalVerifie: boolean = false;
  numFiscalInput: string = '';

  // Autocomplete société
  societeDropdownOpen: boolean = false;

  readonly toutesLesSocietes: string[] = [
    'Medianet', 'Saphir Consult', 'Express FM', 'Quallipro',
    'Resrv', 'Digihost', 'BigDeal', 'Bmoov', 'Webcoming',
    'Publicket', 'Co-chef', 'Cothinking', 'Bookpicups',
    'Atslik', 'Wesearch', 'Openwork'
  ];

  reservation: Reservation = {
    typeUtilisateur: '',
    natureManifestation: '',
    date: '',
    heureDebut: '',
    heureFin: '',
    mail: '',
    nomComplet: '',
    societe: '',
    telephone: '',
    description: ''
  };

  constructor(
    private reservationService: ReservationService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router,
    private profilShared: ProfilSharedService,
    private entrepriseService: EntrepriseService
  ) {}

  ngOnInit(): void {
    this.salleId = Number(this.route.snapshot.paramMap.get('id'));
    this.profilShared.loadProfil();
    this.profilShared.profil$.subscribe(p => this.profil = p);
  }

  toggleUserMenu(): void {
    this.userMenuOpen = !this.userMenuOpen;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.user-menu')) {
      this.userMenuOpen = false;
    }
  }

  // ── Autocomplete société ──
  societesFiltrees(): string[] {
    const q = (this.reservation.societe || '').toLowerCase();
    if (!q) return this.toutesLesSocietes;
    return this.toutesLesSocietes.filter(s => s.toLowerCase().includes(q));
  }

  onSocieteInput(): void {
    this.societeDropdownOpen = true;
  }

  toggleSocieteDropdown(): void {
    this.societeDropdownOpen = !this.societeDropdownOpen;
  }

  selectSociete(s: string): void {
    this.reservation.societe = s;
    this.societeDropdownOpen = false;
  }

  // ── Fiscal (uniquement pour le type SOCIETE) ──
  verifierFiscal(): void {
    if (!this.numFiscalInput.trim()) {
      this.fieldErrors['numFiscal'] = 'Entrez un numéro fiscal';
      return;
    }

    this.checkingFiscal = true;
    this.fiscalVerifie = false;
    this.entrepriseTrouvee = null;
    this.fieldErrors['numFiscal'] = '';

    this.entrepriseService.verifierNumFiscal(this.numFiscalInput.trim()).subscribe({
      next: (entreprise) => {
        this.checkingFiscal = false;
        this.fiscalVerifie = true;
        this.entrepriseTrouvee = entreprise;
        this.reservation.societe = entreprise.nom;
        // Si l'entreprise est membre Startup Village, on la traite comme INTERNE (gratuit)
        this.reservation.typeUtilisateur = entreprise.estMembre ? 'INTERNE' : 'SOCIETE';
      },
      error: () => {
        this.checkingFiscal = false;
        this.fiscalVerifie = true;
        this.entrepriseTrouvee = null;
        this.reservation.typeUtilisateur = 'SOCIETE';
      }
    });
  }

  resetFiscal(): void {
    this.numFiscalInput = '';
    this.fiscalVerifie = false;
    this.entrepriseTrouvee = null;
    this.reservation.societe = '';
    this.reservation.typeUtilisateur = 'SOCIETE';
  }

  // ── Utilitaires ──
  getDureeHeures(): number {
    if (!this.reservation.heureDebut || !this.reservation.heureFin) return 0;
    const [h1, m1] = this.reservation.heureDebut.split(':').map(Number);
    const [h2, m2] = this.reservation.heureFin.split(':').map(Number);
    return ((h2 * 60 + m2) - (h1 * 60 + m1)) / 60;
  }

  getDureeFormatted(): string {
    const total = this.getDureeHeures();
    const h = Math.floor(total);
    const m = Math.round((total - h) * 60);
    return m > 0 ? `${h}h${m.toString().padStart(2, '0')}` : `${h}h`;
  }

  getMontant(): string {
    if (this.entrepriseTrouvee?.estMembre) return 'GRATUIT';
    return '—';
  }

  isIndividuel(): boolean {
    return this.reservation.typeUtilisateur === 'ETUDIANT' || this.reservation.typeUtilisateur === 'FREELANCE';
  }

  // ── Navigation steps ──
  nextStep(): void {
    this.fieldErrors = {};
    this.error = '';

    if (this.currentStep === 1) {
      if (!this.reservation.typeUtilisateur)
        this.fieldErrors['typeUtilisateur'] = 'Ce champ est obligatoire';

      if (!this.reservation.natureManifestation)
        this.fieldErrors['natureManifestation'] = 'Ce champ est obligatoire';

      if (!this.reservation.date) {
        this.fieldErrors['date'] = 'La date est obligatoire';
      } else {
        const today = new Date(new Date().toDateString());
        const selectedDate = new Date(this.reservation.date);
        if (selectedDate < today)
          this.fieldErrors['date'] = 'La date ne peut pas être dans le passé';
      }

      if (!this.reservation.heureDebut)
        this.fieldErrors['heureDebut'] = "L'heure de début est obligatoire";

      if (!this.reservation.heureFin)
        this.fieldErrors['heureFin'] = "L'heure de fin est obligatoire";

      if (this.reservation.heureDebut && this.reservation.heureFin &&
          this.reservation.heureDebut >= this.reservation.heureFin) {
        this.fieldErrors['heureFin'] = "L'heure de fin doit être après l'heure de début";
      }

      if (Object.keys(this.fieldErrors).length > 0) return;

      this.checkingDisponibilite = true;
      this.reservationService.verifierDisponibilite(
        this.salleId,
        this.reservation.date,
        this.reservation.heureDebut,
        this.reservation.heureFin
      ).subscribe({
        next: (disponible: boolean) => {
          this.checkingDisponibilite = false;
          if (disponible) {
            this.currentStep++;
          } else {
            this.fieldErrors['heureFin'] = 'Cette salle est déjà réservée sur ce créneau.';
          }
        },
        error: () => {
          this.checkingDisponibilite = false;
          this.currentStep++;
        }
      });
      return;
    }

    if (this.currentStep === 2) {
      const individuel = this.isIndividuel();

      if (individuel) {
        if (!this.nomInput)
          this.fieldErrors['nomInput'] = 'Le nom est obligatoire';
        if (!this.prenomInput)
          this.fieldErrors['prenomInput'] = 'Le prénom est obligatoire';
      } else {
        if (!this.reservation.nomComplet)
          this.fieldErrors['nomComplet'] = 'Le nom complet est obligatoire';
      }

      if (this.reservation.typeUtilisateur === 'SOCIETE' && !this.reservation.societe)
        this.fieldErrors['societe'] = 'Le nom de la société est obligatoire';

      if (!this.reservation.mail) {
        this.fieldErrors['mail'] = "L'email est obligatoire";
      } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.reservation.mail)) {
        this.fieldErrors['mail'] = 'Format email invalide';
      }

      if (!this.reservation.telephone) {
        this.fieldErrors['telephone'] = 'Le téléphone est obligatoire';
      } else if (!/^\d{8}$/.test(this.reservation.telephone)) {
        this.fieldErrors['telephone'] = 'Le téléphone doit contenir 8 chiffres';
      }

      if (Object.keys(this.fieldErrors).length > 0) return;

      if (individuel) {
        this.reservation.nomComplet = `${this.prenomInput} ${this.nomInput}`.trim();
      }
    }

    this.currentStep++;
  }

  prevStep(): void {
    this.error = '';
    this.fieldErrors = {};
    this.currentStep--;
  }

  onSubmit(): void {
    this.error = '';
    this.loading = true;

    this.reservationService.create(this.salleId, this.reservation).subscribe({
      next: () => {
        this.loading = false;
        this.showSuccess = true;
        setTimeout(() => {
          this.router.navigate(['/reservations'], { queryParams: { statut: 'EN_ATTENTE' } });
        }, 3000);
      },
      error: (err: any) => {
        this.error = err.error?.message || err.error || 'Erreur lors de la réservation';
        this.loading = false;
      }
    });
  }

  logout(): void {
    this.auth.logout();
  }
}