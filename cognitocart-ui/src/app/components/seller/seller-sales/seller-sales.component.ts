import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SellerService } from '../../../services/seller.service';
import { ToastService } from '../../../services/toast.service';
import { SaleService } from '../../../services/sale.service';
import { ProductService } from '../../../services/product.service';

@Component({
  selector: 'app-seller-sales',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="command-center">
      <div class="cc-header">
        <div class="header-left">
          <div class="greeting-block">
            <h1 style="display:flex; align-items:center; gap:12px;">
              <div class="kpi-icon kpi-icon-red">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:24px;height:24px;color:var(--red-400);"><path stroke-linecap="round" stroke-linejoin="round" d="m3.75 13.5 10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75Z" /></svg>
              </div>
              Flash Sales
            </h1>
            <p>Opt your products into admin-created sale events.</p>
          </div>
        </div>
      </div>

      <div class="grid-2">
        <!-- Live Events -->
        <div>
          <div class="cc-panel" style="margin-bottom:24px;">
            <div class="panel-header border-bottom">
              <h2>
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:20px;height:20px;color:var(--red-400);"><path stroke-linecap="round" stroke-linejoin="round" d="M15.362 5.214A8.252 8.252 0 0 1 12 21 8.25 8.25 0 0 1 6.038 7.047 8.287 8.287 0 0 0 9 9.601a8.983 8.983 0 0 1 3.361-6.867 8.21 8.21 0 0 0 3 2.48Z" /><path stroke-linecap="round" stroke-linejoin="round" d="M12 18a3.75 3.75 0 0 0 .495-7.468 5.99 5.99 0 0 0-1.925 3.547 5.975 5.975 0 0 1-2.133-1.001A3.75 3.75 0 0 0 12 18Z" /></svg>
                Active & Upcoming Events
              </h2>
            </div>
            <div class="panel-body">
              <div *ngFor="let e of allEvents" class="event-card" [class.live]="e.status === 'ACTIVE'" [class.scheduled]="e.status === 'SCHEDULED'">
                <div style="display:flex; justify-content:space-between; align-items:flex-start;">
                  <div>
                    <div style="font-weight:700; color:#fff;">{{e.eventName}}</div>
                    <div style="font-size:13px; color:var(--text-muted);">{{e.description}}</div>
                    <div style="font-size:12px; color:var(--text-dim); margin-top:4px;">
                      {{e.startTime | date:'medium'}} &#8594; {{e.endTime | date:'medium'}}
                    </div>
                  </div>
                  <div style="display:flex; flex-direction:column; gap:8px; align-items:flex-end;">
                    <span class="badge" [class]="e.status === 'ACTIVE' ? 'badge-red' : 'badge-purple'" style="display:inline-flex; align-items:center; gap:4px;">
                      <svg *ngIf="e.status === 'ACTIVE'" fill="currentColor" viewBox="0 0 24 24" style="width:12px;height:12px;"><circle cx="12" cy="12" r="10"/></svg>
                      <svg *ngIf="e.status !== 'ACTIVE'" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5" /></svg>
                      {{e.status === 'ACTIVE' ? 'LIVE' : 'UPCOMING'}}
                    </span>
                    <button class="btn btn-primary btn-sm" (click)="selectEvent(e)">
                      Submit Product
                    </button>
                  </div>
                </div>
              </div>
              <div class="empty-state" *ngIf="allEvents.length === 0" style="min-height:120px; padding:24px;">
                <div class="empty-icon" style="font-size:32px;">
                  <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:32px;height:32px;color:var(--text-muted);"><path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5m-9-6h.008v.008H12v-.008ZM12 15h.008v.008H12V15Zm0 2.25h.008v.008H12v-.008ZM9.75 15h.008v.008H9.75V15Zm0 2.25h.008v.008H9.75v-.008ZM7.5 15h.008v.008H7.5V15Zm0 2.25h.008v.008H7.5v-.008Zm6.75-4.5h.008v.008h-.008v-.008Zm0 2.25h.008v.008h-.008V15Zm0 2.25h.008v.008h-.008v-.008Zm2.25-4.5h.008v.008H16.5v-.008Zm0 2.25h.008v.008H16.5V15Z" /></svg>
                </div>
                <div class="empty-title">No events available</div>
              </div>
            </div>
          </div>

          <!-- Bulk CSV Upload -->
          <div class="cc-panel" *ngIf="selectedEventId">
            <div class="panel-header border-bottom">
              <h2>
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:20px;height:20px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" /></svg>
                Bulk CSV Upload
              </h2>
            </div>
            <div class="panel-body">
              <p style="font-size:13px; color:var(--text-muted); margin-bottom:16px;">
                Upload a CSV with columns: <code>variant_id, discount_percentage, max_units, max_units_per_user</code><br/>
                <span style="font-size:12px; color:var(--primary); cursor:pointer; display:inline-flex; align-items:center; gap:4px;" (click)="showCsvHelp = !showCsvHelp">
                  <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="m11.25 11.25.041-.02a.75.75 0 0 1 1.063.852l-.708 2.836a.75.75 0 0 0 1.063.853l.041-.021M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Zm-9-3.75h.008v.008H12V8.25Z" /></svg>
                  How to create this CSV?
                </span>
              </p>
              
              <div *ngIf="showCsvHelp" style="background:rgba(255,255,255,0.05); padding:12px; border-radius:8px; font-size:12px; color:var(--text-dim); margin-bottom:16px;">
                <strong>Steps:</strong>
                <ol style="margin-left:20px; margin-top:8px; margin-bottom:0;">
                  <li>Open Excel or Google Sheets.</li>
                  <li>In row 1, add these exact headers: <code>variant_id</code>, <code>discount_percentage</code>, <code>max_units</code>, <code>max_units_per_user</code>.</li>
                  <li>Add your variant IDs (e.g., from your product dashboard UUIDs).</li>
                  <li>Go to File > Download > Comma Separated Values (.csv).</li>
                </ol>
              </div>

              <input type="file" (change)="onFileChange($event)" accept=".csv" class="form-input" style="cursor:pointer;" />
              <button class="btn btn-primary" (click)="uploadBulk()" [disabled]="!bulkFile || uploadingBulk" style="margin-top:12px;">
                <svg *ngIf="!uploadingBulk" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;margin-right:4px;"><path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75V16.5m-13.5-9L12 3m0 0 4.5 4.5M12 3v13.5" /></svg>
                {{uploadingBulk ? 'Uploading...' : 'Upload CSV'}}
              </button>
              <div *ngIf="bulkResult" class="alert alert-success" style="margin-top:12px;">{{bulkResult}}</div>
            </div>
          </div>
        </div>

        <!-- My Submissions -->
        <div>
          <div class="cc-panel">
            <div class="panel-header border-bottom">
              <h2>
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:20px;height:20px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25ZM6.75 12h.008v.008H6.75V12Zm0 3h.008v.008H6.75V15Zm0 3h.008v.008H6.75V18Z" /></svg>
                My Submissions
              </h2>
            </div>
            <div class="panel-body">

              <!-- Submit Form -->
              <div *ngIf="selectedEvent" class="submit-form" style="background:rgba(255,255,255,0.03); border-radius:var(--radius); padding:16px; margin-bottom:20px; border:1px solid var(--primary);">
                <div style="font-size:13px; font-weight:600; color:var(--primary); margin-bottom:12px;">
                  Submitting for: {{selectedEvent.eventName}}
                </div>
                <div class="form-group">
                  <label class="form-label">Variant</label>
                  <select [(ngModel)]="newSubmission.variantPublicId" class="form-select">
                    <option value="">Select variant...</option>
                    <option *ngFor="let v of myVariants" [value]="v.variantPublicId">{{v.productName}} &#8212; {{v.displayLabel || 'Default'}}</option>
                  </select>
                </div>
                <div class="form-row form-row-2">
                  <div class="form-group">
                    <label class="form-label">Discount %</label>
                    <input type="number" [(ngModel)]="newSubmission.discountPercentage" class="form-input" placeholder="e.g. 20" min="1" max="90" />
                  </div>
                  <div class="form-group">
                    <label class="form-label">Max Units</label>
                    <input type="number" [(ngModel)]="newSubmission.maxUnits" class="form-input" placeholder="e.g. 100" />
                  </div>
                </div>
                <div class="form-group">
                  <label class="form-label">Max Units per User</label>
                  <input type="number" [(ngModel)]="newSubmission.maxUnitsPerUser" class="form-input" placeholder="e.g. 2" />
                </div>
                <div style="display:flex; gap:8px;">
                  <button class="btn btn-primary" (click)="submitItem()" [disabled]="submitting">
                    <svg *ngIf="!submitting" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;margin-right:4px;"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" /></svg>
                    {{submitting ? 'Submitting...' : 'Submit'}}
                  </button>
                  <button class="btn btn-secondary" (click)="selectedEvent = null">Cancel</button>
                </div>
              </div>

              <!-- Submissions List -->
              <div class="loading-center" *ngIf="loadingSubmissions"><div class="spinner spinner-sm"></div></div>

              <div *ngFor="let s of mySubmissions" style="padding:12px; border:1px solid var(--border); border-radius:var(--radius); margin-bottom:8px;">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                  <div>
                    <div style="font-weight:600; font-size:14px; color:#fff;">{{s.sku || s.variantName || 'Variant'}}</div>
                    <div style="font-size:12px; color:var(--text-muted);">Discount: {{s.discountPercentage}}% | Max: {{s.maxUnits}} units</div>
                    <div style="font-size:11px; color:var(--text-dim); margin-top:2px;">Event: {{s.eventName || '&#8212;'}}</div>
                  </div>
                  <span class="badge" [class]="getSubmissionBadge(s.approvalStatus)">{{s.approvalStatus}}</span>
                </div>
              </div>

              <div class="empty-state" *ngIf="!loadingSubmissions && mySubmissions.length === 0" style="padding:24px; min-height:100px;">
                <div class="empty-title">No submissions yet</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .event-card { padding: 16px; border: 1px solid var(--border); border-radius: var(--radius); margin-bottom: 12px; transition: var(--transition); }
    .event-card.live { border-color: rgba(252,129,129,0.4); background: rgba(252,129,129,0.04); }
    .event-card.scheduled { border-color: rgba(183,148,244,0.3); background: rgba(183,148,244,0.03); }
  `]
})
export class SellerSalesComponent implements OnInit {
  allEvents: any[] = [];
  mySubmissions: any[] = [];
  myVariants: any[] = [];
  selectedEvent: any = null;
  selectedEventId = '';
  loadingSubmissions = true;
  submitting = false;
  bulkFile: File | null = null;
  bulkResult = '';
  uploadingBulk = false;
  showCsvHelp = false;
  newSubmission = { variantPublicId: '', saleEventPublicId: '', discountPercentage: 0, maxUnits: 100, maxUnitsPerUser: 2 };

  constructor(private sellerService: SellerService, private saleService: SaleService, private productService: ProductService, private toast: ToastService) {}

  ngOnInit(): void {
    this.saleService.getLiveSales().subscribe({ next: e => this.allEvents = [...this.allEvents, ...e], error: () => {} });
    this.saleService.getUpcomingSales().subscribe({ next: e => this.allEvents = [...this.allEvents, ...e], error: () => {} });
    this.sellerService.getMyVariants().subscribe({ next: v => this.myVariants = v.content || v, error: () => {} });
    this.loadSubmissions();
  }

  loadSubmissions(): void {
    this.sellerService.getMySubmissions().subscribe({
      next: s => { this.mySubmissions = s; this.loadingSubmissions = false; },
      error: () => this.loadingSubmissions = false
    });
  }

  selectEvent(event: any): void {
    this.selectedEvent = event;
    // Backend now returns saleEventPublicId (UUID). Fall back to publicId if present.
    this.selectedEventId = event.saleEventPublicId || event.publicId || event.id;
    this.newSubmission.saleEventPublicId = this.selectedEventId;
  }

  submitItem(): void {
    if (!this.newSubmission.variantPublicId || !this.newSubmission.discountPercentage) {
      this.toast.warning('Fill all required fields'); return;
    }
    this.submitting = true;
    this.sellerService.submitFlashSaleItem(this.newSubmission).subscribe({
      next: () => { this.toast.success('Submitted for review!'); this.submitting = false; this.selectedEvent = null; this.loadSubmissions(); },
      error: (e) => { this.submitting = false; this.toast.error(e.error?.message || 'Submit failed'); }
    });
  }

  onFileChange(e: any): void { this.bulkFile = e.target.files[0] || null; }

  uploadBulk(): void {
    if (!this.bulkFile || !this.selectedEventId) return;
    this.uploadingBulk = true;
    this.sellerService.uploadBulkSaleCsv(this.selectedEventId, this.bulkFile).subscribe({
      next: (res) => { this.bulkResult = res; this.uploadingBulk = false; this.toast.success('Bulk upload done!'); },
      error: () => { this.uploadingBulk = false; this.toast.error('Upload failed'); }
    });
  }

  getSubmissionBadge(status: string): string {
    const map: Record<string, string> = { PENDING: 'badge-yellow', APPROVED: 'badge-green', REJECTED: 'badge-red' };
    return map[status] || 'badge-gray';
  }
}
