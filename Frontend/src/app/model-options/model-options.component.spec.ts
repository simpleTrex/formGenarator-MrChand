import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModelOptionsComponent } from './model-options.component';

describe('ModelOptionsComponent', () => {
  let component: ModelOptionsComponent;
  let fixture: ComponentFixture<ModelOptionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ModelOptionsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ModelOptionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
