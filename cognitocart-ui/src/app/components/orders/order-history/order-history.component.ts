import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { OrderService } from '../../../services/order.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <div class="page-header">
        <h1 class="page-title">📦 My Orders</h1>
        <p class="page-subtitle">Track all your orders and manage returns</p>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div *ngIf="!loading">
        <div *ngIf="orders.length === 0" class="empty-state fade-up" style="min-height:400px;">
          <div class="empty-icon" style="font-size:3rem; margin-bottom:16px;">📦</div>
          <div class="empty-title" style="font-family:var(--font-head); font-size:1.8rem; margin-bottom:8px; color:var(--text-primary);">No orders yet</div>
          <div class="empty-subtitle" style="color:var(--text-muted); margin-bottom:24px;">Start shopping to see your orders here</div>
          <a routerLink="/products" class="btn btn-primary btn-lg">Shop Now</a>
        </div>

        <div *ngFor="let order of orders" class="order-card glass-card fade-up" style="margin-bottom:24px; border-color:var(--border-subtle);">
          <div class="card-body" style="padding:24px;">
            <div class="order-header">
              <div>
                <div class="order-id">{{order.orderNumber}}</div>
                <div style="font-size:12px; color:var(--text-dim);">{{order.orderDate | date:'medium'}}</div>
              </div>
              <div style="display:flex; align-items:center; gap:12px;">
                <span class="badge" [class]="'status-' + order.status">{{order.status}}</span>
                <span style="font-weight:700; color:var(--primary); font-size:1.1rem;">₹{{order.totalAmount | number:'1.0-0'}}</span>
              </div>
            </div>

            <!-- Order Items preview -->
            <div class="order-items-preview">
              <div *ngFor="let item of order.items?.slice(0, 3)" style="display:flex; align-items:center; gap:10px; font-size:13px; color:var(--text-muted); margin-bottom:4px;">
                <span>📦</span>
                <span>{{item.productName}} × {{item.quantity}}</span>
                <span *ngIf="item.isReturnable === false" style="background:rgba(239,68,68,0.1); color:#ef4444; padding:2px 6px; border-radius:4px; font-size:10px; font-weight:600;">Non-Returnable</span>
              </div>
              <div *ngIf="order.items?.length > 3" style="font-size:12px; color:var(--text-dim);">
                +{{order.items.length - 3}} more items
              </div>
            </div>

            <div class="order-actions">
              <a [routerLink]="['/orders', order.orderNumber || order.orderPublicId || order.publicId || order.id]" class="btn btn-secondary btn-sm">
                View Details →
              </a>
              <button *ngIf="order.status === 'PENDING' || order.status === 'CONFIRMED'"
                class="btn btn-danger btn-sm" (click)="cancelOrder(order)">
                Cancel Order
              </button>
              <button *ngIf="order.status === 'DELIVERED'"
                class="btn btn-warning btn-sm" (click)="requestReturn(order)">
                Return/Replace
              </button>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div class="pagination" *ngIf="totalPages > 1">
          <button class="page-btn" (click)="loadPage(page-1)" [disabled]="page === 0">←</button>
          <button *ngFor="let p of pageArray" class="page-btn" [class.active]="p === page" (click)="loadPage(p)">{{p+1}}</button>
          <button class="page-btn" (click)="loadPage(page+1)" [disabled]="page >= totalPages-1">→</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .order-card { transition: var(--transition); }
    .order-header { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; border-bottom: 1px solid var(--border-default); padding-bottom: 16px; }
    .order-id { font-weight: 700; color: var(--text-primary); font-size: 16px; font-family: var(--font-head); letter-spacing: 0.5px; }
    .order-items-preview { margin-bottom: 20px; padding: 16px; background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: 12px; }
    .order-actions { display: flex; gap: 12px; justify-content: flex-end; }
  `]
})
export class OrderHistoryComponent implements OnInit {
  orders: any[] = [];
  loading = true;
  page = 0;
  totalPages = 0;

  get pageArray(): number[] {
    return Array.from({ length: Math.min(this.totalPages, 7) }, (_, i) => i);
  }

  constructor(private orderService: OrderService, private toast: ToastService, private router: Router) {}

  ngOnInit(): void { this.loadPage(0); }

  loadPage(p: number): void {
    this.page = p;
    this.loading = true;
    this.orderService.getHistory(p, 10).subscribe({
      next: (res) => {
        this.orders = res.content ?? res ?? [];
        this.totalPages = res.totalPages ?? 1;
        this.loading = false;
      },
      error: () => { this.loading = false; this.toast.error('Failed to load orders'); }
    });
  }

  cancelOrder(order: any): void {
    if (!confirm('Cancel this order? You will receive a refund.')) return;
    this.orderService.cancel(order.orderNumber || order.orderPublicId || order.publicId || order.id).subscribe({
      next: () => { this.toast.success('Order cancelled. Refund initiated.'); this.loadPage(this.page); },
      error: (e) => this.toast.error(e.error?.message || 'Cannot cancel order')
    });
  }

  requestReturn(order: any): void {
    const id = order.orderNumber || order.orderPublicId || order.publicId || order.id;
    // Navigate to order detail for return flow
    this.router.navigate(['/orders', id], { fragment: 'return' });
  }
}
