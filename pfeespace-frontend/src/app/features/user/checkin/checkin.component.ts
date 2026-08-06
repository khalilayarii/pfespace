import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CheckinService, CheckinResponse } from '../../../core/services/checkin.service';

@Component({
  selector: 'app-checkin',
  templateUrl: './checkin.component.html',
  styleUrls: ['./checkin.component.css']
})
export class CheckinComponent implements OnInit {
  loading = true;
  succes = false;
  messageErreur = '';
  reservation: CheckinResponse | null = null;

  constructor(
    private route: ActivatedRoute,
    private checkinService: CheckinService
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('token');
    if (!token) {
      this.loading = false;
      this.messageErreur = 'Lien invalide.';
      return;
    }

    this.checkinService.checkin(token).subscribe({
      next: (res) => {
        this.loading = false;
        this.succes = true;
        this.reservation = res;
      },
      error: (err) => {
        this.loading = false;
        this.succes = false;
        this.messageErreur = err.error?.message || err.error || 'Erreur lors du check-in.';
      }
    });
  }
}