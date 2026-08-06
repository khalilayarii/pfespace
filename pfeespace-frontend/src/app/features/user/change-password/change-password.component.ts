import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-change-password',
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.css']
})
export class ChangePasswordComponent {

  ancienMotDePasse: string = '';
  nouveauMotDePasse: string = '';
  confirmMotDePasse: string = '';
  error: string = '';
  success: string = '';
  loading: boolean = false;
  showAncien: boolean = false;
  showNouveau: boolean = false;
  showConfirm: boolean = false;

  constructor(private userService: UserService, private router: Router) {}

  toggleAncien()  { this.showAncien  = !this.showAncien;  }
  toggleNouveau() { this.showNouveau = !this.showNouveau; }
  toggleConfirm() { this.showConfirm = !this.showConfirm; }

  onChangePassword() {
    this.error = '';
    this.success = '';

    if (!this.ancienMotDePasse || !this.nouveauMotDePasse || !this.confirmMotDePasse) {
      this.error = 'Veuillez remplir tous les champs !';
      return;
    }

    if (this.nouveauMotDePasse !== this.confirmMotDePasse) {
      this.error = 'Les nouveaux mots de passe ne correspondent pas !';
      return;
    }

    if (this.nouveauMotDePasse.length < 6) {
      this.error = 'Le mot de passe doit contenir au moins 6 caractères !';
      return;
    }

    this.loading = true;

    const data = {
      ancienMotDePasse: this.ancienMotDePasse,
      nouveauMotDePasse: this.nouveauMotDePasse
    };

    this.userService.changePassword(data).subscribe({
      next: () => {
        this.success = 'Mot de passe modifié avec succès !';
        this.loading = false;
        setTimeout(() => this.router.navigate(['/profil']), 2000);
      },
      error: (err: any) => {
        // Status 200 = succès même si Angular croit que c'est une erreur
        if (err.status === 200) {
          this.success = 'Mot de passe modifié avec succès !';
          this.loading = false;
          setTimeout(() => this.router.navigate(['/profil']), 2000);
        } else {
          this.error = err.error || 'Ancien mot de passe incorrect !';
          this.loading = false;
        }
      }
    });
  }
}