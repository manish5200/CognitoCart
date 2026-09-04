import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SellerService } from '../../../services/seller.service';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { SellerIdentityComponent } from '../../shared/seller-identity/seller-identity.component';

@Component({
  selector: 'app-seller-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, SellerIdentityComponent],
  template: `
    <div class="dashboard-layout" [class.sidebar-open]="sidebarOpen">
      <!-- Mobile Overlay -->
      <div class="sidebar-overlay" *ngIf="sidebarOpen" (click)="toggleSidebar()"></div>
      
      <!-- Premium Seller Sidebar -->
      <aside class="sidebar premium-sidebar" [class.open]="sidebarOpen">
        <div class="sidebar-inner">
          <div class="sidebar-header">
            <app-seller-identity
              *ngIf="auth.currentUser$ | async as user"
              [name]="user.name || 'Store Owner'"
              [status]="stats?.kycStatus === 'VERIFIED' ? 'VERIFIED SELLER' : 'PENDING KYC'">
            </app-seller-identity>
            <button class="mobile-close-btn" (click)="toggleSidebar()">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
          </div>

          <div class="nav-groups">
            <span class="sidebar-section-title">Command Center</span>
            <a routerLink="/seller" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}" class="sidebar-link">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="sidebar-link-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-2.25Z" /></svg> 
              <span class="sidebar-text">Overview</span>
            </a>
            <a routerLink="/seller/analytics" routerLinkActive="active" class="sidebar-link">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="sidebar-link-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" /></svg> 
              <span class="sidebar-text">Analytics</span>
            </a>

            <span class="sidebar-section-title">Store Management</span>
            <a routerLink="/seller/orders" routerLinkActive="active" class="sidebar-link">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="sidebar-link-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z" /></svg> 
              <span class="sidebar-text">Orders</span>
              <span class="badge-count" *ngIf="actionRequiredCount > 0">{{actionRequiredCount}}</span>
            </a>
            <a routerLink="/seller/products" routerLinkActive="active" class="sidebar-link">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="sidebar-link-icon"><path stroke-linecap="round" stroke-linejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" /></svg> 
              <span class="sidebar-text">Inventory</span>
            </a>
            <a routerLink="/seller/policies" routerLinkActive="active" class="sidebar-link">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="sidebar-link-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25ZM6.75 12h.008v.008H6.75V12Zm0 3h.008v.008H6.75V15Zm0 3h.008v.008H6.75V18Z" /></svg> 
              <span class="sidebar-text">Return Policies</span>
            </a>
            <a routerLink="/seller/sales" routerLinkActive="active" class="sidebar-link">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="sidebar-link-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75Z" /></svg> 
              <span class="sidebar-text">Flash Sales</span>
            </a>
          </div>

          <div class="sidebar-footer">
            <a routerLink="/" class="sidebar-link ghost-link">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="sidebar-link-icon"><path stroke-linecap="round" stroke-linejoin="round" d="m9 15 6-6m0 0-6-6m6 6H3" /></svg> 
              <span class="sidebar-text">Exit to Customer View</span>
            </a>
          </div>
        </div>
      </aside>

      <!-- Main Content Area -->
      <main class="dashboard-content command-center">
        <div class="loading-overlay" *ngIf="loading">
          <div class="spinner"></div>
        </div>

        <ng-container *ngIf="!loading">
          <!-- Premium Context Header -->
          <header class="cc-header fade-in">
            <div class="header-left">
              <button class="mobile-menu-btn" (click)="toggleSidebar()">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" /></svg>
              </button>
              <div class="greeting-block" *ngIf="auth.currentUser$ | async as user">
                <h1>Good {{getTimeOfDay()}}, {{user.name ? user.name.split(' ')[0] : 'Seller'}} 👋</h1>
                <p>Here's what's happening with your store today.</p>
              </div>
            </div>
            <div class="header-right">
              <button class="btn btn-secondary btn-icon" (click)="loadStats()" title="Refresh Data">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" /></svg>
              </button>
              <a routerLink="/seller/products" class="btn btn-primary cc-btn">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" /></svg>
                Add Product
              </a>
            </div>
          </header>

          <!-- Warning/KYC Banner -->
          <div *ngIf="stats?.kycStatus && stats.kycStatus !== 'VERIFIED'" class="cc-alert cc-alert-warning slide-down">
            <div class="alert-icon">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
            </div>
            <div class="alert-content">
              <strong>Verification Required</strong>
              <span>Your KYC status is {{stats.kycStatus}}. Complete verification to withdraw funds and lift order limits.</span>
            </div>
            <button class="btn btn-outline-warning btn-sm">Verify Now</button>
          </div>

          <!-- Attention Required & AI Insights Grid -->
          <div class="grid-2-layout">
            <!-- Left: Action Hub -->
            <div class="cc-panel action-hub">
              <div class="panel-header">
                <h2>Attention Required</h2>
              </div>
              <div class="panel-body">
                <div class="action-items-list">
                  
                  <a routerLink="/seller/orders" [queryParams]="{status: 'CONFIRMED'}" class="action-item" [class.empty]="actionRequiredCount === 0">
                    <div class="item-icon bg-indigo"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" /></svg></div>
                    <div class="item-content">
                      <span class="item-title">Orders to Fulfill</span>
                      <span class="item-desc" *ngIf="actionRequiredCount > 0">{{actionRequiredCount}} orders await packing</span>
                      <span class="item-desc" *ngIf="actionRequiredCount === 0">You're all caught up!</span>
                    </div>
                    <div class="item-action" *ngIf="actionRequiredCount > 0">
                      <span class="badge-urgent">{{actionRequiredCount}}</span>
                      <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" /></svg>
                    </div>
                  </a>

                  <a routerLink="/seller/products" class="action-item" [class.empty]="(stats?.outOfStockProducts || 0) === 0">
                    <div class="item-icon bg-amber"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg></div>
                    <div class="item-content">
                      <span class="item-title">Low Stock Alerts</span>
                      <span class="item-desc" *ngIf="(stats?.outOfStockProducts || 0) > 0">{{stats?.outOfStockProducts}} products out of stock</span>
                      <span class="item-desc" *ngIf="(stats?.outOfStockProducts || 0) === 0">Inventory levels are healthy</span>
                    </div>
                    <div class="item-action" *ngIf="(stats?.outOfStockProducts || 0) > 0">
                      <span class="badge-warning">{{stats?.outOfStockProducts}}</span>
                      <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" /></svg>
                    </div>
                  </a>

                </div>
              </div>
            </div>

            <!-- Right: AI Insights -->
            <div class="cc-panel ai-insights">
              <div class="panel-header">
                <h2><span class="ai-sparkle">&#10024;</span> Cognito Insights</h2>
              </div>
              <div class="panel-body">
                <div class="insight-card">
                  <div class="insight-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 18 9 11.25l4.306 4.306a11.95 11.95 0 0 1 5.814-5.518l2.74-1.22m0 0-5.94-2.281m5.94 2.28-2.28 5.941" /></svg></div>
                  <div class="insight-text">
                    <strong>High Demand Anticipated</strong>
                    Your top product is trending! Ensure adequate stock to capture a projected 15% increase in weekend sales.
                  </div>
                </div>
                <div class="insight-card secondary">
                  <div class="insight-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6v12m-3-2.818.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg></div>
                  <div class="insight-text">
                    <strong>Pricing Opportunity</strong>
                    Consider joining the upcoming Flash Sale to clear aged inventory and boost overall visibility.
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Business Performance KPIs -->
          <div class="kpi-grid">
            <div class="cc-kpi-card">
              <div class="kpi-header">
                <span class="kpi-title">Gross Revenue</span>
                <div class="kpi-trend positive"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 18 9 11.25l4.306 4.306a11.95 11.95 0 0 1 5.814-5.518l2.74-1.22m0 0-5.94-2.281m5.94 2.28-2.28 5.941" /></svg> +12%</div>
              </div>
              <div class="kpi-value">\u20B9{{(stats?.totalRevenue || 0) | number:'1.0-0'}}</div>
              <div class="kpi-subtext">\u20B9{{(stats?.pendingRevenue || 0) | number:'1.0-0'}} pending clearance</div>
            </div>

            <div class="cc-kpi-card">
              <div class="kpi-header">
                <span class="kpi-title">Total Orders</span>
                <div class="kpi-trend positive"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 18 9 11.25l4.306 4.306a11.95 11.95 0 0 1 5.814-5.518l2.74-1.22m0 0-5.94-2.281m5.94 2.28-2.28 5.941" /></svg> +8%</div>
              </div>
              <div class="kpi-value">{{stats?.totalOrders || 0}}</div>
              <div class="kpi-subtext">{{stats?.pendingOrders || 0}} pending, {{stats?.deliveredOrders || 0}} delivered</div>
            </div>

            <div class="cc-kpi-card">
              <div class="kpi-header">
                <span class="kpi-title">Active Catalog</span>
              </div>
              <div class="kpi-value">{{stats?.totalProducts || 0}}</div>
              <div class="kpi-subtext">{{stats?.availableProducts || 0}} currently live</div>
            </div>
          </div>

          <!-- Bottom Section: Top Products -->
          <div class="cc-panel mt-6" *ngIf="stats?.topProducts?.length">
            <div class="panel-header border-bottom">
              <h2>Top Performing Products</h2>
              <a routerLink="/seller/analytics" class="btn btn-ghost btn-sm">View Report</a>
            </div>
            <div class="panel-body no-padding">
              <div class="cc-table-wrapper">
                <table class="cc-table">
                  <thead>
                    <tr>
                      <th class="rank-col">Rank</th>
                      <th>Product Name</th>
                      <th class="text-right">Units Sold</th>
                      <th class="text-right">Revenue</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let p of stats.topProducts; let i = index">
                      <td class="rank-col"><span class="rank-badge" [class.top-rank]="i < 3">#{{i+1}}</span></td>
                      <td class="font-medium text-white">{{p.productName || p.name}}</td>
                      <td class="text-right">{{p.unitsSold || 0}}</td>
                      <td class="text-right font-bold text-indigo">\u20B9{{(p.revenue || 0) | number:'1.0-0'}}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- Empty state for new sellers -->
          <div *ngIf="!stats?.totalOrders && !stats?.totalProducts" class="cc-empty-state mt-6">
            <div class="empty-icon-ring">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M15.59 14.37a6 6 0 0 1-5.84 7.38v-4.8m5.84-2.58a14.98 14.98 0 0 0 6.16-12.12A14.98 14.98 0 0 0 9.631 8.41m5.96 5.96a14.926 14.926 0 0 1-5.841 2.58m-.119-8.54a6 6 0 0 0-7.381 5.84h4.8m2.581-5.84a14.927 14.927 0 0 0-2.58 5.84m2.699 2.7c-.103.021-.207.041-.311.06a15.09 15.09 0 0 1-2.448-2.45m.31-.31c.019-.104.04-.208.06-.312m2.448 2.45-.31-.31" /></svg>
            </div>
            <h3>Launch Your Store</h3>
            <p>Your command center awaits data. Add your first product to activate your analytics dashboard.</p>
            <a routerLink="/seller/products" class="btn btn-primary mt-4">Add Your First Product</a>
          </div>

        </ng-container>
      </main>
    </div>
  `
})
export class SellerDashboardComponent implements OnInit {
  stats: any = null;
  loading = true;
  sidebarOpen = false;
  actionRequiredCount = 0;

  constructor(private sellerService: SellerService, public auth: AuthService, private toast: ToastService) {}

  ngOnInit(): void { this.loadStats(); }

  getTimeOfDay(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'morning';
    if (hour < 17) return 'afternoon';
    return 'evening';
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  loadStats(): void {
    this.loading = true;
    this.sellerService.getDashboardStats().subscribe({
      next: s => { 
        this.stats = s; 
        this.loadActionableOrders();
      },
      error: () => { 
        this.loading = false; 
        this.toast.error('Failed to load dashboard'); 
      }
    });
  }

  loadActionableOrders(): void {
    // Fetch specifically orders that are CONFIRMED and await packing
    this.sellerService.getOrders({status: 'CONFIRMED', size: 1}).subscribe({
      next: (res) => {
        // The API returns actionRequiredCount or we can use statusCounts.CONFIRMED or totalElements
        this.actionRequiredCount = res.actionRequiredCount || res.statusCounts?.CONFIRMED || res.totalOrders || 0;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
