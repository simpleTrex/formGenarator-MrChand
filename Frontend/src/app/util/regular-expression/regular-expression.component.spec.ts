import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegularExpressionComponent } from './regular-expression.component';

describe('RegularExpressionComponent', () => {
  let component: RegularExpressionComponent;
  let fixture: ComponentFixture<RegularExpressionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RegularExpressionComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RegularExpressionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
