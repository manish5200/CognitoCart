import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../services/admin.service';
import { ProductService } from '../../../services/product.service';
import { ToastService } from '../../../services/toast.service';

import { AdminShellComponent } from '../admin-shell/admin-shell.component';

@Component({
  selector: 'app-admin-intelligence',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminShellComponent],
  template: `
    <app-admin-shell>
      <div class="page-header" style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <h1 class="page-title" style="display:flex; align-items:center; gap:12px;">
            <div style="background:var(--grad-brand); border-radius:8px; padding:6px; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 14px rgba(99,102,241,0.3);">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
            </div>
            Platform Intelligence
          </h1>
          <p class="page-subtitle">BI metrics, Customer Lifetime Value analysis, and Churn prediction</p>
        </div>
        <div style="display:flex; gap:16px; align-items:center;">
          <button class="btn btn-secondary" (click)="reindexSearch()" [disabled]="reindexing" style="display:flex; align-items:center; gap:8px;">
            <svg *ngIf="!reindexing" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 2v6h-6"></path><path d="M3 12a9 9 0 0 1 15-6.7L21 8"></path><path d="M3 22v-6h6"></path><path d="M21 12a9 9 0 0 1-15 6.7L3 16"></path></svg>
            <span *ngIf="reindexing" class="mini-spinner" style="border-radius:50%; width:14px; height:14px; border:2px solid currentColor; border-top-color:transparent; animation: spin 1s linear infinite; display:inline-block;"></span>
            {{reindexing ? 'Reindexing AI...' : 'Reindex AI Search'}}
          </button>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs" style="margin-bottom:32px;">
        <button class="tab" [class.active]="tab === 'platform'" (click)="tab='platform'; loadPlatform()" style="display:flex; align-items:center; gap:6px;">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" /></svg>
          Platform BI
        </button>
        <button class="tab" [class.active]="tab === 'customers'" (click)="tab='customers'; loadCustomers()" style="display:flex; align-items:center; gap:6px;">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z" /></svg>
          Customer Intelligence
        </button>
        <button class="tab" [class.active]="tab === 'category'" (click)="tab='category'; loadCategoryRevenue()" style="display:flex; align-items:center; gap:6px;">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 18 9 11.25l4.306 4.306a11.95 11.95 0 0 1 5.814-5.518l2.74-1.22m0 0-5.94-2.281m5.94 2.28-2.28 5.941" /></svg>
          Category Revenue
        </button>
      </div>

      <!-- Platform BI -->
      <div *ngIf="tab === 'platform'">
        <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>
        <div *ngIf="!loading && biData">
          <div class="grid-4" style="margin-bottom:32px;">
            <div class="stat-card" *ngFor="let metric of biMetrics">
              <div class="stat-icon" [class]="metric.color" [innerHTML]="metric.icon"></div>
              <div>
                <div class="stat-value">{{metric.value}}</div>
                <div class="stat-label">{{metric.label}}</div>
              </div>
            </div>
          </div>

          <div *ngIf="biData.topReturnReasons?.length" class="card">
            <div class="card-body">
              <h3 style="margin-bottom:20px; display:flex; align-items:center; gap:8px;">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:20px;height:20px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="M12 3c2.755 0 5.455.232 8.083.678.533.09.917.556.917 1.096v1.044a2.25 2.25 0 0 1-.659 1.591l-5.432 5.432a2.25 2.25 0 0 0-.659 1.591v2.927a2.25 2.25 0 0 1-1.244 2.013L9.75 21v-6.568a2.25 2.25 0 0 0-.659-1.591L3.659 7.409A2.25 2.25 0 0 1 3 5.818V4.774c0-.54.384-1.006.917-1.096A48.32 48.32 0 0 1 12 3Z" /></svg>
                Top Return Reasons
              </h3>
              <div *ngFor="let reason of biData.topReturnReasons" style="display:flex; align-items:center; gap:16px; margin-bottom:12px;">
                <span style="width:140px; font-size:13px; color:var(--text-muted);">{{reason.returnReason}}</span>
                <span style="width:40px; text-align:center; font-weight:600; color:var(--warning);">{{reason.count}}</span>
                <div class="rating-bar" style="flex:1; height:10px;">
                  <div class="rating-bar-fill" [style.width.%]="reason.count * 10" style="background:var(--gradient-ocean);"></div>
                </div>
                <span style="width:100px; text-align:right; font-weight:600; color:#fff;">\u20B9{{reason.financialImpact | number:'1.0-0'}}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Customer Intelligence -->
      <div *ngIf="tab === 'customers'">
        <div style="display:flex; gap:16px; margin-bottom:24px; align-items:flex-end;">
          <div class="form-group" style="margin:0;">
            <label class="form-label">Top N Customers</label>
            <input type="number" [(ngModel)]="topN" class="form-input" style="width:100px;" min="5" max="100" />
          </div>
          <div class="form-group" style="margin:0;">
            <label class="form-label">Churn after (days)</label>
            <input type="number" [(ngModel)]="churnDays" class="form-input" style="width:120px;" min="30" max="365" />
          </div>
          <button class="btn btn-primary" (click)="loadCustomers()">Refresh</button>
        </div>

        <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

        <div *ngIf="!loading">
          <div class="grid-2">
            <!-- Top Customers (CLV) -->
            <div class="table-wrapper" style="box-shadow:0 8px 32px rgba(0,0,0,0.4); border:1px solid rgba(255,255,255,0.05); border-radius:12px;">
              <div class="table-header" style="background:rgba(255,255,255,0.02); border-bottom:1px solid rgba(255,255,255,0.08); display:flex; align-items:center; gap:8px;">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:18px;height:18px;color:var(--warning);"><path stroke-linecap="round" stroke-linejoin="round" d="M16.5 18.75h-9m9 0a3 3 0 0 1 3 3h-15a3 3 0 0 1 3-3m9 0v-3.375c0-.621-.503-1.125-1.125-1.125h-.871M7.5 18.75v-3.375c0-.621.504-1.125 1.125-1.125h.872m5.007 0H9.497m5.007 0a7.454 7.454 0 0 1-.982-3.172M9.497 14.25a7.454 7.454 0 0 0 .981-3.172M5.25 4.236c-.982.143-1.954.317-2.916.52A6.003 6.003 0 0 0 7.73 9.728M5.25 4.236V4.5c0 2.108.966 3.99 2.48 5.228M5.25 4.236V2.721C7.456 2.41 9.71 2.25 12 2.25c2.291 0 4.545.16 6.75.47v1.516M7.73 9.728a6.726 6.726 0 0 0 2.748 1.35m8.272-6.842V4.5c0 2.108-.966 3.99-2.48 5.228m2.48-5.492a46.32 46.32 0 0 1 2.916.52 6.003 6.003 0 0 1-5.395 4.972m0 0a6.726 6.726 0 0 1-2.749 1.35m0 0a6.772 6.772 0 0 1-3.044 0" /></svg>
                <span class="table-title">Top {{topN}} Customers (CLV)</span>
              </div>
              <table style="width:100%; border-collapse:collapse;">
                <thead style="background:rgba(255,255,255,0.02); border-bottom:1px solid rgba(255,255,255,0.08);">
                  <tr>
                    <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">#</th>
                    <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Customer</th>
                    <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Orders</th>
                    <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Lifetime Value</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let c of topCustomers; let i = index" style="border-bottom:1px solid rgba(255,255,255,0.04);">
                    <td style="padding:16px; color:var(--warning); font-weight:700;">{{i+1}}</td>
                    <td style="padding:16px;">
                      <div style="font-weight:600; color:#fff;">{{c.customerName || c.name}}</div>
                      <div style="font-size:12px; color:var(--text-dim);">{{c.email}}</div>
                    </td>
                    <td style="padding:16px;">{{c.totalOrders}}</td>
                    <td style="padding:16px; font-weight:700; color:var(--primary);">\u20B9{{c.totalSpent | number:'1.0-0'}}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <!-- Churned Customers -->
            <div class="table-wrapper" style="box-shadow:0 8px 32px rgba(0,0,0,0.4); border:1px solid rgba(255,255,255,0.05); border-radius:12px;">
              <div class="table-header" style="background:rgba(255,255,255,0.02); border-bottom:1px solid rgba(255,255,255,0.08); display:flex; align-items:center; gap:8px;">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:18px;height:18px;color:var(--danger);"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3Z" /></svg>
                <span class="table-title">At-Risk Customers ({{churnDays}}+ days inactive)</span>
              </div>
              <table style="width:100%; border-collapse:collapse;">
                <thead style="background:rgba(255,255,255,0.02); border-bottom:1px solid rgba(255,255,255,0.08);">
                  <tr>
                    <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Customer</th>
                    <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Last Order</th>
                    <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Days Inactive</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let c of churnedCustomers" style="border-bottom:1px solid rgba(255,255,255,0.04);">
                    <td style="padding:16px;">
                      <div style="font-weight:600; color:#fff;">{{c.customerName || c.name}}</div>
                      <div style="font-size:12px; color:var(--text-dim);">{{c.email}}</div>
                    </td>
                    <td style="padding:16px;">{{c.lastOrderDate | date:'mediumDate'}}</td>
                    <td style="padding:16px;"><span class="badge badge-red">{{c.daysSinceLastOrder}} days</span></td>
                  </tr>
                  <tr *ngIf="churnedCustomers.length === 0">
                    <td colspan="3" style="text-align:center; padding:32px; color:var(--text-muted);">No churned customers found</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      <!-- Category Revenue -->
      <div *ngIf="tab === 'category'">
        <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>
        <div *ngIf="!loading && categoryRevenue.length > 0">
          <div class="table-wrapper" style="box-shadow:0 8px 32px rgba(0,0,0,0.4); border:1px solid rgba(255,255,255,0.05); border-radius:12px;">
            <table style="width:100%; border-collapse:collapse;">
              <thead style="background:rgba(255,255,255,0.02); border-bottom:1px solid rgba(255,255,255,0.08);">
                <tr>
                  <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Category</th>
                  <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Revenue</th>
                  <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Orders</th>
                  <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Avg Order Value</th>
                  <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Share</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let c of categoryRevenue" style="border-bottom:1px solid rgba(255,255,255,0.04); transition:background 0.2s;" onmouseover="this.style.background='rgba(255,255,255,0.02)'" onmouseout="this.style.background='transparent'">
                  <td style="padding:16px; font-weight:600; color:#fff;">{{c.categoryName}}</td>
                  <td style="padding:16px; font-weight:700; color:var(--primary);">\u20B9{{c.revenue | number:'1.0-0'}}</td>
                  <td style="padding:16px;">{{c.totalOrders}}</td>
                  <td style="padding:16px;">\u20B9{{c.averageOrderValue | number:'1.0-0'}}</td>
                  <td style="padding:16px;">
                    <div style="display:flex; align-items:center; gap:12px;">
                      <div class="rating-bar" style="width:100px; height:8px;">
                        <div class="rating-bar-fill" [style.width.%]="c.revenueShare" style="background:var(--gradient-primary);"></div>
                      </div>
                      <span style="font-size:13px; font-weight:600;">{{c.revenueShare | number:'1.1-1'}}%</span>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </app-admin-shell>
  `,
  styles: [`
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class AdminIntelligenceComponent implements OnInit {
  tab = 'platform';
  loading = false;
  biData: any = null;
  topCustomers: any[] = [];
  churnedCustomers: any[] = [];
  categoryRevenue: any[] = [];
  topN = 10;
  churnDays = 60;

  get biMetrics(): any[] {
    if (!this.biData) return [];
    return [
      { icon: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"></line><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>', label: 'Gross Revenue', value: '\u20B9' + ((this.biData.financialHealth?.grossRevenue ?? 0) | 0).toLocaleString(), color: 'green' },
      { icon: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>', label: 'Net Revenue', value: '\u20B9' + ((this.biData.financialHealth?.netRevenue ?? 0) | 0).toLocaleString(), color: 'blue' },
      { icon: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path><line x1="3" y1="6" x2="21" y2="6"></line><path d="M16 10a4 4 0 0 1-8 0"></path></svg>', label: 'Total Return Requests', value: this.biData.totalReturnRequests ?? 0, color: 'purple' },
      { icon: '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2"></polygon><polyline points="2 17 12 22 22 17"></polyline><polyline points="2 12 12 17 22 12"></polyline></svg>', label: 'Refund Rate', value: ((this.biData.financialHealth?.refundRatePercentage ?? 0).toFixed(1)) + '%', color: 'yellow' },
    ];
  }

  reindexing = false;

  constructor(
    private adminService: AdminService,
    private productService: ProductService,
    private toast: ToastService
  ) {}

  ngOnInit(): void { this.loadPlatform(); }

  loadPlatform(): void {
    this.loading = true;
    this.adminService.getPlatformIntelligence().subscribe({
      next: d => { this.biData = d; this.loading = false; },
      error: () => { this.loading = false; this.toast.error('Failed to load BI data'); }
    });
  }

  loadCustomers(): void {
    this.loading = true;
    this.adminService.getCustomerIntelligence(this.topN, this.churnDays).subscribe({
      next: d => {
        this.topCustomers = d.topCustomers ?? [];
        this.churnedCustomers = d.atRiskCustomers ?? [];
        this.loading = false;
      },
      error: () => { this.loading = false; this.toast.error('Failed to load customer intelligence'); }
    });
  }

  loadCategoryRevenue(): void {
    this.loading = true;
    this.adminService.getCategoryRevenue().subscribe({
      next: d => {
        const data = Array.isArray(d) ? d : [];
        const total = data.reduce((sum, c) => sum + (c.revenue || 0), 0);
        this.categoryRevenue = data.map(c => ({
          ...c,
          averageOrderValue: c.totalOrders ? (c.revenue / c.totalOrders) : 0,
          revenueShare: total ? (c.revenue / total) * 100 : 0
        }));
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  reindexSearch(): void {
    if (this.reindexing) return;
    this.reindexing = true;
    this.productService.reindexAi().subscribe({
      next: (res) => {
        this.reindexing = false;
        const msg = res?.Status || 'Reindex triggered successfully!';
        this.toast.success(msg);
      },
      error: () => {
        this.reindexing = false;
        this.toast.error('Failed to trigger reindex');
      }
    });
  }
}
