import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { UserService } from './user.service';

@Injectable({ providedIn: 'root' })
export class ProfilSharedService {

  private profilSubject = new BehaviorSubject<any>(null);
  profil$ = this.profilSubject.asObservable();

  constructor(private userService: UserService) {}

  loadProfil() {
    this.userService.getProfil().subscribe({
      next: (data) => this.profilSubject.next(data),
      error: () => {}
    });
  }

  getProfil() {
    return this.profilSubject.value;
  }
}