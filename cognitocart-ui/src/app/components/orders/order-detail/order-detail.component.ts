import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderService } from '../../../services/order.service';
import { ToastService } from '../../../services/toast.service';
import { AuthService } from '../../../services/auth.service';
import { ProductReturnStatusComponent } from '../../shared/product-return-status/product-return-status.component';

declare var Razorpay: any;

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ProductReturnStatusComponent],
  template: `
    <div class="page">
      <div class="breadcrumb">
        <a routerLink="/orders">My Orders</a>
        <span class="sep">›</span>
        <span class="current">Order Detail</span>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div *ngIf="!loading && order" class="order-detail-layout">
        <!-- Left Column -->
        <div>
          <!-- Order Header -->
          <div class="glass-card fade-up" style="margin-bottom:24px;">
            <div class="card-body">
              <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:16px;">
                <div>
                  <h2 style="margin-bottom:4px;">{{order.orderNumber}}</h2>
                  <p>{{order.orderDate | date:'fullDate'}} at {{order.orderDate | date:'shortTime'}}</p>
                </div>
                <div style="display:flex; align-items:center; gap:12px;">
                  <span class="badge badge-lg" [class]="'status-' + order.status" style="font-size:14px; padding:6px 16px;">
                    {{order.status}}
                  </span>
                  <button (click)="downloadInvoice()" class="btn btn-ghost btn-sm">📄 Invoice</button>
                </div>
              </div>

              <div class="divider"></div>

              <!-- Items -->
              <h4 style="margin-bottom:16px;">Order Items</h4>
              <div *ngFor="let item of order.items" style="display:flex; gap:16px; padding:12px 0; border-bottom:1px solid var(--border-default);">
                <img [src]="item.productImageUrl || 'https://via.placeholder.com/80'" style="width:80px; height:80px; border-radius:12px; object-fit:cover; border:1px solid var(--border-subtle); background:var(--bg-card);" />
                <div style="flex:1;">
                  <div style="font-weight:600; color:var(--text-primary); font-size:15px;">{{item.productName}}</div>
                  <div style="font-size:13px; color:var(--text-muted);">{{item.variantInfo || 'Standard'}}</div>
                  <div style="font-size:13px; color:var(--text-muted);">Qty: {{item.quantity}}</div>
                  <div style="margin-top: 8px;">
                    <app-product-return-status [isReturnable]="item.policyType !== 'NON_RETURNABLE'"></app-product-return-status>
                  </div>
                  <div *ngIf="!item.policyType || item.policyType === 'NON_RETURNABLE'" style="margin-top:4px; font-size:11px; color:var(--text-muted);">
                    Not eligible for any post-purchase action (non-returnable as per seller's policy at time of purchase).
                  </div>
                </div>
                <div style="font-weight:700; color:var(--primary); font-size:16px;">\u20B9{{item.price * item.quantity | number:'1.0-0'}}</div>
              </div>

              <!-- Totals -->
              <div style="margin-top:20px;">
                <div class="summary-row" *ngIf="order.discountAmount > 0" style="color:var(--success);">
                  <span>Discount</span><span>−\u20B9{{order.discountAmount | number:'1.0-0'}}</span>
                </div>
                <div class="summary-row" style="font-size:1.1rem; font-weight:700; color:var(--text-primary); border-top:1px solid var(--border-default); padding-top:16px; margin-top:8px;">
                  <span>Total</span>
                  <span style="color:var(--primary); font-size:1.4rem; font-family:var(--font-head);">\u20B9{{order.totalAmount | number:'1.0-0'}}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Order Timeline -->
          <div class="glass-card fade-up" style="margin-bottom:24px; animation-delay:0.1s;">
            <div class="card-body">
              <h4 style="margin-bottom:20px;">📅 Order Timeline</h4>
              <div class="timeline">
                <div *ngFor="let event of timeline" class="timeline-item">
                  <div class="timeline-dot" [class.success]="true">✓</div>
                  <div class="timeline-content">
                    <div class="timeline-title">{{event.status || event.event}}</div>
                    <div class="timeline-date">{{event.timestamp | date:'medium'}}</div>
                    <div class="timeline-desc" *ngIf="event.comment || event.note">{{event.comment || event.note}}</div>
                  </div>
                </div>
                <div *ngIf="timeline.length === 0" style="color:var(--text-dim); font-size:14px;">No timeline events yet</div>
              </div>
            </div>
          </div>

          <!-- Return Request -->
          <div *ngIf="order.status === 'DELIVERED' && !returnSubmitted" class="glass-card fade-up" style="animation-delay:0.2s;">
            <div class="card-body" *ngIf="hasReturnableItems()">
              <h4 style="margin-bottom:16px;">↩️ Request Return / Replacement</h4>
              <div class="form-group">
                <label class="form-label">Type</label>
                <select [(ngModel)]="returnReq.type" class="form-select">
                  <option value="RETURN">Return & Refund</option>
                  <option value="REPLACEMENT">Replacement</option>
                  <option value="EXCHANGE">Exchange</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Reason</label>
                <select [(ngModel)]="returnReq.reason" class="form-select">
                  <option value="DEFECTIVE">Defective / Damaged</option>
                  <option value="WRONG_ITEM">Wrong Item Received</option>
                  <option value="NOT_AS_DESCRIBED">Not As Described</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Description</label>
                <textarea [(ngModel)]="returnReq.description" class="form-textarea" placeholder="Describe the issue..." rows="3"></textarea>
              </div>
              <div class="form-group">
                <label class="form-label">Upload Images (Optional)</label>
                <input type="file" class="form-input" accept="image/*" multiple (change)="onFilesSelected($event)">
                <div style="font-size:12px; color:var(--text-muted); margin-top:4px;">Attach photos of the defect or issue</div>
              </div>
              <button class="btn btn-warning" (click)="submitReturn()" [disabled]="submittingReturn">
                {{submittingReturn ? 'Submitting...' : 'Submit Return Request'}}
              </button>
            </div>
            <div class="card-body" *ngIf="!hasReturnableItems()">
              <h4 style="margin-bottom:16px;">↩️ Request Return / Replacement</h4>
              <div class="alert alert-warning" style="background: rgba(239,68,68,0.1); color: #ef4444; border: 1px solid rgba(239,68,68,0.2); padding: 16px; border-radius: 8px;">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:8px; vertical-align:text-bottom;"><circle cx="12" cy="12" r="10"></circle><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line></svg>
                This order is not eligible for return or replacement because all items in the order are non-returnable.
              </div>
            </div>
          </div>

          <div *ngIf="returnSubmitted" class="alert alert-success">
            ✅ Return request submitted! Admin will review within 1-2 business days.
          </div>
        </div>

        <!-- Right Column -->
        <div>
          <!-- Delivery Address -->
          <div class="glass-card fade-up" style="margin-bottom:24px; animation-delay:0.1s;">
            <div class="card-body">
              <h4 style="margin-bottom:16px;">📍 Delivery Address</h4>
              <div *ngIf="order.deliveryAddress" style="font-size:14px; color:var(--text-muted); line-height:1.8;">
                <div style="color:#fff; font-weight:600;">{{order.deliveryAddress.fullName}}</div>
                <div>{{order.deliveryAddress.addressLine1}}</div>
                <div *ngIf="order.deliveryAddress.addressLine2">{{order.deliveryAddress.addressLine2}}</div>
                <div>{{order.deliveryAddress.city}}, {{order.deliveryAddress.state}} - {{order.deliveryAddress.pincode}}</div>
                <div>📞 {{order.deliveryAddress.phone}}</div>
              </div>
            </div>
          </div>

          <!-- Shipping Info -->
          <div class="glass-card fade-up" *ngIf="order.trackingNumber" style="margin-bottom:24px; animation-delay:0.2s;">
            <div class="card-body">
              <h4 style="margin-bottom:16px;">🚚 Tracking</h4>
              <div style="font-size:14px; color:var(--text-muted);">
                <div><strong>Courier:</strong> {{order.courier}}</div>
                <div><strong>Tracking #:</strong> {{order.trackingNumber}}</div>
                <div *ngIf="order.trackingUrl"><a [href]="order.trackingUrl" target="_blank" class="btn btn-ghost btn-sm" style="margin-top:8px;">Track Package →</a></div>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="glass-card fade-up" style="animation-delay:0.3s;">
            <div class="card-body">
              <h4 style="margin-bottom:16px;">Actions</h4>
              <div style="display:flex; flex-direction:column; gap:8px;">
                <button (click)="downloadInvoice()" class="btn btn-secondary btn-full">📄 Download Invoice</button>
                <button *ngIf="order.status === 'PAYMENT_PENDING'"
                  class="btn btn-primary btn-full" (click)="retryPayment()" [disabled]="placingOrder">
                  <span *ngIf="placingOrder" class="spinner spinner-sm"></span>
                  {{placingOrder ? 'Processing...' : '💳 Pay Now (\u20B9' + (order.totalAmount | number:'1.0-0') + ')'}}
                </button>
                <button *ngIf="order.status === 'PAYMENT_PENDING' || order.status === 'PENDING' || order.status === 'CONFIRMED'"
                  class="btn btn-danger btn-full" (click)="cancelOrder()">
                  Cancel Order
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div *ngIf="!loading && !order" class="empty-state" style="min-height:400px;">
        <div class="empty-icon">❌</div>
        <div class="empty-title">Order not found</div>
        <a routerLink="/orders" class="btn btn-primary">Back to Orders</a>
      </div>
    </div>
  `,
  styles: [`
    .order-detail-layout { display: grid; grid-template-columns: 1fr 380px; gap: 32px; align-items: start; }
    .summary-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 15px; color: var(--text-muted); }
    
    /* Timeline styles */
    .timeline { position: relative; padding-left: 24px; }
    .timeline::before { content: ''; position: absolute; left: 7px; top: 0; bottom: 0; width: 2px; background: var(--border-default); }
    .timeline-item { position: relative; padding-bottom: 24px; }
    .timeline-item:last-child { padding-bottom: 0; }
    .timeline-item:last-child::after { content: ''; position: absolute; left: 7px; top: 20px; bottom: 0; width: 2px; background: var(--bg-surface); z-index: 1; }
    .timeline-dot { position: absolute; left: -24px; top: 0; width: 16px; height: 16px; border-radius: 50%; background: var(--border-default); border: 2px solid var(--bg-surface); z-index: 2; display: flex; align-items: center; justify-content: center; font-size: 10px; color: #fff; }
    .timeline-dot.success { background: var(--primary); box-shadow: 0 0 12px rgba(99,102,241,0.6); border-color: var(--primary); }
    .timeline-content { padding-left: 12px; }
    .timeline-title { font-weight: 700; font-size: 14px; color: var(--text-primary); margin-bottom: 4px; }
    .timeline-date { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }
    .timeline-desc { font-size: 13px; color: var(--text-secondary); line-height: 1.5; background: var(--bg-card); border: 1px solid var(--border-subtle); padding: 8px 12px; border-radius: 8px; margin-top: 6px; }

    @media (max-width: 1024px) { .order-detail-layout { grid-template-columns: 1fr; } }
  `]
})
export class OrderDetailComponent implements OnInit {
  order: any = null;
  timeline: any[] = [];
  loading = true;
  returnSubmitted = false;
  submittingReturn = false;
  returnReq = { type: 'RETURN', reason: 'DEFECTIVE', description: '' };
  returnImages: File[] = [];

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService,
    private toast: ToastService,
    public auth: AuthService
  ) {}

  hasReturnableItems(): boolean {
    if (!this.order || !this.order.items) return true; // Default to true if items not loaded
    // If ANY item is NOT explicitly marked as NON_RETURNABLE, we allow return.
    return this.order.items.some((item: any) => item.policyType && item.policyType !== 'NON_RETURNABLE');
  }

  downloadInvoice(): void {
    const id = this.order?.orderPublicId || this.route.snapshot.paramMap.get('id')!;
    this.orderService.downloadInvoice(id).subscribe({
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

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.orderService.getDetail(id).subscribe({
      next: (res) => {
        this.order = res;
        this.loadTimeline(id);
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  loadTimeline(id: string): void {
    this.orderService.getTimeline(id).subscribe({ next: t => this.timeline = t, error: () => {} });
  }

  cancelOrder(): void {
    if (!confirm('Cancel this order?')) return;
    const id = this.order.orderPublicId || this.route.snapshot.paramMap.get('id')!;
    this.orderService.cancel(id).subscribe({
      next: () => { this.toast.success('Order cancelled. Refund initiated.'); this.order.status = 'CANCELLED'; },
      error: (e) => this.toast.error(e.error?.message || 'Cannot cancel')
    });
  }

  onFilesSelected(event: any): void {
    if (event.target.files) {
      this.returnImages = Array.from(event.target.files);
    }
  }

  submitReturn(): void {
    if (!this.returnReq.description) { this.toast.warning('Please describe the issue'); return; }
    this.submittingReturn = true;
    const id = this.order.orderPublicId || this.route.snapshot.paramMap.get('id')!;
    const payload = { 
      returnType: this.returnReq.type, 
      returnReason: this.returnReq.reason, 
      returnDescription: this.returnReq.description 
    };
    this.orderService.requestReturn(id, payload, this.returnImages).subscribe({
      next: () => { this.submittingReturn = false; this.returnSubmitted = true; this.toast.success('Return request submitted!'); },
      error: (e) => { this.submittingReturn = false; this.toast.error(e.error?.message || 'Failed to submit'); }
    });
  }

  placingOrder = false;

  retryPayment(): void {
    this.placingOrder = true;
    this.initRazorpay(this.order);
  }

  private initRazorpay(order: any): void {
    const options = {
      key: order.razorpayKeyId || 'rzp_test_xxx',
      amount: order.razorpayAmount || (order.totalAmount * 100),
      currency: 'INR',
      name: 'CognitoCart',
      description: `Order #${order.orderNumber || order.orderPublicId}`,
      order_id: order.razorpayOrderId,
      handler: (response: any) => this.verifyPayment(response, order),
      prefill: {},
      theme: { color: '#63b3ed' },
      modal: { ondismiss: () => { this.placingOrder = false; this.toast.warning('Payment cancelled'); } }
    };

    if (typeof Razorpay !== 'undefined') {
      const rzp = new Razorpay(options);
      rzp.open();
    } else {
      this.toast.warning('Payment gateway not loaded.');
      this.placingOrder = false;
    }
  }

  private verifyPayment(response: any, order: any): void {
    this.orderService.verifyPayment({
      razorpayOrderId: response.razorpay_order_id,
      razorpayPaymentId: response.razorpay_payment_id,
      razorpaySignature: response.razorpay_signature
    }).subscribe({
      next: () => {
        this.toast.success('Payment verified! Order confirmed! 🎉');
        this.placingOrder = false;
        this.order.status = 'PENDING';
      },
      error: (e) => {
        this.placingOrder = false;
        this.toast.error(e.error?.message || 'Payment verification failed');
      }
    });
  }
}
