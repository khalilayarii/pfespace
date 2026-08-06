import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AvisSalleComponent } from './avis-salle.component';

describe('AvisSalleComponent', () => {
  let component: AvisSalleComponent;
  let fixture: ComponentFixture<AvisSalleComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AvisSalleComponent]
    });
    fixture = TestBed.createComponent(AvisSalleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
