import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../services/admin.service';
import { SaleService } from '../../../services/sale.service';
import { ToastService } from '../../../services/toast.service';

import { AdminShellComponent } from '../admin-shell/admin-shell.component';

@Component({
  selector: 'app-admin-sales',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminShellComponent],
  template: `
    <app-admin-shell>
      <div class="page-header" style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <h1 class="page-title" style="display:flex; align-items:center; gap:12px;">
            <div style="background:var(--grad-brand); border-radius:8px; padding:6px; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 14px rgba(99,102,241,0.3);">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 2L2 14h9l-1 8 9-12h-9z"/></svg>
            </div>
            Flash Sale Events
          </h1>
          <p class="page-subtitle">Create and manage platform-wide flash sale events</p>
        </div>
        <button class="btn btn-primary" (click)="showCreate = !showCreate" style="display:flex; align-items:center; gap:6px;">
          <svg *ngIf="showCreate" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" /></svg>
          <svg *ngIf="!showCreate" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" /></svg>
          {{showCreate ? 'Cancel' : 'Create Event'}}
        </button>
      </div>

      <!-- Create Event Form -->
      <div class="card" *ngIf="showCreate" style="margin-bottom:24px;">
        <div class="card-body">
          <h3 style="margin-bottom:20px;">New Flash Sale Event</h3>
          <div class="form-group">
            <label class="form-label">Event Name *</label>
            <input type="text" [(ngModel)]="newEvent.eventName" class="form-input" placeholder="Diwali Mega Sale 2026" />
          </div>
          <div class="form-group">
            <label class="form-label">Description</label>
            <input type="text" [(ngModel)]="newEvent.description" class="form-input" placeholder="Up to 70% off on all electronics" />
          </div>
          <div class="form-row form-row-2">
            <div class="form-group">
              <label class="form-label">Start Time *</label>
              <input type="datetime-local" [(ngModel)]="newEvent.startTime" class="form-input" />
            </div>
            <div class="form-group">
              <label class="form-label">End Time *</label>
              <input type="datetime-local" [(ngModel)]="newEvent.endTime" class="form-input" />
            </div>
          </div>
          <button class="btn btn-primary" (click)="createEvent()" [disabled]="creating" style="display:flex; align-items:center; gap:6px;">
            <svg *ngIf="!creating" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" /></svg>
            {{creating ? 'Creating...' : 'Create Event'}}
          </button>
        </div>
      </div>

      <!-- Events Table -->
      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div *ngIf="!loading" class="grid-2">
        <!-- Events -->
        <div>
          <div class="table-wrapper" style="box-shadow:0 8px 32px rgba(0,0,0,0.4); border:1px solid rgba(255,255,255,0.05); border-radius:12px;">
            <div class="table-header" style="background:rgba(255,255,255,0.02); border-bottom:1px solid rgba(255,255,255,0.08);">
              <span class="table-title">Sale Events ({{events.length}})</span>
            </div>
            <table style="width:100%; border-collapse:collapse;">
              <thead style="background:rgba(255,255,255,0.02); border-bottom:1px solid rgba(255,255,255,0.08);">
                <tr>
                  <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Event</th>
                  <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Start</th>
                  <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">End</th>
                  <th style="padding:16px; text-align:left; font-size:12px; font-weight:700; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em;">Status</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let e of events" (click)="selectEvent(e)" style="cursor:pointer; border-bottom:1px solid rgba(255,255,255,0.04); transition:background 0.2s;" [style.background]="selectedEvent === e ? 'rgba(99,102,241,0.1)' : ''" onmouseover="this.style.background='rgba(255,255,255,0.02)'" onmouseout="this.style.background=''">
                  <td style="padding:16px;">
                    <div style="font-weight:600; color:#fff;">{{e.eventName}}</div>
                    <div style="font-size:12px; color:var(--text-dim);">{{e.description}}</div>
                  </td>
                  <td style="padding:16px; font-size:13px; color:var(--text-secondary);">{{e.startTime | date:'mediumDate'}}</td>
                  <td style="padding:16px; font-size:13px; color:var(--text-secondary);">{{e.endTime | date:'mediumDate'}}</td>
                  <td style="padding:16px;">
                    <span class="badge" [class]="getEventBadge(e.status)" style="padding:4px 10px; font-size:11px;">{{e.status}}</span>
                  </td>
                </tr>
                <tr *ngIf="events.length === 0">
                  <td colspan="4" style="text-align:center; padding:32px; color:var(--text-muted);">No events yet</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Seller Submissions -->
        <div *ngIf="selectedEvent">
          <div class="card">
            <div class="card-body">
              <h3 style="margin-bottom:16px; display:flex; align-items:center; gap:8px;">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:20px;height:20px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 17.25v3.375c0 .621-.504 1.125-1.125 1.125h-9.75a1.125 1.125 0 0 1-1.125-1.125V7.875c0-.621.504-1.125 1.125-1.125H6.75a9.06 9.06 0 0 1 1.5.124m7.5 10.376h3.375c.621 0 1.125-.504 1.125-1.125V11.25c0-4.46-3.243-8.161-7.5-8.876a9.06 9.06 0 0 0-1.5-.124H9.375c-.621 0-1.125.504-1.125 1.125v3.5m7.5 10.375H9.375a1.125 1.125 0 0 1-1.125-1.125v-9.25m12 6.625v-1.875a3.375 3.375 0 0 0-3.375-3.375h-1.5a1.125 1.125 0 0 1-1.125-1.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H9.75" /></svg>
                Submissions &#8212; {{selectedEvent.eventName}}
              </h3>
              <div class="loading-center" *ngIf="loadingSubmissions"><div class="spinner spinner-sm"></div></div>
              <div *ngFor="let s of submissions" style="padding:16px; border:1px solid rgba(255,255,255,0.08); border-radius:12px; margin-bottom:12px; background:rgba(255,255,255,0.02);">
                <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                  <div>
                    <div style="font-weight:600; color:#fff; font-size:14px; margin-bottom:4px;">{{s.sku || s.variantName || s.productName}}</div>
                    <div style="font-size:13px; color:var(--text-secondary);">Discount: <strong style="color:var(--text-primary);">{{s.discountPercentage}}%</strong></div>
                    <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">Max: {{s.maxUnits}} units | Per user: {{s.maxUnitsPerUser}}</div>
                  </div>
                  <span class="badge" [class]="getSubmBadge(s.approvalStatus)" style="padding:4px 10px; font-size:11px;">{{s.approvalStatus}}</span>
                </div>
                <div *ngIf="s.approvalStatus === 'PENDING'" style="display:flex; gap:8px; margin-top:16px;">
                  <button class="btn btn-success btn-sm" (click)="reviewItem(s, 'APPROVED')" style="display:flex; align-items:center; gap:4px;">
                    <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" /></svg>
                    Approve
                  </button>
                  <button class="btn btn-danger btn-sm" (click)="reviewItem(s, 'REJECTED')" style="display:flex; align-items:center; gap:4px;">
                    <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" /></svg>
                    Reject
                  </button>
                </div>
              </div>
              <div *ngIf="!loadingSubmissions && submissions.length === 0" style="text-align:center; padding:32px; color:var(--text-muted);">
                No submissions for this event
              </div>
            </div>
          </div>
        </div>

        <div *ngIf="!selectedEvent" class="empty-state" style="min-height:200px; display:flex; flex-direction:column; align-items:center; justify-content:center; color:var(--text-muted);">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:48px;height:48px;margin-bottom:16px;color:var(--text-dim);"><path stroke-linecap="round" stroke-linejoin="round" d="M15.042 21.672 13.684 16.6m0 0-2.51 2.225.569-9.47 5.227 7.917-3.286-.672ZM12 2.25V4.5m5.834.166-1.591 1.591M20.25 10.5H18M7.757 14.743l-1.59 1.59M6 10.5H3.75m4.007-4.243-1.59-1.59" /></svg>
          <div class="empty-title" style="font-size:18px; font-weight:600;">Select an event to view submissions</div>
        </div>
      </div>
    </app-admin-shell>
  `,
  styles: []
})
export class AdminSalesComponent implements OnInit {
  events: any[] = [];
  submissions: any[] = [];
  selectedEvent: any = null;
  loading = true;
  loadingSubmissions = false;
  showCreate = false;
  creating = false;
  newEvent: any = { eventName: '', description: '', startTime: '', endTime: '' };

