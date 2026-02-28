import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModernButtonComponent } from '../modern-button/modern-button.component';

@Component({
  selector: 'navi-bar',
  standalone: true,
  imports: [CommonModule, ModernButtonComponent],
  templateUrl: './navi-bar.component.html',
  styleUrls: ['./navi-bar.component.css']
})
export class NaviBarComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

}
