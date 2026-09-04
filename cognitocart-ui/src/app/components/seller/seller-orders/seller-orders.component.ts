import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SellerService } from '../../../services/seller.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-seller-orders',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="command-center">
      <div class="cc-header">
        <div class="header-left">
          <div class="greeting-block">
            <h1 style="display:flex; align-items:center; gap:12px;">
              <div class="kpi-icon kpi-icon-blue">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:24px;height:24px;color:var(--blue-400);"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z" /></svg>
              </div>
              Order Management
            </h1>
            <p>Process customer orders and manage fulfillments.</p>
          </div>
        </div>
        <div class="header-right">
          <button class="btn btn-primary cc-btn" (click)="load()">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width:18px;height:18px;"><path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" /></svg>
            Refresh
          </button>
        </div>
      </div>

      <!-- Filters -->
      <div class="cc-panel" style="margin-bottom:32px;">
        <div class="panel-body" style="padding: 24px; display:flex; gap:16px; flex-wrap:wrap; align-items:flex-end;">
          <div class="form-group" style="margin:0; flex:1; min-width:200px;">
            <label class="form-label">Status Filter</label>
            <select [(ngModel)]="statusFilter" class="form-select" (change)="load()">
              <option value="">All Statuses</option>
              <option *ngFor="let s of statuses" [value]="s">{{s}}</option>
            </select>
          </div>
          <div class="form-group" style="margin:0; flex:1; min-width:200px;">
            <label class="form-label">Search Order #</label>
            <input type="text" [(ngModel)]="orderSearch" class="form-input" placeholder="ORD-20260820-XXXXX" />
          </div>
          <button class="btn btn-secondary" style="height: 44px;" (click)="searchOrder()">Search</button>
          <button class="btn btn-ghost" style="height: 44px;" (click)="resetSearch()">Reset</button>
        </div>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div class="cc-panel" *ngIf="!loading">
        <div class="panel-header border-bottom">
          <h2>
            <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width:20px;height:20px;color:var(--indigo-400);"><path stroke-linecap="round" stroke-linejoin="round" d="M8.25 6.75h12M8.25 12h12m-12 5.25h12M3.75 6.75h.007v.008H3.75V6.75Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0ZM3.75 12h.007v.008H3.75V12Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm-.375 5.25h.007v.008H3.75v-.008Zm.375 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" /></svg>
            Orders List
            <span class="badge badge-blue" style="margin-left: 8px;">{{orders.length}} Orders</span>
          </h2>
        </div>
        
        <div class="cc-table-wrapper" *ngIf="orders.length > 0">
          <table class="cc-table">
            <thead>
              <tr>
                <th>Order #</th>
                <th>Customer</th>
                <th>Date</th>
                <th>Items</th>
                <th class="text-right">Total</th>
                <th>Status</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let o of orders">
                <td><code style="color:var(--primary); font-size:13px; background:var(--glass-sm); padding:4px 8px; border-radius:4px;">{{o.orderNumber}}</code></td>
                <td class="font-medium text-white">{{o.customerFirstName || '&#8212;'}}</td>
                <td>{{o.orderDate | date:'mediumDate'}}</td>
                <td>{{o.myItems?.length || '&#8212;'}}</td>
                <td class="text-right"><strong class="text-white">\u20B9{{o.myItemsNetTotal | number:'1.0-0'}}</strong></td>
                <td><span class="badge" [class]="'status-' + o.orderStatus">{{o.orderStatus}}</span></td>
                <td>
                  <div style="display:flex; gap:6px; justify-content:flex-end;">
                    <button *ngIf="o.orderStatus === 'CONFIRMED'" class="btn btn-success btn-sm" (click)="pack(o)">
                      <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;margin-right:4px;"><path stroke-linecap="round" stroke-linejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" /></svg>
                      Pack
                    </button>
                    <button class="btn btn-secondary btn-sm" (click)="viewDetail(o)">Details</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="cc-empty-state" *ngIf="orders.length === 0" style="margin: 24px; border: none; background: transparent;">
          <div class="empty-icon-ring" style="background: rgba(255,255,255,0.05); border-color: rgba(255,255,255,0.1); color: var(--text-muted);">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007ZM8.625 10.5a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm7.5 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" /></svg>
          </div>
          <h3>No Orders Found</h3>
          <p>You don't have any orders matching the current filters.</p>
        </div>

        <!-- Pagination -->
        <div class="panel-body border-top" style="padding:16px 24px; border-top: 1px solid var(--border-subtle); display:flex; justify-content:center;" *ngIf="totalPages > 1">
          <div class="pagination">
            <button class="page-btn" (click)="goPage(page-1)" [disabled]="page === 0">←</button>
            <button *ngFor="let p of pageArr" class="page-btn" [class.active]="p === page" (click)="goPage(p)">{{p+1}}</button>
            <button class="page-btn" (click)="goPage(page+1)" [disabled]="page >= totalPages-1">&#8594;</button>
          </div>
        </div>
      </div>

      <!-- Order Detail Modal -->
      <div class="modal-backdrop" *ngIf="selectedOrder" (click)="selectedOrder = null">
        <div class="modal" style="border: 1px solid var(--border-default); border-radius: var(--r-xl); background: var(--bg-elevated); box-shadow: var(--shadow-xl);" (click)="$event.stopPropagation()">
          <div class="modal-header" style="border-bottom: 1px solid var(--border-subtle); padding: 20px 24px;">
            <span class="modal-title" style="display:flex; align-items:center; gap:8px; font-family: var(--font-head); font-weight: 700; font-size: 1.2rem;">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width:20px;height:20px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" /></svg>
              Order Details: {{selectedOrder.orderNumber}}
            </span>
            <button class="btn-icon" (click)="selectedOrder = null">✕</button>
          </div>
          <div class="modal-body" style="padding: 24px;">
            <div *ngFor="let item of selectedOrder.myItems" style="display:flex; gap:12px; padding:12px 0; border-bottom:1px solid var(--border-subtle);">
              <div style="flex:1;">
                <div style="font-weight:600; color: var(--text-primary);">{{item.productName}}</div>
                <div style="color:var(--text-muted); font-size:13px; margin-top: 4px;">Variant: {{item.variantLabel || 'Standard'}} &nbsp;|&nbsp; Qty: {{item.quantity}}</div>
              </div>
              <div style="font-weight:700; color:var(--primary); font-family: var(--font-mono); font-size: 15px;">\u20B9{{item.lineTotal | number:'1.0-0'}}</div>
            </div>
            <div style="padding-top:20px; font-family: var(--font-head); font-size:1.2rem; font-weight:800; color:var(--text-primary); text-align:right;">
              Total: <span style="color: var(--primary);">\u20B9{{selectedOrder.myItemsNetTotal | number:'1.0-0'}}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class SellerOrdersComponent implements OnInit {
  orders: any[] = [];
  loading = true;
  statusFilter = '';
  orderSearch = '';
  page = 0;
  totalPages = 0;
  selectedOrder: any = null;
  statuses = ['PAYMENT_PENDING', 'CONFIRMED', 'PACKED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'RETURNED'];

  get pageArr(): number[] { return Array.from({ length: Math.min(this.totalPages, 7) }, (_, i) => i); }

  constructor(private sellerService: SellerService, private toast: ToastService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.sellerService.getOrders({ status: this.statusFilter || undefined, page: this.page, size: 20 }).subscribe({
      next: (res) => {
        this.orders = res.orders ?? res.content ?? res ?? [];
        this.totalPages = res.totalPages ?? 1;
        this.loading = false;
      },
      error: () => { this.loading = false; this.toast.error('Failed to load orders'); }
    });
  }

  searchOrder(): void {
    if (!this.orderSearch) { this.load(); return; }
    this.loading = true;
    this.sellerService.searchOrder(this.orderSearch).subscribe({
      next: (o) => { this.orders = o ? [o] : []; this.totalPages = 1; this.loading = false; },
      error: () => { this.loading = false; this.toast.error('Order not found'); }
    });
  }

  resetSearch(): void { this.orderSearch = ''; this.statusFilter = ''; this.page = 0; this.load(); }

  pack(order: any): void {
    const id = order.orderNumber || order.orderPublicId || order.publicId;
    this.sellerService.markAsPacked(id).subscribe({
      next: () => { this.toast.success('Marked as PACKED!'); order.orderStatus = 'PACKED'; },
      error: (e) => this.toast.error(e.error?.message || 'Failed')
    });
  }

  viewDetail(order: any): void { this.selectedOrder = order; }

  goPage(p: number): void { this.page = p; this.load(); }
}
