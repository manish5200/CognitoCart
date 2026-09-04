import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

import { AdminShellComponent } from '../admin-shell/admin-shell.component';

@Component({
  selector: 'app-admin-returns',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminShellComponent],
  template: `
    <app-admin-shell>
      <div class="page-header" style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <h1 class="page-title" style="display:flex; align-items:center; gap:12px;">
            <div style="background:var(--grad-brand); border-radius:8px; padding:6px; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 14px rgba(99,102,241,0.3);">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 7v6h6"/><path d="M21 17v-6h-6"/><path d="M21 11.2A9 9 0 0 0 4.1 7L3 8"/><path d="M3 12.8A9 9 0 0 0 19.9 17l1-1"/></svg>
            </div>
            Return Queue
          </h1>
          <p class="page-subtitle">Manage all pending return, replacement, and exchange requests</p>
        </div>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div *ngIf="!loading">
        <div *ngIf="returns.length === 0" class="empty-state" style="min-height:400px; display:flex; flex-direction:column; align-items:center; justify-content:center; color:var(--text-muted);">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:48px;height:48px;margin-bottom:16px;color:var(--text-dim);"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg>
          <div class="empty-title" style="font-size:18px; font-weight:600;">No pending returns!</div>
          <div class="empty-subtitle">All return requests have been processed.</div>
        </div>

        <div *ngFor="let r of returns" class="return-card card" style="margin-bottom:16px;">
          <div class="card-body">
            <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:16px;">
              <div>
                <div style="font-size:18px; font-weight:700; color:#fff; margin-bottom:8px;">
                  {{r.orderNumber || r.publicId}}
                </div>
                <div style="display:flex; gap:12px; flex-wrap:wrap;">
                  <span class="badge" [class]="getTypeBadge(r.status || r.requestType)">
                    {{r.status || r.requestType}}
                  </span>
                  <span style="font-size:13px; color:var(--text-muted);">Customer: {{r.customerName || '&#8212;'}}</span>
                  <span style="font-size:13px; color:var(--text-muted);">Date: {{r.requestedAt | date:'mediumDate'}}</span>
                </div>
              </div>
              <span style="font-size:1.2rem; font-weight:700; color:var(--primary);">&#8377;{{r.totalAmount | number:'1.0-0'}}</span>
            </div>

            <!-- Items -->
            <div *ngIf="r.items?.length" style="margin:16px 0; padding:12px; background:rgba(255,255,255,0.02); border-radius:var(--radius);">
              <div *ngFor="let item of r.items" style="font-size:13px; color:var(--text-muted); display:flex; align-items:center; gap:6px;">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" /></svg>
                {{item.productName}} Ã&#8212; {{item.quantity}}
              </div>
            </div>

            <div *ngIf="r.reason" style="margin-bottom:12px;">
              <span style="font-size:13px; color:var(--text-muted);">Reason: </span>
              <span style="font-size:13px; color:var(--text);">{{r.reason}}</span>
            </div>
            <div *ngIf="r.description" style="margin-bottom:16px; font-size:14px; color:var(--text-muted); font-style:italic;">
              "{{r.description}}"
            </div>

            <!-- Return Images -->
            <div *ngIf="r.mediaGallery?.length" style="display:flex; gap:8px; margin-bottom:16px;">
              <img *ngFor="let media of r.mediaGallery" [src]="media.mediaUrl" style="width:72px; height:72px; object-fit:cover; border-radius:8px; border:1px solid var(--border);" />
            </div>

            <!-- Actions -->
            <div style="display:flex; gap:12px; flex-wrap:wrap;">
              <button *ngIf="isReturn(r)" class="btn btn-success" (click)="approve(r)" style="display:flex; align-items:center; gap:6px;">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" /></svg>
                Approve Return + Refund
              </button>
              <button *ngIf="isReplacement(r)" class="btn btn-success" (click)="approveReplacement(r)" style="display:flex; align-items:center; gap:6px;">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" /></svg>
                Approve Replacement
              </button>

              <!-- Reject with comment -->
              <div style="display:flex; gap:8px; align-items:center;">
                <input type="text" [(ngModel)]="rejectComments[r.publicId || r.id]"
                  class="form-input" placeholder="Rejection reason..." style="width:200px;" />
                <button class="btn btn-danger" (click)="reject(r)" style="display:flex; align-items:center; gap:6px;">
                  <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" /></svg>
                  Reject
                </button>
              </div>

              <button (click)="downloadInvoice(r)" class="btn btn-ghost btn-sm" style="display:flex; align-items:center; gap:6px;">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><line x1="16" x2="8" y1="13" y2="13"/><line x1="16" x2="8" y1="17" y2="17"/><line x1="10" x2="8" y1="9" y2="9"/></svg>
                Invoice
              </button>
            </div>
          </div>
        </div>
      </div>
    </app-admin-shell>
  `,
  styles: []
})
export class AdminReturnsComponent implements OnInit {
  returns: any[] = [];
  loading = true;
  rejectComments: Record<string, string> = {};

