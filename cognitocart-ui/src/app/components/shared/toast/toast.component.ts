import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '../../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      <div
        *ngFor="let toast of toastService.toasts$ | async"
        class="toast toast-{{toast.type}}"
        (click)="toastService.dismiss(toast.id)"
        style="cursor:pointer;"
      >
        <span>{{toast.icon}}</span>
        <span style="flex:1;">{{toast.message}}</span>
        <span style="opacity:0.6; font-size:12px;">✕</span>
      </div>
    </div>
  `
})
export class ToastComponent {
  constructor(public toastService: ToastService) {}
}
