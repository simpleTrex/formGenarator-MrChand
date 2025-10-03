import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModelRenderComponent } from './model-render.component';

describe('ModelRenderComponent', () => {
  let component: ModelRenderComponent;
  let fixture: ComponentFixture<ModelRenderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ModelRenderComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ModelRenderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
