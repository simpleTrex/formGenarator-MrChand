import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type CardTheme = 'light' | 'dark' | 'glass';

@Component({
  selector: 'app-modern-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './modern-card.component.html',
  styleUrls: ['./modern-card.component.css']
})
export class ModernCardComponent {
  @Input() theme: CardTheme = 'light';
  @Input() noPadding: boolean = false;
  @Input() glow: boolean = false;
}
