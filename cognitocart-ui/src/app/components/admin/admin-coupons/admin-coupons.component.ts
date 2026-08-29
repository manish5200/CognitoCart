import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

import { AdminShellComponent } from '../admin-shell/admin-shell.component';

@Component({
  selector: 'app-admin-coupons',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminShellComponent],
  template: `
    <app-admin-shell>
      <div class="page-header" style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <h1 class="page-title" style="display:flex; align-items:center; gap:12px;">
            <div style="background:var(--grad-brand); border-radius:8px; padding:6px; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 14px rgba(99,102,241,0.3);">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg>
            </div>
            Coupon Engine
          </h1>
          <p class="page-subtitle">Create and manage discount coupons</p>
        </div>
        <button class="btn btn-primary" (click)="showForm = !showForm" style="display:flex; align-items:center; gap:6px;">
          <svg *ngIf="showForm" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" /></svg>
          <svg *ngIf="!showForm" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" /></svg>
          {{showForm ? 'Cancel' : 'Create Coupon'}}
        </button>
      </div>

      <!-- Create Form -->
      <div class="card" *ngIf="showForm" style="margin-bottom:24px;">
        <div class="card-body">
          <h3 style="margin-bottom:20px;">New Coupon</h3>
          <div class="form-row form-row-2">
            <div class="form-group">
              <label class="form-label">Coupon Code *</label>
              <input type="text" [(ngModel)]="newCoupon.code" class="form-input" placeholder="SAVE20" style="text-transform:uppercase;" />
            </div>
            <div class="form-group">
              <label class="form-label">Discount Type *</label>
              <select [(ngModel)]="newCoupon.discountType" class="form-select">
                <option value="PERCENTAGE">Percentage (%)</option>
                <option value="FLAT">Flat Amount (₹)</option>
              </select>
            </div>
          </div>
          <div class="form-row form-row-3">
            <div class="form-group">
              <label class="form-label">Discount Value *</label>
              <input type="number" [(ngModel)]="newCoupon.discountValue" class="form-input" placeholder="20" />
            </div>
            <div class="form-group">
              <label class="form-label">Max Discount (₹)</label>
              <input type="number" [(ngModel)]="newCoupon.maxDiscountAmount" class="form-input" placeholder="500" />
            </div>
            <div class="form-group">
              <label class="form-label">Min Order (₹)</label>
              <input type="number" [(ngModel)]="newCoupon.minOrderAmount" class="form-input" placeholder="299" />
            </div>
          </div>
          <div class="form-row form-row-2">
            <div class="form-group">
              <label class="form-label">Expires At</label>
              <input type="datetime-local" [(ngModel)]="newCoupon.expiresAt" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">Max Usage</label>
              <input type="number" [(ngModel)]="newCoupon.maxUsage" class="form-input" placeholder="1000" />
            </div>
          </div>
          <button class="btn btn-primary" (click)="createCoupon()" [disabled]="creating" style="display:flex; align-items:center; gap:6px;">
            <svg *ngIf="!creating" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" /></svg>
            {{creating ? 'Creating...' : 'Create Coupon'}}
          </button>
        </div>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div class="table-wrapper" *ngIf="!loading" style="box-shadow:0 8px 32px rgba(0,0,0,0.4); border:1px solid rgba(255,255,255,0.05); border-radius:12px;">
        <table style="width:100%; border-collapse:collapse;">
          <thead style="background:rgba(255,255,255,0.02); border-bottom:1px solid rgba(255,255,255,0.08);">
            <tr>
              <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Code</th>
              <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Type</th>
              <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Value</th>
              <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Min Order</th>
              <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Max Discount</th>
              <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Max Usage</th>
              <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Expires</th>
              <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Status</th>
              <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of coupons" style="border-bottom:1px solid rgba(255,255,255,0.04); transition:background 0.2s;" onmouseover="this.style.background='rgba(255,255,255,0.02)'" onmouseout="this.style.background='transparent'">
              <td style="padding:16px;"><code style="color:var(--primary); font-size:15px; font-weight:700; background:rgba(99,102,241,0.1); padding:4px 8px; border-radius:4px;">{{c.code}}</code></td>
              <td style="padding:16px;"><span class="badge badge-blue" style="padding:4px 10px; font-size:11px;">{{c.discountType}}</span></td>
              <td style="padding:16px; font-weight:600; color:#fff;">
                {{c.discountType === 'PERCENTAGE' ? c.discountValue + '%' : '₹' + c.discountValue}}
              </td>
              <td style="padding:16px;">{{c.minOrderAmount ? '₹' + c.minOrderAmount : '—'}}</td>
              <td style="padding:16px;">{{c.maxDiscountAmount ? '₹' + c.maxDiscountAmount : '—'}}</td>
              <td style="padding:16px;">{{c.maxUsage ?? '∞'}} <span *ngIf="c.usedCount !== undefined" style="color:var(--text-dim);">({{c.usedCount}} used)</span></td>
              <td style="padding:16px; color:var(--text-secondary);">{{c.expiresAt ? (c.expiresAt | date:'mediumDate') : '—'}}</td>
              <td style="padding:16px;">
                <span class="badge" [class]="c.active ? 'badge-green' : 'badge-gray'" style="padding:4px 10px; font-size:11px;">
                  {{c.active ? 'Active' : 'Inactive'}}
                </span>
              </td>
              <td style="padding:16px;">
                <button class="btn btn-sm" [class]="c.active ? 'btn-warning' : 'btn-success'" (click)="toggle(c)">
                  {{c.active ? 'Deactivate' : 'Activate'}}
                </button>
              </td>
            </tr>
            <tr *ngIf="coupons.length === 0">
              <td colspan="9" style="text-align:center; padding:48px; color:var(--text-muted);">No coupons yet</td>
            </tr>
          </tbody>
        </table>
      </div>
    </app-admin-shell>
  `,
  styles: []
})
export class AdminCouponsComponent implements OnInit {
  coupons: any[] = [];
  loading = true;
  showForm = false;
  creating = false;
  newCoupon: any = { code: '', discountType: 'PERCENTAGE', discountValue: 0, maxDiscountAmount: null, minOrderAmount: null, expiresAt: '', maxUsage: null };

