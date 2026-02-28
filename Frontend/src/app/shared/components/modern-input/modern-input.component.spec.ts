import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModernInputComponent } from './modern-input.component';

describe('ModernInputComponent', () => {
  let component: ModernInputComponent;
  let fixture: ComponentFixture<ModernInputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModernInputComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ModernInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
