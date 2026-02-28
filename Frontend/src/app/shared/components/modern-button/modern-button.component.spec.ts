import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModernButtonComponent } from './modern-button.component';

describe('ModernButtonComponent', () => {
  let component: ModernButtonComponent;
  let fixture: ComponentFixture<ModernButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ModernButtonComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ModernButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