  constructor(private adminService: AdminService, private saleService: SaleService, private toast: ToastService) {}

  ngOnInit(): void {
    this.adminService.getAllSaleEvents().subscribe({
      next: e => { this.events = Array.isArray(e) ? e : (e.content ?? []); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  createEvent(): void {
    if (!this.newEvent.eventName || !this.newEvent.startTime || !this.newEvent.endTime) {
      this.toast.warning('Fill all required fields'); return;
    }
    this.creating = true;
    this.adminService.createSaleEvent(this.newEvent).subscribe({
      next: (e) => { this.toast.success('Event created!'); this.events.unshift(e); this.creating = false; this.showCreate = false; },
      error: (err) => { this.creating = false; this.toast.error(err.error?.message || 'Failed'); }
    });
  }

  selectEvent(event: any): void {
    this.selectedEvent = event;
    this.loadingSubmissions = true;
    this.submissions = [];
    this.adminService.getEventSubmissions(event.saleEventPublicId || event.publicId || event.id).subscribe({
      next: (subs) => { this.submissions = subs || []; this.loadingSubmissions = false; },
      error: () => { this.submissions = []; this.loadingSubmissions = false; this.toast.error('Failed to load submissions'); }
    });
  }

  reviewItem(item: any, status: 'APPROVED' | 'REJECTED'): void {
    const id = item.publicId || item.id;
    this.adminService.reviewSellerSubmission(id, status).subscribe({
      next: () => { item.approvalStatus = status; this.toast.success(`Submission ${status.toLowerCase()}`); },
      error: (e) => this.toast.error(e.error?.message || 'Failed')
    });
  }

  getEventBadge(status: string): string {
    return status === 'ACTIVE' ? 'badge-red' : status === 'SCHEDULED' ? 'badge-purple' : 'badge-gray';
  }

  getSubmBadge(status: string): string {
    return status === 'APPROVED' ? 'badge-green' : status === 'REJECTED' ? 'badge-red' : 'badge-yellow';
  }
}
