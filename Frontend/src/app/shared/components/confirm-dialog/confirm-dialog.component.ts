import { CommonModule } from '@angular/common';
import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { ModernButtonComponent, ButtonVariant } from '../modern-button/modern-button.component';
import { ModernCardComponent, CardTheme } from '../modern-card/modern-card.component';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, ModernCardComponent, ModernButtonComponent],
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.css']
})
export class ConfirmDialogComponent {
  @Input() title = 'Confirm';
  @Input() message = 'Are you sure?';

  @Input() theme: CardTheme = 'dark';

  @Input() confirmText = 'Yes';
  @Input() cancelText = 'No';

  @Input() confirmVariant: ButtonVariant = 'primary';
  @Input() cancelVariant: ButtonVariant = 'outline-light';

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  onConfirm(): void {
    this.confirm.emit();
  }

  onCancel(): void {
    this.cancel.emit();
  }

  onBackdropClick(): void {
    this.onCancel();
  }

  @HostListener('document:keydown.escape')
  onEsc(): void {
    this.onCancel();
  }
}
