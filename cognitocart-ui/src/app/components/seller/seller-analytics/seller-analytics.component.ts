import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SellerService } from '../../../services/seller.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-seller-analytics',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="command-center">
      <div class="cc-header">
        <div class="header-left">
          <div class="greeting-block">
            <h1 style="display:flex; align-items:center; gap:12px;">
              <div class="kpi-icon kpi-icon-blue">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:24px;height:24px;color:var(--blue-400);"><path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" /></svg>
              </div>
              Product Analytics
            </h1>
            <p>Quality scores, ratings and return rates for your products.</p>
          </div>
        </div>
        <div class="header-right">
          <button class="btn btn-secondary cc-btn" (click)="download()">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;margin-right:4px;"><path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5M16.5 12 12 16.5m0 0L7.5 12m4.5 4.5V3" /></svg>
            Download Revenue CSV
          </button>
        </div>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div class="cc-panel" *ngIf="!loading">
        <div class="panel-header border-bottom">
          <h2>Product Quality Scores</h2>
        </div>
        <div class="cc-table-wrapper">
          <table class="cc-table">
            <thead>
              <tr>
                <th>Product</th>
                <th>Avg Rating</th>
                <th>Total Reviews</th>
                <th>Return Rate</th>
                <th>Quality Score</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let p of analytics">
                <td>
                  <div style="font-weight:600; color:#fff;">{{p.productName}}</div>
                  <div style="font-size:12px; color:var(--text-dim);">{{p.productPublicId}}</div>
                </td>
                <td>
                  <div style="display:flex; align-items:center; gap:4px; color:var(--warning);">
                    <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M11.48 3.499a.562.562 0 0 1 1.04 0l2.125 5.111a.563.563 0 0 0 .475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 0 0-.182.557l1.285 5.385a.562.562 0 0 1-.84.61l-4.725-2.885a.562.562 0 0 0-.586 0L6.982 20.54a.562.562 0 0 1-.84-.61l1.285-5.386a.562.562 0 0 0-.182-.557l-4.204-3.602a.562.562 0 0 1 .321-.988l5.518-.442a.563.563 0 0 0 .475-.345L11.48 3.5Z" /></svg>
                    {{p.averageRating | number:'1.1-1'}}
                  </div>
                </td>
                <td>{{p.totalReviews}}</td>
                <td>
                  <span [class]="p.returnRate > 10 ? 'badge badge-red' : 'badge badge-green'">
                    {{p.returnRate | number:'1.1-1'}}%
                  </span>
                </td>
                <td>
                  <div class="quality-bar">
                    <div class="quality-fill" 
                      [style.width.%]="p.qualityScore === 'EXCELLENT' ? 95 : p.qualityScore === 'GOOD' ? 75 : p.qualityScore === 'WARNING' ? 50 : 25"
                      [class.q-green]="p.qualityScore === 'EXCELLENT' || p.qualityScore === 'GOOD'"
                      [class.q-yellow]="p.qualityScore === 'WARNING'"
                      [class.q-red]="p.qualityScore === 'CRITICAL'">
                    </div>
                  </div>
                  <span style="font-size:12px; color:var(--text-muted);">{{p.qualityScore}}</span>
                </td>
                <td>
                  <span class="badge" [class]="p.isActive ? 'badge-green' : 'badge-gray'">
                    {{p.isActive ? 'Active' : 'Inactive'}}
                  </span>
                </td>
              </tr>
              <tr *ngIf="analytics.length === 0">
                <td colspan="6" style="text-align:center; padding:48px; color:var(--text-muted);">No analytics data yet</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .quality-bar { width: 100px; height: 6px; background: var(--border); border-radius: 3px; overflow: hidden; margin-bottom: 4px; }
    .quality-fill { height: 100%; border-radius: 3px; transition: width 0.5s ease; }
    .q-green { background: var(--accent); }
    .q-yellow { background: var(--warning); }
    .q-red { background: var(--danger); }
  `]
})
export class SellerAnalyticsComponent implements OnInit {
  analytics: any[] = [];
  loading = true;

  constructor(private sellerService: SellerService, private toast: ToastService) {}

  ngOnInit(): void {
    this.sellerService.getProductAnalytics().subscribe({
      next: (d) => { this.analytics = Array.isArray(d) ? d : (d.products ?? []); this.loading = false; },
      error: () => { this.loading = false; this.toast.error('Failed to load analytics'); }
    });
  }

  download(): void {
    this.sellerService.downloadRevenueCsv().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'revenue.csv';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.toast.error('Failed to download CSV')
    });
  }
}
