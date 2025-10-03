import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NaviDataComponent } from './navi-data.component';

describe('NaviDataComponent', () => {
  let component: NaviDataComponent;
  let fixture: ComponentFixture<NaviDataComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ NaviDataComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(NaviDataComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