  constructor(private adminService: AdminService, private toast: ToastService) {}

  ngOnInit(): void {
    this.adminService.getPendingReturns().subscribe({
      next: (r) => { this.returns = Array.isArray(r) ? r : (r.content ?? []); this.loading = false; },
      error: () => { this.loading = false; this.toast.error('Failed to load return queue'); }
    });
  }

  isReturn(r: any): boolean { return ['RETURN_REQUESTED', 'RETURN', 'EXCHANGE_REQUESTED', 'EXCHANGE'].includes(r.status || r.requestType); }
  isReplacement(r: any): boolean { return ['REPLACEMENT_REQUESTED', 'REPLACEMENT'].includes(r.status || r.requestType); }

  downloadInvoice(r: any): void {
    const id = r.publicId || r.orderNumber;
    this.adminService.downloadInvoice(id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Invoice-${id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.toast.error('Failed to download invoice')
    });
  }

  getTypeBadge(type: string): string {
    const map: Record<string, string> = {
      RETURN_REQUESTED: 'badge-yellow', REPLACEMENT_REQUESTED: 'badge-yellow',
      RETURN: 'badge-yellow', REPLACEMENT: 'badge-yellow',
      APPROVED: 'badge-green', REJECTED: 'badge-red'
    };
    return map[type] || 'badge-gray';
  }

  approve(r: any): void {
    const id = r.orderPublicId || r.orderNumber || r.publicId || r.id;
    if (!window.confirm(`Are you sure you want to approve return for order ${id}? This will initiate a Razorpay refund.`)) return;
    this.adminService.approveReturn(id).subscribe({
      next: () => { this.toast.success('Return approved! Razorpay refund initiated.'); this.remove(r); },
      error: (e) => this.toast.error(e.error?.message || 'Failed to approve')
    });
  }

  approveReplacement(r: any): void {
    const id = r.orderPublicId || r.orderNumber || r.publicId || r.id;
    if (!window.confirm(`Are you sure you want to approve replacement for order ${id}? This will adjust stock.`)) return;
    this.adminService.approveReplacement(id).subscribe({
      next: () => { this.toast.success('Replacement approved!'); this.remove(r); },
      error: (e) => this.toast.error(e.error?.message || 'Failed')
    });
  }

  reject(r: any): void {
    const id = r.orderPublicId || r.orderNumber || r.publicId || r.id;
    if (!window.confirm(`Are you sure you want to reject return/replacement for order ${id}?`)) return;
    const comment = this.rejectComments[id] || 'Rejected by admin';
    this.adminService.rejectReturn(id, comment).subscribe({
      next: () => { this.toast.success('Return rejected.'); this.remove(r); },
      error: (e) => this.toast.error(e.error?.message || 'Failed to reject')
    });
  }

  private remove(r: any): void { this.returns = this.returns.filter(x => x !== r); }
}

