import { Component, OnInit } from '@angular/core';
import { FactureService, Facture } from '../../../core/services/facture.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProfilSharedService } from '../../../core/services/profil-shared.service';

@Component({
  selector: 'app-factures',
  templateUrl: './factures.component.html',
  styleUrls: ['./factures.component.css']
})
export class FacturesComponent implements OnInit {

  factures: Facture[] = [];
  loading = false;
  message = '';
  messageType = '';
  profil: any = null;
  today: Date = new Date();

  constructor(
    private factureService: FactureService,
    private auth: AuthService,
    private profilShared: ProfilSharedService
  ) {}

  ngOnInit() {
    this.today = new Date();
    this.profilShared.loadProfil();
    this.profilShared.profil$.subscribe(p => this.profil = p);
    this.loadFactures();
  }

  loadFactures() {
    this.loading = true;
    this.factureService.getAll().subscribe({
      next: (data) => {
        this.factures = data;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  voirPdf(id: number) {
    const url = this.factureService.getPdfUrl(id);
    window.open(url, '_blank');
  }

  envoyer(id: number) {
    this.factureService.envoyerParMail(id).subscribe({
      next: () => this.showMessage('Facture envoyée par email !', 'success'),
      error: () => this.showMessage("Erreur lors de l'envoi", 'error')
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