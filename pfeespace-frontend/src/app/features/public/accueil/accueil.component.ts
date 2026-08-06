import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { SalleService } from '../../../core/services/salle.service';

@Component({
  selector: 'app-accueil',
  templateUrl: './accueil.component.html',
  styleUrls: ['./accueil.component.css']
})
export class AccueilComponent implements OnInit {

  salles: any[] = [];
  currentSlide = 0;
  menuOpen = false;

  heroImages = [
    'assets/images/cothinking.jpg',
    'assets/images/zinaa.jpg',
    'assets/images/polyy.jpg'
  ];

  espacesImages = [
    { img: 'assets/images/bureau1.jpg', titre: 'Salle de Réunion', desc: 'Espace moderne pour vos réunions' },
    { img: 'assets/images/bureau2.jpg', titre: 'Grande Salle', desc: 'Pour vos événements et conférences' },
    { img: 'assets/images/bureau3.jpg', titre: 'Salle Board', desc: 'Table ovale pour vos présentations' },
    { img: 'assets/images/bureau4.jpg', titre: 'Bureau Privé', desc: 'Espace de travail avec vue panoramique' },
    { img: 'assets/images/bureau5.jpg', titre: 'Espace Créatif', desc: 'Cadre original et inspirant' }
  ];

  constructor(private router: Router, private salleService: SalleService) {}

  ngOnInit() {
    this.startSlider();
    this.salleService.getAll().subscribe({
      next: (data) => this.salles = data.filter(s => s.disponible)
    });
  }

  startSlider() {
    setInterval(() => {
      this.currentSlide = (this.currentSlide + 1) % this.heroImages.length;
    }, 4000);
  }

  goToLogin() { this.router.navigate(['/login']); }
  goToRegister() { this.router.navigate(['/register']); }
  goToReserver() { this.router.navigate(['/login']); }
}