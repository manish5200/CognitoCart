import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { OrderService } from '../../../services/order.service';
import { WishlistService } from '../../../services/wishlist.service';

@Component({
  selector: 'app-customer-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <!-- Welcome Banner -->
      <div class="card" style="margin-bottom:32px; background:linear-gradient(135deg, rgba(59,130,246,0.12) 0%, rgba(139,92,246,0.08) 100%); border-color:rgba(96,165,250,0.25);">
        <div class="card-body" style="padding:28px 32px;">
          <div style="display:flex; align-items:center; gap:20px; flex-wrap:wrap;">
            <div style="width:64px; height:64px; border-radius:50%; background:var(--grad-brand); display:flex; align-items:center; justify-content:center; font-size:28px; font-weight:800; color:#fff; flex-shrink:0;">
              {{userName[0].toUpperCase()}}
            </div>
            <div style="flex:1;">
              <h1 style="font-family:var(--font-head); font-size:1.5rem; font-weight:800; color:var(--text-primary); margin-bottom:4px; display:flex; align-items:center; gap:8px;">
                Welcome back, {{userName}}!
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:24px;height:24px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="M15.182 15.182a4.5 4.5 0 0 1-6.364 0M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0ZM9.75 9.75c0 .414-.168.75-.375.75S9 10.164 9 9.75 9.168 9 9.375 9s.375.336.375.75Zm-.375 0h.008v.015h-.008V9.75Zm5.625 0c0 .414-.168.75-.375.75s-.375-.336-.375-.75.168-.75.375-.75.375.336.375.75Zm-.375 0h.008v.015h-.008V9.75Z" /></svg>
              </h1>
              <p style="color:var(--text-muted); font-size:14px;">Here's an overview of your shopping activity.</p>
            </div>
            <div style="display:flex; gap:10px;">
              <a routerLink="/products" class="btn btn-primary">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;margin-right:6px;"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007ZM8.625 10.5a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm7.5 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" /></svg>
                Shop Now
              </a>
              <a routerLink="/orders" class="btn btn-secondary">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;margin-right:6px;"><path stroke-linecap="round" stroke-linejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" /></svg>
                My Orders
              </a>
            </div>
          </div>
        </div>
      </div>

      <!-- KPIs -->
      <div class="grid-4" style="margin-bottom:32px;">
        <div class="kpi-card kpi-card-blue fade-up">
          <div class="kpi-icon kpi-icon-blue"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:28px;height:28px;"><path stroke-linecap="round" stroke-linejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" /></svg></div>
          <div class="kpi-content">
            <div class="kpi-value">{{summary.totalOrders}}</div>
            <div class="kpi-label">Total Orders</div>
          </div>
        </div>
        <div class="kpi-card kpi-card-green fade-up fade-up-delay-1">
          <div class="kpi-icon kpi-icon-green"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:28px;height:28px;"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg></div>
          <div class="kpi-content">
            <div class="kpi-value">{{summary.deliveredOrders}}</div>
            <div class="kpi-label">Delivered</div>
          </div>
        </div>
        <div class="kpi-card kpi-card-amber fade-up fade-up-delay-2">
          <div class="kpi-icon kpi-icon-amber"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:28px;height:28px;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg></div>
          <div class="kpi-content">
            <div class="kpi-value">{{summary.pendingOrders}}</div>
            <div class="kpi-label">In Progress</div>
          </div>
        </div>
        <div class="kpi-card kpi-card-purple fade-up fade-up-delay-3">
          <div class="kpi-icon kpi-icon-purple"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:28px;height:28px;"><path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" /></svg></div>
          <div class="kpi-content">
            <div class="kpi-value">{{summary.wishlistCount}}</div>
            <div class="kpi-label">Wishlist Items</div>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="grid-4" style="margin-bottom:32px;">
        <a routerLink="/orders" class="card" style="text-decoration:none; cursor:pointer;" onmouseenter="this.style.transform='translateY(-4px)'; this.style.borderColor='rgba(96,165,250,0.4)'" onmouseleave="this.style.transform='none'; this.style.borderColor='var(--border-default)'">
          <div class="card-body" style="text-align:center; padding:24px 16px;">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:32px;height:32px;margin-bottom:10px;color:var(--blue-400);"><path stroke-linecap="round" stroke-linejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" /></svg>
            <div style="font-weight:700; color:var(--text-primary);">My Orders</div>
            <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">Track, return, review</div>
          </div>
        </a>
        <a routerLink="/wishlist" class="card" style="text-decoration:none; cursor:pointer;" onmouseenter="this.style.transform='translateY(-4px)'; this.style.borderColor='rgba(248,113,113,0.4)'" onmouseleave="this.style.transform='none'; this.style.borderColor='var(--border-default)'">
          <div class="card-body" style="text-align:center; padding:24px 16px;">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:32px;height:32px;margin-bottom:10px;color:var(--red-400);"><path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" /></svg>
            <div style="font-weight:700; color:var(--text-primary);">Wishlist</div>
            <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">{{summary.wishlistCount}} saved items</div>
          </div>
        </a>
        <a routerLink="/cart" class="card" style="text-decoration:none; cursor:pointer;" onmouseenter="this.style.transform='translateY(-4px)'; this.style.borderColor='rgba(139,92,246,0.4)'" onmouseleave="this.style.transform='none'; this.style.borderColor='var(--border-default)'">
          <div class="card-body" style="text-align:center; padding:24px 16px;">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:32px;height:32px;margin-bottom:10px;color:var(--purple-400);"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z" /></svg>
            <div style="font-weight:700; color:var(--text-primary);">Cart</div>
            <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">Continue shopping</div>
          </div>
        </a>
        <a routerLink="/profile" class="card" style="text-decoration:none; cursor:pointer;" onmouseenter="this.style.transform='translateY(-4px)'; this.style.borderColor='rgba(52,211,153,0.4)'" onmouseleave="this.style.transform='none'; this.style.borderColor='var(--border-default)'">
          <div class="card-body" style="text-align:center; padding:24px 16px;">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:32px;height:32px;margin-bottom:10px;color:var(--green-400);"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z" /></svg>
            <div style="font-weight:700; color:var(--text-primary);">Profile</div>
            <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">Addresses & settings</div>
          </div>
        </a>
      </div>

      <!-- Recent Orders -->
      <div class="table-wrapper" *ngIf="recentOrders.length > 0">
        <div class="table-toolbar">
          <span class="table-title">Recent Orders</span>
          <a routerLink="/orders" class="btn btn-ghost btn-sm">View All →</a>
        </div>
        <table>
          <thead>
            <tr><th>Order #</th><th>Date</th><th>Amount</th><th>Status</th><th>Action</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let o of recentOrders">
              <td><span class="td-mono">{{o.orderNumber}}</span></td>
              <td style="color:var(--text-muted); font-size:13px;">{{o.orderDate | date:'mediumDate'}}</td>
              <td><strong>₹{{o.totalAmount | number:'1.0-0'}}</strong></td>
              <td><span class="badge" [class]="'status-' + o.status">{{o.status}}</span></td>
              <td>
                <a [routerLink]="['/orders', o.orderNumber || o.orderPublicId || o.publicId || o.id]" class="btn btn-ghost btn-sm">View →</a>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="loading-center" *ngIf="loading">
        <div class="spinner"></div>
      </div>
    </div>
  `
})
export class CustomerDashboardComponent implements OnInit {
  loading = true;
  recentOrders: any[] = [];
  userName = '';
  summary = { totalOrders: 0, deliveredOrders: 0, pendingOrders: 0, wishlistCount: 0 };

  constructor(
    private auth: AuthService,
    private orderService: OrderService,
    private wishlistService: WishlistService
  ) {}

  ngOnInit(): void {
    this.userName = this.auth.getCurrentUser()?.name || 'Shopper';

    this.orderService.getMyOrders(0, 5).subscribe({
      next: (r: any) => {
        const orders = r.content ?? r ?? [];
        this.recentOrders = orders.slice(0, 5);
        this.summary.totalOrders = r.totalElements ?? orders.length;
        this.summary.deliveredOrders = orders.filter((o: any) => o.status === 'DELIVERED').length;
        this.summary.pendingOrders = orders.filter((o: any) => !['DELIVERED', 'CANCELLED'].includes(o.status)).length;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });

    this.wishlistService.getAll().subscribe({
      next: (w: any) => this.summary.wishlistCount = Array.isArray(w) ? w.length : (w.content?.length ?? 0),
      error: () => {}
    });
  }
}
