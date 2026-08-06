import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {

  nom: string = '';
  prenom: string = '';
  email: string = '';
  telephone: string = '';
  mdp: string = '';
  confirmMdp: string = '';
  acceptTerms: boolean = false;
  error: string = '';
  success: string = '';
  loading: boolean = false;
  showPassword: boolean = false;
  showConfirm: boolean = false;

  constructor(private auth: AuthService, private router: Router) {}

  togglePassword() { this.showPassword = !this.showPassword; }
  toggleConfirm()  { this.showConfirm  = !this.showConfirm;  }

  onRegister() {
    this.error = '';
    this.success = '';

    if (this.mdp !== this.confirmMdp) {
      this.error = 'Les mots de passe ne correspondent pas !';
      return;
    }

    if (!this.acceptTerms) {
      this.error = 'Veuillez accepter les termes et conditions !';
      return;
    }

    this.loading = true;

    const data = {
      nom:       this.nom,
      prenom:    this.prenom,
      email:     this.email,
      telephone: this.telephone,
      mdp:       this.mdp
    };

    this.auth.register(data).subscribe({
      next: () => {
        this.success = 'Compte créé avec succès ! Redirection...';
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err: any) => {
        this.error = err.error?.message || 'Erreur lors de l\'inscription';
        this.loading = false;
      }
    });
  }
}