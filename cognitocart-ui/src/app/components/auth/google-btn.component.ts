import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-google-btn',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button 
      type="button" 
      class="google-oauth-btn" 
      (click)="onClick()" 
      [disabled]="disabled || loading"
      [class.loading]="loading">
      <span *ngIf="!loading" class="google-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
          <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
          <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
          <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
        </svg>
      </span>
      <span *ngIf="loading" class="spinner spinner-sm" style="margin-right: 12px; border-top-color: var(--text-primary);"></span>
      <span>{{ loading ? loadingText : text }}</span>
    </button>
  `,
  styles: [`
    .google-oauth-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 54px;
      background: var(--bg-surface);
      border: 1px solid var(--border-strong);
      border-radius: 12px;
      color: var(--text-primary);
      font-size: 15px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;
      margin-bottom: 24px;
    }
    
    .google-oauth-btn:hover:not(:disabled) {
      background: var(--bg-elevated);
      border-color: var(--text-muted);
    }
    
    .google-oauth-btn:focus-visible {
      outline: none;
      box-shadow: 0 0 0 3px var(--primary-glow);
    }
    
    .google-oauth-btn:active:not(:disabled) {
      transform: scale(0.98);
    }
    
    .google-oauth-btn:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }
    
    .google-oauth-btn.loading {
      background: var(--bg-surface);
      border-color: var(--border-default);
    }

    .google-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 12px;
    }
  `]
})
export class GoogleBtnComponent {
  @Input() text: string = 'Continue with Google';
  @Input() loadingText: string = 'Connecting to Google...';
  @Input() loading: boolean = false;
  @Input() disabled: boolean = false;
  @Output() action = new EventEmitter<void>();

  onClick() {
    this.action.emit();
  }
}
