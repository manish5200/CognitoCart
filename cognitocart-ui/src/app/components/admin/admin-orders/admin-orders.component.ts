import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

import { AdminShellComponent } from '../admin-shell/admin-shell.component';

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminShellComponent],
  template: `
    <app-admin-shell>
      <div class="page-header" style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <h1 class="page-title" style="display:flex; align-items:center; gap:12px;">
            <div style="background:var(--grad-brand); border-radius:8px; padding:6px; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 14px rgba(99,102,241,0.3);">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21.64 3.64-1.28-1.28A2 2 0 0 0 18.94 2h-13.9a2 2 0 0 0-1.42.59L2.36 3.86a2 2 0 0 0-.36.78v15.36A2 2 0 0 0 4 22h16a2 2 0 0 0 2-2V4.64a2 2 0 0 0-.36-.78"/><path d="m22 6-20 0"/><path d="M12 12v.01"/></svg>
            </div>
            Order Management
          </h1>
          <p class="page-subtitle">Full control over all platform orders</p>
        </div>
        <button class="btn btn-secondary btn-sm" (click)="loadOrders()" style="display:flex; align-items:center; gap:8px;">
          <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/></svg>
          Refresh
        </button>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div *ngIf="!loading">
        <!-- Order Table -->
        <div class="table-wrapper" style="box-shadow:0 8px 32px rgba(0,0,0,0.4); border:1px solid rgba(255,255,255,0.05); border-radius:12px;">
          <table style="width:100%; border-collapse:collapse;">
            <thead style="background:rgba(255,255,255,0.02); border-bottom:1px solid rgba(255,255,255,0.08);">
              <tr>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Order #</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Customer</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Date</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Amount</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Payment</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Status</th>
                <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let o of orders" style="border-bottom:1px solid rgba(255,255,255,0.04); transition:background 0.2s;" onmouseover="this.style.background='rgba(255,255,255,0.02)'" onmouseout="this.style.background='transparent'">
                <td style="padding:16px;">
                  <code style="color:var(--primary); font-size:13px; background:rgba(99,102,241,0.1); padding:4px 8px; border-radius:4px; font-weight:600;">{{o.orderNumber || o.publicId?.slice(0,8)}}</code>
                </td>
                <td style="padding:16px; font-size:14px; font-weight:500; color:#fff;">{{o.customerName || o.customerEmail || '—'}}</td>
                <td style="padding:16px; font-size:13px; color:var(--text-secondary);">{{o.orderDate | date:'MMM d, yyyy'}}</td>
                <td style="padding:16px; font-size:14px;"><strong style="color:var(--text-primary);">₹{{o.totalAmount | number:'1.0-0'}}</strong></td>
                <td style="padding:16px;">
                  <span class="badge" [class]="o.paymentStatus === 'PAID' ? 'badge-green' : 'badge-amber'" style="padding:4px 10px; font-size:11px;">
                    {{o.paymentStatus || 'PENDING'}}
                  </span>
                </td>
                <td style="padding:16px;"><span class="badge" [class]="'status-' + o.status" style="padding:4px 10px; font-size:11px;">{{o.status}}</span></td>
                <td style="padding:16px;">
                  <div style="display:flex; gap:8px; align-items:center;">
                    <select class="form-select" style="width:140px; padding:6px 10px; font-size:12px; border-radius:6px;" (change)="changeStatus(o, $event)">
                      <option value="">Update Status</option>
                      <option *ngFor="let s of statuses" [value]="s">{{s}}</option>
                    </select>
                    <button class="btn btn-secondary btn-sm" (click)="openShipment(o)" title="Attach Shipment" style="padding:6px; display:flex; align-items:center; justify-content:center;">
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 18H3c-.6 0-1-.4-1-1V7c0-.6.4-1 1-1h10c.6 0 1 .4 1 1v11"/><path d="M14 9h4l4 4v4c0 .6-.4 1-1 1h-2"/><circle cx="7" cy="18" r="2"/><circle cx="17" cy="18" r="2"/></svg>
                    </button>
                    <button (click)="downloadInvoice(o)" class="btn btn-ghost btn-sm" title="Invoice" style="padding:6px; display:flex; align-items:center; justify-content:center;">
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><line x1="16" x2="8" y1="13" y2="13"/><line x1="16" x2="8" y1="17" y2="17"/><line x1="10" x2="8" y1="9" y2="9"/></svg>
                    </button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="orders.length === 0">
                <td colspan="7" style="text-align:center; padding:64px 20px;">
                  <div style="color:var(--text-muted); font-size:16px;">No orders found</div>
                  <div style="color:var(--text-dim); font-size:13px; margin-top:8px;">Orders placed on the platform will appear here.</div>
                </td>
              </tr>
            </tbody>
          </table>
          
          <!-- Pagination -->
          <div style="display:flex; justify-content:space-between; align-items:center; padding:16px; border-top:1px solid rgba(255,255,255,0.05); background:rgba(255,255,255,0.01);">
            <div style="font-size:13px; color:var(--text-muted);">
              Showing Page {{page + 1}} ({{orders.length}} records)
            </div>
            <div style="display:flex; gap:12px;">
              <button class="btn btn-secondary btn-sm" [disabled]="page === 0" (click)="prevPage()">← Previous</button>
              <button class="btn btn-secondary btn-sm" [disabled]="orders.length < size" (click)="nextPage()">Next →</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Shipment Modal -->
      <div class="modal-backdrop" *ngIf="shipmentOrder" (click)="shipmentOrder = null">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <span class="modal-title">🚚 Attach Shipment — {{shipmentOrder?.orderNumber}}</span>
            <button class="btn-icon" (click)="shipmentOrder = null">✕</button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label">Courier Name *</label>
              <input type="text" [(ngModel)]="shipment.courier" class="form-input" placeholder="BlueDart, DTDC, Delhivery..." />
            </div>
            <div class="form-group">
              <label class="form-label">Tracking Number *</label>
              <input type="text" [(ngModel)]="shipment.trackingNumber" class="form-input" placeholder="AWB1234567890" />
            </div>
            <div class="form-group">
              <label class="form-label">Tracking URL</label>
              <input type="text" [(ngModel)]="shipment.trackingUrl" class="form-input" placeholder="https://..." />
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" (click)="shipmentOrder = null">Cancel</button>
            <button class="btn btn-primary" (click)="attachShipment()" [disabled]="attaching">
              <span *ngIf="!attaching" style="display:flex; align-items:center; gap:4px;">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" /></svg>
                Attach & Ship
              </span>
              <span *ngIf="attaching">Saving...</span>
            </button>
          </div>
        </div>
      </div>
    </app-admin-shell>
  `,
  styles: []
})
export class AdminOrdersComponent implements OnInit {
  orders: any[] = [];
  loading = true;
  shipmentOrder: any = null;
  attaching = false;
  shipment = { courier: '', trackingNumber: '', trackingUrl: '' };
  statuses = ['PAYMENT_PENDING', 'CONFIRMED', 'PACKED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];

  page = 0;
  size = 15;

  constructor(private adminService: AdminService, private toast: ToastService) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.adminService.getAllOrders(this.page, this.size).subscribe({
      next: (orders: any) => { this.orders = orders.content || orders || []; this.loading = false; },
      error: () => { this.loading = false; this.toast.error('Failed to load orders'); }
    });
  }