  constructor(private adminService: AdminService, private toast: ToastService) {}

  ngOnInit(): void {
    this.adminService.getCoupons().subscribe({
      next: c => { this.coupons = Array.isArray(c) ? c : (c.content ?? []); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  createCoupon(): void {
    if (!this.newCoupon.code || !this.newCoupon.discountValue) { this.toast.warning('Code and discount value required'); return; }
    this.creating = true;
    const payload = { ...this.newCoupon, code: this.newCoupon.code.toUpperCase() };
    if (!payload.maxDiscountAmount) delete payload.maxDiscountAmount;
    if (!payload.minOrderAmount) delete payload.minOrderAmount;
    if (!payload.expiresAt) delete payload.expiresAt;
    if (!payload.maxUsage) delete payload.maxUsage;

    this.adminService.createCoupon(payload).subscribe({
      next: (c) => {
        this.toast.success('Coupon created!');
        this.coupons.unshift(c);
        this.creating = false;
        this.showForm = false;
        this.newCoupon = { code: '', discountType: 'PERCENTAGE', discountValue: 0 };
      },
      error: (e) => { this.creating = false; this.toast.error(e.error?.message || 'Failed to create'); }
    });
  }

  toggle(coupon: any): void {
    this.adminService.toggleCoupon(coupon.publicId).subscribe({
      next: () => { coupon.active = !coupon.active; this.toast.success(`Coupon ${coupon.active ? 'activated' : 'deactivated'}!`); },
      error: (e) => this.toast.error(e.error?.message || 'Failed to toggle')
    });
  }
}
