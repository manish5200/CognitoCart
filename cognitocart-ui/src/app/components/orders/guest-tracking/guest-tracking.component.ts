import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../services/order.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-guest-tracking',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page" style="display:flex; flex-direction:column; align-items:center; justify-content:center; min-height:70vh;">
      <div class="card" style="width: 100%; max-width: 600px; padding: 32px; text-align: center;">
        <h1 style="margin-bottom:8px; font-size:28px;">Track Your Order &#128666;</h1>
        <p style="color:var(--text-muted); margin-bottom:32px;">Enter your Order Number or Tracking ID to see the latest updates.</p>

        <div style="display:flex; gap:12px; margin-bottom:32px;">
          <input type="text" [(ngModel)]="trackingId" class="form-input" placeholder="e.g. ORD-2026..." style="flex:1;" (keyup.enter)="trackOrder()" />
          <button class="btn btn-primary" (click)="trackOrder()" [disabled]="loading">
            <span *ngIf="loading" class="spinner spinner-sm" style="margin-right:8px;"></span>
            {{loading ? 'Searching...' : 'Track'}}
          </button>
        </div>

        <div *ngIf="order" class="tracking-result" style="text-align:left; border-top:1px solid var(--border); padding-top:24px;">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:24px;">
            <div>
              <div style="font-size:12px; color:var(--text-muted);">Order Number</div>
              <div style="font-size:18px; font-weight:700; color:#fff;">{{order.orderNumber}}</div>
            </div>
            <div>
              <span class="badge badge-lg" [class]="'status-' + order.status">{{order.status}}</span>
            </div>
          </div>

          <h4 style="margin-bottom:16px;">Timeline</h4>
          <div class="timeline" style="margin-left:8px;">
            <div *ngFor="let event of timeline" class="timeline-item" style="padding-bottom:20px;">
              <div class="timeline-dot" style="width:12px; height:12px; background:var(--primary); border-radius:50%; margin-left:-6px;"></div>
              <div class="timeline-content" style="padding-left:16px;">
                <div style="font-weight:600; color:#fff; font-size:14px;">{{event.status || event.event}}</div>
                <div style="font-size:12px; color:var(--text-muted);">{{event.timestamp | date:'medium'}}</div>
                <div *ngIf="event.comment" style="font-size:13px; color:var(--text-dim); margin-top:4px;">{{event.comment}}</div>
              </div>
            </div>
            <div *ngIf="timeline.length === 0" style="color:var(--text-dim); font-size:14px;">No tracking information available yet.</div>
          </div>

          <div *ngIf="order.trackingNumber" style="margin-top:24px; padding:16px; background:rgba(99,102,241,0.1); border-radius:8px; border:1px solid rgba(99,102,241,0.2);">
            <div style="font-size:12px; color:#818cf8; font-weight:600; text-transform:uppercase; letter-spacing:1px; margin-bottom:4px;">Courier Tracking</div>
            <div style="display:flex; justify-content:space-between; align-items:center;">
              <div>
                <span style="color:#fff; font-weight:600; margin-right:8px;">{{order.courier}}</span>
                <span style="color:var(--text-muted);">{{order.trackingNumber}}</span>
              </div>
              <a *ngIf="order.trackingUrl" [href]="order.trackingUrl" target="_blank" class="btn btn-sm" style="background:var(--primary); color:#fff; border:none; border-radius:4px; padding:6px 12px; text-decoration:none; font-size:12px; font-weight:600;">Track on Courier Site</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .timeline { position: relative; border-left: 2px solid rgba(255,255,255,0.1); }
    .timeline-item { position: relative; }
    .timeline-dot { position: absolute; left: 0; top: 4px; box-shadow: 0 0 0 4px var(--bg-card); }
  `]
})
export class GuestTrackingComponent {
  trackingId = '';
  loading = false;
  order: any = null;
  timeline: any[] = [];

  constructor(private orderService: OrderService, private toast: ToastService) {}

  trackOrder(): void {
    if (!this.trackingId.trim()) return;
    this.loading = true;
    this.order = null;
    this.timeline = [];

    // Assuming backend has a public GET /api/v1/tracking/{id} for tracking.
    this.orderService.getTimeline(this.trackingId).subscribe({
      next: (res) => {
        // Mock order details for now if we only get timeline array
        // We'll see how backend tracking endpoint behaves
        if (Array.isArray(res)) {
          this.timeline = res;
          this.order = { 
            orderNumber: this.trackingId, 
            status: res.length > 0 ? res[0].status : 'UNKNOWN',
            // Will need actual backend response to populate these
            trackingNumber: null,
            trackingUrl: null,
            courier: null
          };
        } else {
          this.order = res.order || res;
          this.timeline = res.timeline || [];
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Order not found or access denied. Please check your tracking ID.');
      }
    });
  }
}
