import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { AuthService } from '../../../core/services/auth.service';
import { ProfilSharedService } from '../../../core/services/profil-shared.service';

@Component({
  selector: 'app-powerbi',
  templateUrl: './powerbi.component.html',
  styleUrls: ['./powerbi.component.css']
})
export class PowerbiComponent implements OnInit {

  profil: any = null;
  powerbiUrl: SafeResourceUrl;

  constructor(
    private sanitizer: DomSanitizer,
    private auth: AuthService,
    private profilShared: ProfilSharedService
  ) {
    this.powerbiUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
      'https://app.powerbi.com/reportEmbed?reportId=93fe9c3e-8ff8-4e7e-a78b-ed43ed9fa572&autoAuth=true&ctid=604f1a96-cbe8-43f8-abbf-f8eaf5d85730&filterPaneEnabled=false'
    );
  }

  ngOnInit(): void {
    this.profilShared.loadProfil();
    this.profilShared.profil$.subscribe((p: any) => this.profil = p);
  }

  logout(): void {
    this.auth.logout();
  }
}