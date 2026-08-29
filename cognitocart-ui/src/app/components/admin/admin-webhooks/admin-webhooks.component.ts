import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

import { AdminShellComponent } from '../admin-shell/admin-shell.component';

@Component({
  selector: 'app-admin-webhooks',
  standalone: true,
  imports: [CommonModule, AdminShellComponent],
  template: `
    <app-admin-shell>
      <div class="page-header" style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <h1 class="page-title" style="display:flex; align-items:center; gap:12px;">
            <div style="background:var(--grad-brand); border-radius:8px; padding:6px; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 14px rgba(99,102,241,0.3);">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
            </div>
            Webhook DLQ
          </h1>
          <p class="page-subtitle">Failed payment events — replay to ensure zero payment loss</p>
        </div>
        <button class="btn btn-secondary" (click)="load()" style="display:flex; align-items:center; gap:6px;">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" /></svg>
          Refresh
        </button>
      </div>

      <div *ngIf="webhooks.length > 0" class="alert alert-warning" style="margin-bottom:24px; display:flex; align-items:center; gap:8px;">
        <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:20px;height:20px;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3Z" /></svg>
        <span><strong>{{webhooks.length}} failed webhook events</strong> require attention. Replay to re-process payments.</span>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div *ngIf="!loading">
        <div *ngIf="webhooks.length === 0" class="empty-state" style="min-height:400px; display:flex; flex-direction:column; align-items:center; justify-content:center; color:var(--text-muted);">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:48px;height:48px;margin-bottom:16px;color:var(--text-dim);"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg>
          <div class="empty-title" style="font-size:18px; font-weight:600;">No failed webhooks!</div>
          <div class="empty-subtitle">All payment events processed successfully.</div>
        </div>

        <div class="table-wrapper" *ngIf="webhooks.length > 0" style="box-shadow:0 8px 32px rgba(0,0,0,0.4); border:1px solid rgba(255,255,255,0.05); border-radius:12px;">
          <table style="width:100%; border-collapse:collapse;">
            <thead style="background:rgba(255,255,255,0.02); border-bottom:1px solid rgba(255,255,255,0.08);">
              <tr>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Event ID</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Type</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Order</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Attempt</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Failed At</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Error</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Action</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let w of webhooks" style="border-bottom:1px solid rgba(255,255,255,0.04); transition:background 0.2s;" onmouseover="this.style.background='rgba(255,255,255,0.02)'" onmouseout="this.style.background='transparent'">
                <td style="padding:16px;"><code style="color:var(--primary); font-size:12px; background:rgba(99,102,241,0.1); padding:4px 8px; border-radius:4px;">{{w.publicId || w.razorpayEventId}}</code></td>
                <td style="padding:16px;"><span class="badge badge-yellow" style="padding:4px 10px; font-size:11px;">{{w.eventType}}</span></td>
                <td style="padding:16px;"><code style="font-size:12px; background:rgba(255,255,255,0.05); padding:4px 8px; border-radius:4px; color:var(--text-secondary);">{{w.orderPublicId || '—'}}</code></td>
                <td style="padding:16px; color:var(--text-secondary);">{{w.attemptCount || 1}}/3</td>
                <td style="padding:16px; font-size:13px; color:var(--text-secondary);">{{w.lastAttemptAt | date:'medium'}}</td>
                <td style="padding:16px; max-width:200px; font-size:12px; color:var(--danger); word-break:break-all;">{{w.lastError || '—'}}</td>
                <td style="padding:16px;">
                  <button class="btn btn-primary btn-sm" (click)="replay(w)" [disabled]="w._replaying" style="display:flex; align-items:center; gap:6px;">
                    <svg *ngIf="!w._replaying" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.347a1.125 1.125 0 0 1 0 1.972l-11.54 6.347c-.75.412-1.667-.13-1.667-.986V5.653Z" /></svg>
                    {{w._replaying ? 'Replaying...' : 'Replay'}}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </app-admin-shell>
  `,
  styles: []
})
export class AdminWebhooksComponent implements OnInit {
  webhooks: any[] = [];
  loading = true;

  constructor(private adminService: AdminService, private toast: ToastService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.adminService.getPendingWebhooks().subscribe({
      next: w => { this.webhooks = Array.isArray(w) ? w : (w.content ?? []); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  replay(webhook: any): void {
    webhook._replaying = true;
    this.adminService.replayWebhook(webhook.publicId).subscribe({
      next: () => {
        this.toast.success('Webhook replayed successfully!');
        this.webhooks = this.webhooks.filter(w => w !== webhook);
      },
      error: (e) => {
        webhook._replaying = false;
        this.toast.error(e.error?.message || 'Replay failed');
      }
    });
  }
}
