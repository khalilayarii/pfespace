import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PowerbiComponent } from './powerbi.component';

describe('PowerbiComponent', () => {
  let component: PowerbiComponent;
  let fixture: ComponentFixture<PowerbiComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PowerbiComponent]
    });
    fixture = TestBed.createComponent(PowerbiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
