import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AdminService } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';
import { AuthService } from '../../../services/auth.service';
import { FormsModule } from '@angular/forms';
import { AdminShellComponent } from '../admin-shell/admin-shell.component';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AdminShellComponent],
  template: `
    <app-admin-shell [pendingReturnsCount]="pendingReturns" [pendingKycCount]="pendingKyc">
      <!-- Page Header -->
      <div class="page-header">
        <div>
          <h1 class="page-title" style="display:flex; align-items:center; gap:8px;">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width:32px;height:32px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg>
            Command Center
          </h1>
          <p class="page-subtitle">Real-time platform metrics & admin controls</p>
        </div>
        <button class="btn btn-secondary btn-sm" (click)="loadAll()">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;margin-right:4px;"><path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0 3.181 3.183a8.25 8.25 0 0 0 13.803-3.7M4.031 9.865a8.25 8.25 0 0 1 13.803-3.7l3.181 3.182m0-4.991v4.99" /></svg> Refresh
        </button>
      </div>

        <!-- Loading -->
        <div class="loading-center" *ngIf="loading">
          <div class="spinner spinner-lg"></div>
          <div class="loading-text">Loading platform data...</div>
        </div>

        <div *ngIf="!loading">
          <!-- ── Primary KPIs ── -->
          <div class="grid-4" style="margin-bottom:24px;">
            <div class="kpi-card kpi-card-green fade-up" style="cursor:pointer;" routerLink="/admin/intelligence">
              <div class="kpi-icon kpi-icon-green"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:28px;height:28px;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg></div>
              <div class="kpi-content">
                <div class="kpi-value">\u20B9{{formatNum(data?.totalRevenue || 0)}}</div>
                <div class="kpi-label">Platform Revenue</div>
                <div class="kpi-change kpi-change-up">↑ View Details</div>
              </div>
            </div>
            <div class="kpi-card kpi-card-blue fade-up fade-up-delay-1" style="cursor:pointer;" routerLink="/admin/orders">
              <div class="kpi-icon kpi-icon-blue"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:28px;height:28px;"><path stroke-linecap="round" stroke-linejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" /></svg></div>
              <div class="kpi-content">
                <div class="kpi-value">{{(data?.successfulOrders || 0) + (data?.canceledOrders || 0)}}</div>
                <div class="kpi-label">Total Orders</div>
                <div class="kpi-change kpi-change-up">Manage →</div>
              </div>
            </div>
            <div class="kpi-card kpi-card-purple fade-up fade-up-delay-2">
              <div class="kpi-icon kpi-icon-purple"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:28px;height:28px;"><path stroke-linecap="round" stroke-linejoin="round" d="m9.75 9.75 4.5 4.5m0-4.5-4.5 4.5M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg></div>
              <div class="kpi-content">
                <div class="kpi-value">{{data?.canceledOrders || 0}}</div>
                <div class="kpi-label">Canceled Orders</div>
              </div>
            </div>
            <div class="kpi-card kpi-card-amber fade-up fade-up-delay-3" style="cursor:pointer;" routerLink="/admin/kyc">
              <div class="kpi-icon kpi-icon-amber"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:28px;height:28px;"><path stroke-linecap="round" stroke-linejoin="round" d="M13.5 21v-7.5a.75.75 0 0 1 .75-.75h3a.75.75 0 0 1 .75.75V21m-4.5 0H2.36m11.14 0H18m0 0h3.64m-1.39 0V9.349M3.75 21V9.349m0 0a3.001 3.001 0 0 0 3.75-.615A2.993 2.993 0 0 0 9.75 9.75c.896 0 1.7-.393 2.25-1.016a2.993 2.993 0 0 0 2.25 1.016c.896 0 1.7-.393 2.25-1.015a3.001 3.001 0 0 0 3.75.614m-16.5 0a3.004 3.004 0 0 1-.621-4.72l1.189-1.19A1.5 1.5 0 0 1 5.378 3h13.243a1.5 1.5 0 0 1 1.06.44l1.19 1.189a3 3 0 0 1-.621 4.72M6.75 18h3.75a.75.75 0 0 0 .75-.75V13.5a.75.75 0 0 0-.75-.75H6.75a.75.75 0 0 0-.75.75v3.75c0 .414.336.75.75.75Z" /></svg></div>
              <div class="kpi-content">
                <div class="kpi-value">{{totalSellers}}</div>
                <div class="kpi-label">Active Sellers</div>
                <div class="kpi-change kpi-change-up" *ngIf="pendingKyc > 0">{{pendingKyc}} pending KYC</div>
              </div>
            </div>
          </div>

          <!-- ── Alert KPIs ── -->
          <div class="grid-4" style="margin-bottom:24px;">
            <div class="kpi-card kpi-card-red" style="cursor:pointer;" routerLink="/admin/returns">
              <div class="kpi-icon kpi-icon-red"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:28px;height:28px;"><path stroke-linecap="round" stroke-linejoin="round" d="M9 15 3 9m0 0 6-6M3 9h12a6 6 0 0 1 0 12h-3" /></svg></div>
              <div class="kpi-content">
                <div class="kpi-value" [style.color]="pendingReturns > 0 ? 'var(--red-400)' : 'var(--text-primary)'">{{pendingReturns}}</div>
                <div class="kpi-label">Pending Returns</div>
                <div class="kpi-change kpi-change-down" *ngIf="pendingReturns > 0">Action required!</div>
              </div>
            </div>
            <div class="kpi-card kpi-card-purple" style="cursor:pointer;" routerLink="/admin/kyc">
              <div class="kpi-icon kpi-icon-purple"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:28px;height:28px;"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg></div>
              <div class="kpi-content">
                <div class="kpi-value" [style.color]="pendingKyc > 0 ? 'var(--amber-400)' : 'var(--text-primary)'">{{pendingKyc}}</div>
                <div class="kpi-label">KYC Awaiting</div>
                <div class="kpi-change kpi-change-down" *ngIf="pendingKyc > 0">Review needed</div>
              </div>
            </div>
            <div class="kpi-card kpi-card-blue">
              <div class="kpi-icon kpi-icon-blue"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:28px;height:28px;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg></div>
              <div class="kpi-content">
                <div class="kpi-value">\u20B9{{formatNum(getAov())}}</div>
                <div class="kpi-label">Avg Order Value</div>
              </div>
            </div>
            <div class="kpi-card kpi-card-amber" style="cursor:pointer;" routerLink="/admin/webhooks">
              <div class="kpi-icon kpi-icon-amber"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:28px;height:28px;"><path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" /></svg></div>
              <div class="kpi-content">
                <div class="kpi-value">{{webhookCount}}</div>
                <div class="kpi-label">DLQ Failures</div>
                <div class="kpi-change kpi-change-down" *ngIf="webhookCount > 0">Needs replay</div>
              </div>
            </div>
          </div>

          <!-- ── Quick Action Cards ── -->
          <div style="margin-bottom:28px;">
            <h3 style="font-size:13px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.08em; margin-bottom:14px;">Quick Actions</h3>
            <div class="grid-4">
              <a routerLink="/admin/orders" class="card" style="cursor:pointer; text-decoration:none; transition:var(--transition-slow);" onmouseenter="this.style.transform='translateY(-5px)'; this.style.borderColor='rgba(96,165,250,0.4)'" onmouseleave="this.style.transform='none'; this.style.borderColor='var(--border-default)'">
                <div class="card-body" style="text-align:center; padding:20px 14px;">
                  <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:32px;height:32px;margin-bottom:8px;color:var(--blue-400);"><path stroke-linecap="round" stroke-linejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" /></svg>
                  <div style="font-weight:700; font-size:13px; color:var(--text-primary);">Manage Orders</div>
                  <div style="font-size:11px; color:var(--text-muted); margin-top:4px;">Status · Shipment · Invoice</div>
                </div>
              </a>
              <a routerLink="/admin/kyc" class="card" style="cursor:pointer; text-decoration:none; transition:var(--transition-slow);" onmouseenter="this.style.transform='translateY(-5px)'; this.style.borderColor='rgba(139,92,246,0.4)'" onmouseleave="this.style.transform='none'; this.style.borderColor='var(--border-default)'">
                <div class="card-body" style="text-align:center; padding:20px 14px;">
                  <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:32px;height:32px;margin-bottom:8px;color:var(--purple-400);"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg>
                  <div style="font-weight:700; font-size:13px; color:var(--text-primary);">KYC Review</div>
                  <span *ngIf="pendingKyc > 0" class="badge badge-amber" style="margin-top:4px; display:inline-block;">{{pendingKyc}} awaiting</span>
                  <div *ngIf="pendingKyc === 0" style="font-size:11px; color:var(--success); margin-top:4px;">All clear ✓</div>
                </div>
              </a>
              <a routerLink="/admin/returns" class="card" style="cursor:pointer; text-decoration:none; transition:var(--transition-slow);" onmouseenter="this.style.transform='translateY(-5px)'; this.style.borderColor='rgba(248,113,113,0.4)'" onmouseleave="this.style.transform='none'; this.style.borderColor='var(--border-default)'">
                <div class="card-body" style="text-align:center; padding:20px 14px;">
                  <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:32px;height:32px;margin-bottom:8px;color:var(--red-400);"><path stroke-linecap="round" stroke-linejoin="round" d="M9 15 3 9m0 0 6-6M3 9h12a6 6 0 0 1 0 12h-3" /></svg>
                  <div style="font-weight:700; font-size:13px; color:var(--text-primary);">Return Queue</div>
                  <span *ngIf="pendingReturns > 0" class="badge badge-red" style="margin-top:4px; display:inline-block;">{{pendingReturns}} pending</span>
                  <div *ngIf="pendingReturns === 0" style="font-size:11px; color:var(--success); margin-top:4px;">Queue empty ✓</div>
                </div>
              </a>
              <a routerLink="/admin/intelligence" class="card" style="cursor:pointer; text-decoration:none; transition:var(--transition-slow);" onmouseenter="this.style.transform='translateY(-5px)'; this.style.borderColor='rgba(34,211,238,0.4)'" onmouseleave="this.style.transform='none'; this.style.borderColor='var(--border-default)'">
                <div class="card-body" style="text-align:center; padding:20px 14px;">
                  <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:32px;height:32px;margin-bottom:8px;color:var(--cyan-400);"><path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" /></svg>
                  <div style="font-weight:700; font-size:13px; color:var(--text-primary);">BI Intelligence</div>
                  <div style="font-size:11px; color:var(--text-muted); margin-top:4px;">CLV · Churn · Revenue</div>
                </div>
              </a>
            </div>
          </div>

          <!-- ── Order Status Management ── -->
          <div class="card" style="margin-bottom:24px;">
            <div class="card-body" style="padding:20px 24px;">
              <h3 style="font-weight:700; font-size:15px; color:var(--text-primary); margin-bottom:16px; display:flex; align-items:center; gap:8px;">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:20px;height:20px;color:var(--amber-400);"><path stroke-linecap="round" stroke-linejoin="round" d="m3.75 13.5 10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75Z" /></svg>
                Quick Order Status Update
              </h3>
              <div style="display:flex; gap:12px; flex-wrap:wrap; align-items:flex-end;">
                <div style="flex:1; min-width:200px;">
                  <label class="form-label">Order UUID</label>
                  <input type="text" class="form-input" [(ngModel)]="quickOrderId" placeholder="e.g. 123e4567-e89b-12d3..." />
                </div>
                <div style="min-width:160px;">
                  <label class="form-label">New Status</label>
                  <select class="form-select" [(ngModel)]="quickStatus">
                    <option value="">Select status...</option>
                    <option *ngFor="let s of orderStatuses" [value]="s">{{s}}</option>
                  </select>
                </div>
                <div style="min-width:180px;">
                  <label class="form-label">Courier (for SHIPPED)</label>
                  <input type="text" class="form-input" [(ngModel)]="shipmentForm.courier" placeholder="Delhivery / Bluedart..." />
                </div>
                <div style="min-width:160px;">
                  <label class="form-label">Tracking #</label>
                  <input type="text" class="form-input" [(ngModel)]="shipmentForm.trackingNumber" placeholder="AWB Number" />
                </div>
                <button class="btn btn-primary" (click)="runQuickUpdate()" [disabled]="!quickOrderId || !quickStatus || updatingOrder">
                  <span *ngIf="updatingOrder" class="spinner spinner-sm"></span> {{updatingOrder ? 'Updating...' : 'Update Status'}}
                </button>
                <button class="btn btn-secondary" (click)="downloadInvoice()">
                  <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:18px;height:18px;margin-right:6px;"><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" /></svg> Invoice
                </button>
              </div>
            </div>
          </div>

          <!-- ── Recent Orders Table ── -->
          <div *ngIf="recentOrders.length" class="table-wrapper" style="margin-bottom:24px;">
            <div class="table-toolbar">
              <div>
                <span class="table-title" style="display:flex; align-items:center; gap:6px;">
                  <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:18px;height:18px;"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25ZM6.75 12h.008v.008H6.75V12Zm0 3h.008v.008H6.75V15Zm0 3h.008v.008H6.75V18Z" /></svg>
                  Recent Orders
                </span>
                <span class="table-count">Last {{recentOrders.length}}</span>
              </div>
              <a routerLink="/admin/orders" class="btn btn-ghost btn-sm">View All →</a>
            </div>
            <table>
              <thead>
                <tr>
                  <th>Order #</th>
                  <th>Customer</th>
                  <th>Date</th>
                  <th>Amount</th>
                  <th>Payment</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let o of recentOrders">
                  <td><span class="td-mono">{{o.orderNumber || o.publicId?.slice(0,8)}}</span></td>
                  <td class="td-primary">{{o.customerName || o.customerEmail || '—'}}</td>
                  <td style="color:var(--text-muted); font-size:12px;">{{o.orderDate | date:'MMM d, y'}}</td>
                  <td><strong style="color:var(--text-primary);">\u20B9{{(o.totalAmount || 0) | number:'1.0-0'}}</strong></td>
                  <td>
                    <span class="badge" [class]="o.paymentStatus === 'PAID' ? 'badge-green' : 'badge-amber'">
                      {{o.paymentStatus || 'PENDING'}}
                    </span>
                  </td>
                  <td><span class="badge" [class]="'status-' + o.status">{{o.status}}</span></td>
                  <td>
                    <div class="td-actions">
                      <select
                        class="form-select"
                        style="width:140px; padding:5px 28px 5px 8px; font-size:11px;"
                        (change)="changeStatus(o, $any($event).target.value)"
                      >
                        <option value="">Change...</option>
                        <option *ngFor="let s of orderStatuses" [value]="s">{{s}}</option>
                      </select>
                      <button class="btn btn-ghost btn-xs" title="Invoice" (click)="downloadInvoiceOrder(o)">
                        <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" /></svg>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- ── Low Stock Alerts ── -->
          <div *ngIf="data?.lowStockAlerts?.length" class="table-wrapper">
            <div class="table-toolbar">
              <span class="table-title" style="display:flex; align-items:center; gap:6px;">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:18px;height:18px;color:var(--danger);"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
                Low Stock Alerts
              </span>
              <span class="badge badge-red">{{data.lowStockAlerts.length}} items</span>
            </div>
            <table>
              <thead><tr><th>Product</th><th>Variant</th><th>Stock</th><th>Status</th></tr></thead>
              <tbody>
                <tr *ngFor="let item of data.lowStockAlerts">
                  <td class="td-primary">{{item.productName}}</td>
                  <td style="color:var(--text-muted);">{{item.sku || '—'}}</td>
                  <td><span class="badge badge-red">{{item.stockQuantity}} left</span></td>
                  <td style="color:var(--text-muted);">{{item.status || 'LOW_STOCK'}}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Empty state for fresh platform -->
          <div *ngIf="!recentOrders.length && !data?.lowStockAlerts?.length" class="empty-state">
            <div class="empty-icon">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:48px;height:48px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="M15.59 14.37a6 6 0 0 1-5.84 7.38v-4.8m5.84-2.58a14.98 14.98 0 0 0 6.16-12.12A14.98 14.98 0 0 0 9.631 8.41m5.96 5.96a14.926 14.926 0 0 1-5.841 2.58m-.119-8.54a6 6 0 0 0-7.381 5.84h4.8m2.581-5.84a14.927 14.927 0 0 0-2.58 5.84m2.699 2.7c-.103.021-.207.041-.311.06a15.09 15.09 0 0 1-2.448-2.45m.31-.31c.019-.104.04-.208.06-.312m2.448 2.45-.31-.31" /></svg>
            </div>
            <div class="empty-title">Platform is starting up!</div>
            <div class="empty-subtitle">No orders or alerts yet. Use the quick actions above to explore admin features.</div>
          </div>
        </div>
    </app-admin-shell>
  `
})
export class AdminDashboardComponent implements OnInit {
  data: any = null;
  loading = true;
  pendingReturns = 0;
  pendingKyc = 0;
  webhookCount = 0;
  updatingOrder = false;

