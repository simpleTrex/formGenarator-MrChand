import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModernCardComponent } from './modern-card.component';

describe('ModernCardComponent', () => {
  let component: ModernCardComponent;
  let fixture: ComponentFixture<ModernCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModernCardComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ModernCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