  nextPage(): void {
    if (this.orders.length === this.size) {
      this.page++;
      this.loadOrders();
    }
  }

  prevPage(): void {
    if (this.page > 0) {
      this.page--;
      this.loadOrders();
    }
  }

  downloadInvoice(order: any): void {
    const id = order.orderNumber || order.orderPublicId || order.publicId;
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

  changeStatus(order: any, event: any): void {
    const status = event.target.value;
    if (!status) return;
    const id = order.orderPublicId || order.publicId || order.orderNumber;
    if (!window.confirm(`Are you sure you want to change order ${order.orderNumber || id} status to ${status}? This action may send customer notifications.`)) {
      event.target.value = '';
      return;
    }
    this.adminService.changeOrderStatus(id, status).subscribe({
      next: () => { order.status = status; this.toast.success(`Status → ${status}`); event.target.value = ''; },
      error: (e) => { this.toast.error(e.error?.message || 'Update failed'); event.target.value = ''; }
    });
  }

  openShipment(order: any): void {
    this.shipmentOrder = order;
    this.shipment = { courier: '', trackingNumber: '', trackingUrl: '' };
  }

  attachShipment(): void {
    if (!this.shipment.courier || !this.shipment.trackingNumber) { this.toast.warning('Courier and tracking # required'); return; }
    const id = this.shipmentOrder.orderPublicId || this.shipmentOrder.publicId || this.shipmentOrder.orderNumber;
    if (!window.confirm(`Are you sure you want to attach shipment for order ${this.shipmentOrder.orderNumber || id}? This will mark it as SHIPPED.`)) {
      return;
    }
    this.attaching = true;
    this.adminService.attachShipment(id, this.shipment).subscribe({
      next: () => {
        this.toast.success('Shipment attached! Order marked as SHIPPED.');
        this.shipmentOrder.status = 'SHIPPED';
        this.attaching = false;
        this.shipmentOrder = null;
      },
      error: (e) => { this.attaching = false; this.toast.error(e.error?.message || 'Failed'); }
    });
  }
}