  quickOrderId = '';
  quickStatus = '';
  shipmentForm = { courier: '', trackingNumber: '', trackingUrl: '' };

  recentOrders: any[] = [];
  totalSellers = 0;

  orderStatuses = ['PAYMENT_PENDING', 'CONFIRMED', 'PACKED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];

  constructor(
    public adminService: AdminService,
    private toast: ToastService,
    public auth: AuthService
  ) {}

  ngOnInit(): void { this.loadAll(); }

  loadAll(): void {
    this.loading = true;
    this.adminService.getStats(0, 10).subscribe({
      next: (d: any) => { 
        this.data = d?.message ? null : d; // backend returns {message:...} if empty
        this.loading = false; 
      },
      error: () => { this.loading = false; this.toast.error('Failed to load dashboard stats'); }
    });
    this.adminService.getAllOrders(0, 5).subscribe({
      next: (orders: any) => this.recentOrders = orders?.content || orders || [],
      error: () => {}
    });
    this.adminService.getAllSellers().subscribe({
      next: (s: any) => this.totalSellers = Array.isArray(s) ? s.length : 0,
      error: () => {}
    });
    this.adminService.getPendingReturns().subscribe({
      next: (r: any) => this.pendingReturns = Array.isArray(r) ? r.length : (r.totalElements ?? 0),
      error: () => {}
    });
    this.adminService.getPendingKycSellers().subscribe({
      next: (s: any) => this.pendingKyc = Array.isArray(s) ? s.length : 0,
      error: () => {}
    });
    this.adminService.getPendingWebhooks().subscribe({
      next: (w: any) => this.webhookCount = Array.isArray(w) ? w.length : 0,
      error: () => {}
    });
  }

  changeStatus(order: any, event: any): void {
    const status = event.target.value;
    if (!status) return;
    const id = order.orderPublicId || order.publicId || order.orderNumber;
    if (!window.confirm(`Are you sure you want to change order ${order.orderNumber || id} status to ${status}?`)) {
      event.target.value = '';
      return;
    }
    this.adminService.changeOrderStatus(id, status).subscribe({
      next: () => {
        order.status = status;
        this.toast.success(`Status updated to ${status}`);
        event.target.value = '';
      },
      error: (e: any) => this.toast.error(e?.error?.message || 'Status update failed')
    });
  }

  runQuickUpdate(): void {
    if (this.quickOrderId && this.quickStatus) {
      if (!window.confirm(`Are you sure you want to change order ${this.quickOrderId} status to ${this.quickStatus}?`)) return;
      this.updatingOrder = true;
      this.adminService.changeOrderStatus(this.quickOrderId, this.quickStatus).subscribe({
        next: () => {
          this.toast.success('Quick update successful');
          this.resetQuickForm();
          this.loadAll();
        },
        error: (err) => {
          this.toast.error('Update failed. Ensure Order ID is valid.');
          this.updatingOrder = false;
        }
      });
    }
  }

  downloadInvoice(): void {
    if (!this.quickOrderId) { this.toast.warning('Enter an order ID first'); return; }
    this.adminService.downloadInvoice(this.quickOrderId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Invoice-${this.quickOrderId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.toast.error('Failed to download invoice')
    });
  }

  downloadInvoiceOrder(order: any): void {
    const id = order.orderPublicId || order.publicId || order.orderNumber;
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

  resetQuickForm(): void { this.quickOrderId = ''; this.quickStatus = ''; this.shipmentForm = { courier: '', trackingNumber: '', trackingUrl: '' }; }

  formatNum(n: number): string {
    if (!n) return '0';
    if (n >= 1_00_00_000) return (n / 1_00_00_000).toFixed(1) + 'Cr';
    if (n >= 1_00_000) return (n / 1_00_000).toFixed(1) + 'L';
    if (n >= 1000) return (n / 1000).toFixed(1) + 'K';
    return n.toString();
  }

  getAov(): number {
    if (!this.data || !this.data.totalRevenue || !this.data.successfulOrders) return 0;
    return this.data.totalRevenue / this.data.successfulOrders;
  }
}
