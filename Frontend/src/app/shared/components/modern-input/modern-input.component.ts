import { Component, Input, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';

@Component({
  selector: 'app-modern-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './modern-input.component.html',
  styleUrls: ['./modern-input.component.css'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModernInputComponent),
      multi: true
    }
  ]
})
export class ModernInputComponent implements ControlValueAccessor {
  @Input() label: string = '';
  @Input() type: string = 'text';
  @Input() placeholder: string = '';
  @Input() icon: string = ''; // CSS class for an icon if needed
  @Input() disabled: boolean = false;
  @Input() required: boolean = false;

  value: string = '';

  onChange: any = () => { };
  onTouch: any = () => { };

  onInput(event: any) {
    this.value = event.target.value;
    this.onChange(this.value);
    this.onTouch();
  }

  onEnterKey(event: Event): void {
    event.preventDefault();
    const input = event.target as HTMLInputElement;
    // Find the parent form element
    const form = input.closest('form');
    if (!form) return;
    // Get all focusable inputs/textareas/selects inside the form
    const focusable = Array.from(
      form.querySelectorAll<HTMLElement>('input:not([disabled]), textarea:not([disabled]), select:not([disabled])')
    );
    const currentIndex = focusable.indexOf(input);
    const nextField = focusable[currentIndex + 1];
    if (nextField) {
      // Move focus to next field
      nextField.focus();
    } else {
      // Last field — submit the form
      form.requestSubmit();
    }
  }

  writeValue(value: any): void {
    this.value = value || '';
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouch = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }
}
