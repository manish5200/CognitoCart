import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

import { AdminShellComponent } from '../admin-shell/admin-shell.component';

@Component({
  selector: 'app-admin-kyc',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminShellComponent],
  template: `
    <app-admin-shell>
      <div class="page-header" style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <h1 class="page-title" style="display:flex; align-items:center; gap:12px;">
            <div style="background:var(--grad-brand); border-radius:8px; padding:6px; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 14px rgba(99,102,241,0.3);">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><polyline points="16 11 18 13 22 9"/></svg>
            </div>
            Seller KYC Management
          </h1>
          <p class="page-subtitle">Review and approve seller verification requests</p>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs" style="margin-bottom:24px;">
        <button class="tab" [class.active]="activeTab === 'pending'" (click)="loadPending()" style="display:flex; align-items:center; gap:8px;">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg>
          Pending KYC
        </button>
        <button class="tab" [class.active]="activeTab === 'all'" (click)="loadAll()" style="display:flex; align-items:center; gap:8px;">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" /></svg>
          All Sellers
        </button>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div *ngIf="!loading">
        <div *ngIf="sellers.length === 0" class="empty-state" style="min-height:300px; display:flex; flex-direction:column; align-items:center; justify-content:center; color:var(--text-muted);">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:48px;height:48px;margin-bottom:16px;color:var(--text-dim);"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg>
          <div class="empty-title" style="font-size:18px; font-weight:600;">{{activeTab === 'pending' ? 'No pending KYC requests!' : 'No sellers found'}}</div>
        </div>

        <div class="table-wrapper" *ngIf="sellers.length > 0" style="box-shadow:0 8px 32px rgba(0,0,0,0.4); border:1px solid rgba(255,255,255,0.05); border-radius:12px;">
          <table style="width:100%; border-collapse:collapse;">
            <thead style="background:rgba(255,255,255,0.02); border-bottom:1px solid rgba(255,255,255,0.08);">
              <tr>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Seller</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Store Name</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Email</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">GSTIN</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">KYC Status</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let seller of sellers" style="border-bottom:1px solid rgba(255,255,255,0.04); transition:background 0.2s;" onmouseover="this.style.background='rgba(255,255,255,0.02)'" onmouseout="this.style.background='transparent'">
                <td style="padding:16px;">
                  <div style="font-weight:600; color:#fff;">{{seller.fullName || seller.name}}</div>
                  <div style="font-size:12px; color:var(--text-dim);">
                    <code style="color:var(--primary); background:rgba(99,102,241,0.1); padding:2px 6px; border-radius:4px;">{{seller.sellerCode || seller.sellerPublicId?.slice(0,8)}}</code>
                  </div>
                </td>
                <td style="padding:16px;">{{seller.storeName}}</td>
                <td style="padding:16px; color:var(--text-secondary);">{{seller.email}}</td>
                <td style="padding:16px;">{{seller.gstin || '—'}}</td>
                <td style="padding:16px;">
                  <span class="badge" [class]="'kyc-' + seller.kycStatus" style="padding:4px 10px; font-size:11px;">{{seller.kycStatus}}</span>
                </td>
                <td style="padding:16px;">
                  <div *ngIf="seller.kycStatus === 'PENDING'">
                    <button class="btn btn-primary btn-sm" (click)="startReview(seller)" style="display:flex; align-items:center; gap:6px;">
                      <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z" /></svg>
                      Start Review
                    </button>
                  </div>
                  <div *ngIf="seller.kycStatus === 'IN_REVIEW'">
                    <div style="display:flex; gap:8px; align-items:center; flex-wrap:wrap;">
                      <input type="text" [(ngModel)]="kycComments[seller.sellerPublicId]"
                        class="form-input" placeholder="Comment..." style="width:160px; padding:6px; font-size:12px;" />
                      <button class="btn btn-success btn-sm" (click)="approve(seller)" style="display:flex; align-items:center; justify-content:center; padding:6px;" title="Approve">
                        <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" /></svg>
                      </button>
                      <button class="btn btn-danger btn-sm" (click)="reject(seller)" style="display:flex; align-items:center; justify-content:center; padding:6px;" title="Reject">
                        <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" /></svg>
                      </button>
                    </div>
                  </div>
                  <div *ngIf="seller.kycStatus === 'REJECTED'">
                    <button class="btn btn-primary btn-sm" (click)="startReview(seller)" style="display:flex; align-items:center; gap:6px;">
                      <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" /></svg>
                      Re-Review
                    </button>
                  </div>
                  <div *ngIf="seller.kycStatus === 'VERIFIED'" style="display:flex; gap:8px;">
                    <button class="btn btn-warning btn-sm" (click)="suspend(seller)" style="display:flex; align-items:center; gap:6px;">
                      <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3Z" /></svg>
                      Suspend
                    </button>
                    <button class="btn btn-secondary btn-sm" (click)="openAnalytics(seller)" style="display:flex; align-items:center; gap:6px;" title="Investigate Seller Quality">
                      <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" /></svg>
                    </button>
                  </div>
                  <div *ngIf="seller.kycStatus === 'SUSPENDED'" style="display:flex; gap:8px;">
                    <button class="btn btn-success btn-sm" (click)="approve(seller)" style="display:flex; align-items:center; gap:6px;">
                      <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" /></svg>
                      Reinstate
                    </button>
                    <button class="btn btn-secondary btn-sm" (click)="openAnalytics(seller)" style="display:flex; align-items:center; gap:6px;" title="Investigate Seller Quality">
                      <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" /></svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Seller Analytics Modal -->
      <div class="modal-overlay" *ngIf="showAnalyticsModal" (click)="closeAnalytics()">
        <div class="modal" (click)="$event.stopPropagation()" style="width:700px; max-width:95vw;">
          <div class="modal-header">
            <h3>Analytics: {{ selectedSeller?.storeName }}</h3>
            <button class="close-btn" (click)="closeAnalytics()">&times;</button>
          </div>
          <div class="modal-body" style="background:#f9fafb;">
            <div class="loading-center" *ngIf="analyticsLoading"><div class="spinner"></div></div>
            <div *ngIf="!analyticsLoading && analyticsData">
              <div style="display:grid; grid-template-columns:repeat(4, 1fr); gap:12px; margin-bottom:20px;">
                <div class="stat-card" style="background:#fff; padding:16px; border-radius:8px; box-shadow:0 1px 3px rgba(0,0,0,0.1);">
                  <div style="font-size:12px; color:var(--text-muted); font-weight:600; text-transform:uppercase;">Critical Products</div>
                  <div style="font-size:24px; font-weight:bold; color:var(--danger);">{{ analyticsData.criticalCount || 0 }}</div>
                </div>
                <div class="stat-card" style="background:#fff; padding:16px; border-radius:8px; box-shadow:0 1px 3px rgba(0,0,0,0.1);">
                  <div style="font-size:12px; color:var(--text-muted); font-weight:600; text-transform:uppercase;">Warning Products</div>
                  <div style="font-size:24px; font-weight:bold; color:var(--warning);">{{ analyticsData.warningCount || 0 }}</div>
                </div>
                <div class="stat-card" style="background:#fff; padding:16px; border-radius:8px; box-shadow:0 1px 3px rgba(0,0,0,0.1);">
                  <div style="font-size:12px; color:var(--text-muted); font-weight:600; text-transform:uppercase;">Excellent Products</div>
                  <div style="font-size:24px; font-weight:bold; color:var(--success);">{{ analyticsData.excellentCount || 0 }}</div>
                </div>
                <div class="stat-card" style="background:#fff; padding:16px; border-radius:8px; box-shadow:0 1px 3px rgba(0,0,0,0.1);">
                  <div style="font-size:12px; color:var(--text-muted); font-weight:600; text-transform:uppercase;">Total Evaluated</div>
                  <div style="font-size:24px; font-weight:bold; color:var(--text-primary);">{{ analyticsData.products?.length || 0 }}</div>
                </div>
              </div>
              
              <h4 style="margin:0 0 12px 0; font-size:14px; font-weight:600; color:var(--text-primary);">Product Health Dashboard</h4>
              <div *ngIf="!analyticsData.products || analyticsData.products.length === 0" style="padding:16px; background:#fff; border-radius:8px; text-align:center; color:var(--text-muted); border:1px dashed var(--border-subtle);">
                No products found.
              </div>
              <div *ngIf="analyticsData.products?.length > 0" style="background:#fff; border-radius:8px; border:1px solid var(--border-subtle); overflow:hidden; max-height: 400px; overflow-y: auto;">
                <table style="width:100%; border-collapse:collapse;">
                  <thead style="background:#f3f4f6; border-bottom:1px solid var(--border-subtle); position: sticky; top: 0;">
                    <tr>
                      <th style="padding:12px 16px; text-align:left; font-size:12px; font-weight:600; color:var(--text-muted);">Product</th>
                      <th style="padding:12px 16px; text-align:right; font-size:12px; font-weight:600; color:var(--text-muted);">Returns</th>
                      <th style="padding:12px 16px; text-align:right; font-size:12px; font-weight:600; color:var(--text-muted);">Refund Rate</th>
                      <th style="padding:12px 16px; text-align:left; font-size:12px; font-weight:600; color:var(--text-muted);">Avg Rating</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let p of analyticsData.products" style="border-bottom:1px solid var(--border-subtle);">
                      <td style="padding:12px 16px; font-size:13px; font-weight:500;">
                        {{ p.productName }}
                        <span [class]="'badge badge-' + (p.qualityScore==='CRITICAL'?'danger':(p.qualityScore==='WARNING'?'warning':(p.qualityScore==='GOOD'?'primary':'success')))" style="margin-left:8px; font-size:10px;">{{ p.qualityScore }}</span>
                      </td>
                      <td style="padding:12px 16px; text-align:right; font-size:13px;">{{ p.totalReturns || 0 }}</td>
                      <td style="padding:12px 16px; text-align:right; font-size:13px; color:var(--danger); font-weight:600;">{{ p.refundRate || 0 }}%</td>
                      <td style="padding:12px 16px; font-size:13px; color:var(--text-secondary);">{{ p.averageRating | number:'1.1-1' }} ★</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
          <div class="modal-footer" style="padding:16px; border-top:1px solid var(--border-subtle); display:flex; justify-content:flex-end;">
            <button class="btn btn-secondary" (click)="closeAnalytics()">Close</button>
          </div>
        </div>
      </div>
    </app-admin-shell>
  `,
  styles: [`
    .modal-overlay {
      position: fixed; top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 2000;
      backdrop-filter: blur(2px);
    }
    .modal {
      background: #fff; border-radius: 12px; width: 400px; max-width: 90vw;
      box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04);
      animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .modal-header { padding: 16px 24px; border-bottom: 1px solid var(--border-subtle); display: flex; justify-content: space-between; align-items: center; }
    .modal-header h3 { margin: 0; font-size: 18px; color: var(--text-primary); }
    .close-btn { background: none; border: none; font-size: 24px; cursor: pointer; color: var(--text-muted); }
    @keyframes slideUp {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class AdminKycComponent implements OnInit {
  sellers: any[] = [];
  loading = true;
  activeTab = 'pending';
  kycComments: Record<string, string> = {};

  // Analytics Modal
  showAnalyticsModal = false;
  analyticsLoading = false;
  selectedSeller: any = null;
  analyticsData: any = null;

  constructor(private adminService: AdminService, private toast: ToastService) {}

  ngOnInit(): void { this.loadPending(); }

  loadPending(): void {
    this.activeTab = 'pending';
    this.loading = true;
    this.adminService.getPendingKycSellers().subscribe({
      next: s => { this.sellers = Array.isArray(s) ? s : []; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  loadAll(): void {
    this.activeTab = 'all';
    this.loading = true;
    this.adminService.getAllSellers().subscribe({
      next: s => { this.sellers = Array.isArray(s) ? s : (s.content ?? []); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  startReview(seller: any): void {
    this.adminService.updateKyc(seller.sellerPublicId, 'IN_REVIEW', 'Admin started review').subscribe({
      next: () => { seller.kycStatus = 'IN_REVIEW'; this.toast.info('Review started for ' + seller.storeName); },
      error: (e) => this.toast.error(e.error?.message || 'Failed to start review')
    });
  }

  approve(seller: any): void {
    const comment = this.kycComments[seller.sellerPublicId] || 'KYC verified by admin';
    this.adminService.updateKyc(seller.sellerPublicId, 'VERIFIED', comment).subscribe({
      next: () => { seller.kycStatus = 'VERIFIED'; this.toast.success(`${seller.storeName} KYC verified!`); },
      error: (e) => this.toast.error(e.error?.message || 'Failed to verify')
    });
  }

  reject(seller: any): void {
    const comment = this.kycComments[seller.sellerPublicId] || 'KYC rejected — documents incomplete';
    this.adminService.updateKyc(seller.sellerPublicId, 'REJECTED', comment).subscribe({
      next: () => { seller.kycStatus = 'REJECTED'; this.toast.info(`KYC rejected for ${seller.storeName}`); },
      error: (e) => this.toast.error(e.error?.message || 'Failed')
    });
  }

  suspend(seller: any): void {
    if (!confirm(`Suspend ${seller.storeName}? They will be unable to sell.`)) return;
    this.adminService.updateKyc(seller.sellerPublicId, 'SUSPENDED', 'Suspended by admin').subscribe({
      next: () => { seller.kycStatus = 'SUSPENDED'; this.toast.warning(`${seller.storeName} suspended`); },
      error: (e) => this.toast.error(e.error?.message || 'Failed')
    });
  }

  openAnalytics(seller: any): void {
    this.selectedSeller = seller;
    this.showAnalyticsModal = true;
    this.analyticsLoading = true;
    this.analyticsData = null;

    this.adminService.getSellerAnalytics(seller.sellerPublicId).subscribe({
      next: (data) => {
        this.analyticsData = data;
        this.analyticsLoading = false;
      },
      error: () => {
        this.toast.error('Failed to load analytics data.');
        this.analyticsLoading = false;
      }
    });
  }

  closeAnalytics(): void {
    this.showAnalyticsModal = false;
    this.selectedSeller = null;
    this.analyticsData = null;
  }
}
