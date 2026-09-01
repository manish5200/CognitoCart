import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AddressService } from '../../services/address.service';
import { OrderService } from '../../services/order.service';
import { CartService } from '../../services/cart.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';

declare var Razorpay: any;

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page checkout-page">
      <div class="page-header text-center" style="margin-bottom: 40px;">
        <h1 class="page-title">Checkout</h1>
      </div>

      <!-- Horizontal Steps Indicator -->
      <div class="checkout-stepper">
        <div class="step-wrapper" [class.active]="step >= 1" [class.completed]="step > 1" (click)="step > 1 ? step = 1 : null">
          <div class="step-icon">
            <svg *ngIf="step > 1" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
            <span *ngIf="step <= 1">1</span>
          </div>
          <div class="step-text">Address</div>
        </div>
        <div class="step-line" [class.active]="step >= 2"></div>
        <div class="step-wrapper" [class.active]="step >= 2" [class.completed]="step > 2" (click)="step > 2 ? step = 2 : null">
          <div class="step-icon">
            <svg *ngIf="step > 2" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
            <span *ngIf="step <= 2">2</span>
          </div>
          <div class="step-text">Review</div>
        </div>
        <div class="step-line" [class.active]="step >= 3"></div>
        <div class="step-wrapper" [class.active]="step >= 3" [class.completed]="step > 3">
          <div class="step-icon">
            <svg *ngIf="step > 3" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
            <span *ngIf="step <= 3">3</span>
          </div>
          <div class="step-text">Payment</div>
        </div>
      </div>

      <div class="checkout-layout">
        <!-- Left Column -->
        <div class="checkout-main">

          <!-- Step 1: Address -->
          <div *ngIf="step === 1" class="glass-card animate-fade-in">
            <div class="card-body">
              <div class="section-title-row">
                <div class="icon-circle">📍</div>
                <h3>Delivery Address</h3>
              </div>

              <div *ngIf="addresses.length === 0 && !showAddAddress" class="empty-state">
                <div class="empty-icon">🏠</div>
                <div class="empty-title">No addresses found</div>
                <div class="empty-subtitle">Please add a delivery address to continue checkout.</div>
                <button (click)="showAddAddress = true" class="btn btn-primary">Add New Address</button>
              </div>

              <div *ngIf="addresses.length > 0 && !showAddAddress" style="display:flex; justify-content:flex-end; margin-bottom: 24px;">
                <button (click)="showAddAddress = true" class="btn btn-secondary btn-sm">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                  Add New Address
                </button>
              </div>

              <div *ngIf="showAddAddress" class="add-address-form slide-down">
                <h4 style="margin-bottom:20px; color:#fff;">Add Delivery Address</h4>
                <div class="form-grid">
                  <div class="form-group"><label class="form-label">Full Name</label><input type="text" [(ngModel)]="newAddress.fullName" class="form-input" placeholder="e.g. John Doe"></div>
                  <div class="form-group"><label class="form-label">Phone Number</label><input type="text" [(ngModel)]="newAddress.phone" class="form-input" placeholder="10-digit mobile number"></div>
                  <div class="form-group full-width"><label class="form-label">Address Line 1</label><input type="text" [(ngModel)]="newAddress.addressLine1" class="form-input" placeholder="Flat, House no., Building, Company, Apartment"></div>
                  <div class="form-group full-width"><label class="form-label">Address Line 2 (Optional)</label><input type="text" [(ngModel)]="newAddress.addressLine2" class="form-input" placeholder="Area, Street, Sector, Village"></div>
                  <div class="form-group"><label class="form-label">City / Town</label><input type="text" [(ngModel)]="newAddress.city" class="form-input" placeholder="e.g. Bengaluru"></div>
                  <div class="form-group"><label class="form-label">State</label><input type="text" [(ngModel)]="newAddress.state" class="form-input" placeholder="e.g. Karnataka"></div>
                  <div class="form-group"><label class="form-label">Pincode</label><input type="text" [(ngModel)]="newAddress.pincode" class="form-input" placeholder="6 digits"></div>
                </div>
              </div>

              <div class="address-grid" *ngIf="!showAddAddress && addresses.length > 0">
                <div *ngFor="let addr of addresses" class="address-card" [class.selected]="selectedAddressId === addr.publicAddressId" (click)="selectedAddressId = addr.publicAddressId">
                  <div class="addr-header">
                    <span class="addr-name">{{addr.fullName}}</span>
                    <span class="badge badge-primary" *ngIf="addr.isDefault">Default</span>
                  </div>
                  <div class="addr-body">
                    <div>{{addr.addressLine1}}<span *ngIf="addr.addressLine2">, {{addr.addressLine2}}</span></div>
                    <div>{{addr.city}}, {{addr.state}} - {{addr.pincode}}</div>
                  </div>
                  <div class="addr-footer">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path></svg>
                    {{addr.phone}}
                  </div>
                  <div class="addr-check">
                    <div class="check-circle"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg></div>
                  </div>
                </div>
              </div>

            </div>
          </div>

          <!-- Step 2: Review -->
          <div *ngIf="step === 2" class="glass-card animate-fade-in">
            <div class="card-body">
              <div class="section-title-row">
                <div class="icon-circle">🛍️</div>
                <h3>Order Review</h3>
              </div>
              
              <div class="review-items">
                <div *ngFor="let item of cart?.items" class="review-item">
                  <div class="item-img-wrapper">
                    <img [src]="item.productImageUrl || 'https://via.placeholder.com/80'" [alt]="item.productName" />
                  </div>
                  <div class="item-details">
                    <div class="item-name">{{item.productName}}</div>
                    <div class="item-meta">{{item.variantInfo || 'Standard'}}</div>
                    <div class="item-qty">Qty: <strong>{{item.quantity}}</strong></div>
                  </div>
                  <div class="item-price">₹{{item.price * item.quantity | number:'1.0-0'}}</div>
                </div>
              </div>

              <div class="delivery-estimate">
                <div class="est-icon">🚚</div>
                <div class="est-text">
                  <strong>Estimated Delivery</strong>
                  <div>Dispatches within 24 hours. Delivery in 3-5 business days.</div>
                </div>
              </div>

              <div class="step-footer dual">
                <button class="btn btn-secondary btn-lg" (click)="step = 1">
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
                  Back to Address
                </button>
              </div>
            </div>
          </div>

          <!-- Step 3: Payment -->
          <div *ngIf="step === 3" class="glass-card animate-fade-in">
            <div class="card-body">
              <div class="section-title-row">
                <div class="icon-circle">💳</div>
                <h3>Payment Details</h3>
              </div>

              <div class="secure-banner">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                <span>Secured by Razorpay. Your payment details are encrypted and never stored.</span>
              </div>

              <div class="payment-methods">
                <div class="payment-method selected">
                  <div class="pm-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="14" rx="2" ry="2"></rect><line x1="2" y1="10" x2="22" y2="10"></line></svg>
                  </div>
                  <div class="pm-info">
                    <h4>Razorpay Secure</h4>
                    <p>Cards, UPI, Net Banking, Wallets</p>
                  </div>
                  <div class="pm-radio"><div class="radio-dot"></div></div>
                </div>
              </div>

              <div class="step-footer dual" style="margin-top: 32px;">
                <button class="btn btn-secondary btn-lg" (click)="step = 2">
                  <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
                  Back
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Order Summary Sidebar -->
        <div class="checkout-summary">
          <div class="glass-card summary-card">
            <h4 class="summary-title">Order Summary</h4>
            <div class="summary-content">
              <div class="summary-row">
                <span class="label" style="color:var(--text-primary); font-weight: 500;">Items ({{cart?.items?.length || 0}})</span>
                <span class="value">₹{{(cart?.subtotalAmount || getSubtotal()) | number:'1.0-0'}}</span>
              </div>
              <div class="summary-row discount-row" *ngIf="cart?.discountAmount > 0">
                <span class="label">Total Discount</span>
                <span class="value">−₹{{cart?.discountAmount | number:'1.0-0'}}</span>
              </div>
              <div class="summary-row">
                <span class="label">Shipping Fee</span>
                <span class="value free">FREE</span>
              </div>
              
              <div class="summary-divider"></div>
              
              <div class="summary-row total-row">
                <span class="label">Total Amount</span>
                <span class="value total-price">₹{{cart?.totalAmount | number:'1.0-0'}}</span>
              </div>
            </div>

            <!-- Dynamic Action Button in Sidebar -->
            <div class="sidebar-action" style="margin-top: 32px;">
              <!-- Step 1: Add Address Mode -->
              <button *ngIf="step === 1 && showAddAddress" class="btn btn-primary btn-lg w-100 pulse-glow" style="width: 100%; border-radius: 12px; padding: 16px; font-weight: 700; font-size: 16px;" (click)="saveNewAddress()" [disabled]="savingAddress">
                <span *ngIf="savingAddress" class="spinner spinner-sm"></span>
                {{savingAddress ? 'Saving...' : 'Save Address & Continue'}}
              </button>

              <!-- Step 1: Address Selected Mode -->
              <button *ngIf="step === 1 && !showAddAddress && addresses.length > 0" class="btn btn-primary btn-lg w-100 pulse-glow" style="width: 100%; border-radius: 12px; padding: 16px; font-weight: 700; font-size: 16px;" (click)="step = 2" [disabled]="!selectedAddressId">
                Continue to Review
              </button>

              <!-- Step 2: Review Mode -->
              <button *ngIf="step === 2" class="btn btn-primary btn-lg w-100 pulse-glow" style="width: 100%; border-radius: 12px; padding: 16px; font-weight: 700; font-size: 16px;" (click)="step = 3">
                Proceed to Payment
              </button>

              <!-- Step 3: Payment Mode -->
              <div *ngIf="step === 3" style="display:flex; flex-direction:column; gap:12px;">
                <div *ngIf="!isVerified" class="verification-banner" style="padding:12px; border-radius:8px; text-align:center;">
                  <span class="verification-text" style="font-size:13px; display:block; margin-bottom:8px;">Email not verified.</span>
                  <button class="btn btn-primary btn-sm verify-btn" (click)="verifyEmail()" style="width:100%;">Verify Now</button>
                </div>
                <button class="btn btn-primary btn-lg w-100 pulse-glow" style="width: 100%; border-radius: 12px; padding: 16px; font-weight: 700; font-size: 16px; display:flex; align-items:center; justify-content:center; gap:8px;" (click)="placeOrder()" [disabled]="placingOrder || !isVerified">
                  <span *ngIf="placingOrder" class="spinner spinner-sm"></span>
                  <span *ngIf="!placingOrder" style="display:flex; align-items:center; gap:8px;">
                    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                    Pay ₹{{cart?.totalAmount | number:'1.0-0'}}
                  </span>
                </button>
              </div>
            </div>
            
            <div class="secure-badge">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
              100% Safe & Secure Payments
            </div>
          </div>
        </div>
      </div>

      <!-- Payment Status Overlay -->
      <div class="payment-overlay" *ngIf="paymentStatus !== 'idle'">
        <div class="payment-modal">
          <div *ngIf="paymentStatus === 'processing'" class="anim-processing">
            <div class="spinner-xl"></div>
            <h3>Processing Payment</h3>
            <p>Please do not close this window or press back.</p>
          </div>
          
          <div *ngIf="paymentStatus === 'success'" class="anim-success">
            <div class="success-icon">
              <svg viewBox="0 0 52 52"><circle cx="26" cy="26" r="25" fill="none"/><path fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8"/></svg>
            </div>
            <h3>Payment Successful!</h3>
            <p>Your order has been confirmed.</p>
          </div>
          
          <div *ngIf="paymentStatus === 'failed'" class="anim-error">
            <div class="error-icon">
              <svg viewBox="0 0 52 52"><circle cx="26" cy="26" r="25" fill="none"/><path fill="none" d="M16 16 36 36 M36 16 16 36"/></svg>
            </div>
            <h3>Payment Failed</h3>
            <p>We couldn't process your payment. Please try again.</p>
            <button class="btn btn-primary" style="margin-top:20px;" (click)="paymentStatus = 'idle'">Try Again</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .checkout-page { padding: 40px 20px; max-width: 1200px; margin: 0 auto; }
    
    /* Horizontal Stepper */
    .checkout-stepper { display: flex; align-items: center; justify-content: space-between; max-width: 700px; margin: 0 auto 48px; position: relative; }
    .step-wrapper { display: flex; flex-direction: column; align-items: center; gap: 8px; z-index: 2; cursor: default; }
    .step-wrapper.completed { cursor: pointer; }
    .step-icon { width: 48px; height: 48px; border-radius: 50%; background: rgba(255,255,255,0.03); border: 2px solid rgba(255,255,255,0.1); display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 700; color: var(--text-muted); transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); box-shadow: 0 4px 12px rgba(0,0,0,0.2); }
    .step-wrapper.active .step-icon { border-color: var(--primary); color: #fff; background: rgba(99,102,241,0.1); box-shadow: 0 0 20px rgba(99,102,241,0.3); }
    .step-wrapper.completed .step-icon { background: var(--primary); border-color: var(--primary); color: #fff; }
    .step-text { font-size: 14px; font-weight: 600; color: var(--text-muted); transition: 0.3s; text-transform: uppercase; letter-spacing: 1px; }
    .step-wrapper.active .step-text { color: #fff; }
    .step-wrapper.completed .step-text { color: var(--text-secondary); }
    .step-line { flex: 1; height: 3px; background: rgba(255,255,255,0.05); margin: 0 16px; margin-bottom: 24px; border-radius: 2px; position: relative; overflow: hidden; }
    .step-line::after { content: ''; position: absolute; top: 0; left: 0; height: 100%; width: 0%; background: linear-gradient(90deg, var(--primary), var(--secondary)); transition: width 0.5s ease-in-out; }
    .step-line.active::after { width: 100%; }

    .checkout-layout { display: grid; grid-template-columns: 1fr 380px; gap: 32px; align-items: start; }
    .checkout-summary { position: sticky; top: 100px; }
    
    .card-body { padding: 32px; }
    
    .section-title-row { display: flex; align-items: center; gap: 16px; margin-bottom: 32px; padding-bottom: 20px; border-bottom: 1px solid var(--border-default); }
    .icon-circle { width: 44px; height: 44px; border-radius: 50%; background: var(--bg-surface); display: flex; align-items: center; justify-content: center; font-size: 20px; border: 1px solid var(--border-subtle); }
    .section-title-row h3 { margin: 0; font-family: var(--font-head); font-size: 1.6rem; color: var(--text-primary); }

    /* Address Grid */
    .address-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .address-card { background: var(--bg-surface); border: 2px solid var(--border-default); border-radius: 16px; padding: 20px; cursor: pointer; transition: var(--transition); position: relative; overflow: hidden; }
    .address-card:hover { border-color: rgba(99,102,241,0.5); transform: translateY(-2px); box-shadow: var(--shadow-sm); }
    .address-card.selected { border-color: var(--primary); background: rgba(99,102,241,0.05); box-shadow: 0 10px 30px rgba(99,102,241,0.1); }
    .address-card.selected::before { content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 4px; background: linear-gradient(90deg, var(--primary), var(--secondary)); }
    .addr-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
    .addr-name { font-weight: 700; font-size: 16px; color: var(--text-primary); }
    .addr-body { color: var(--text-secondary); font-size: 14px; line-height: 1.6; margin-bottom: 16px; }
    .addr-footer { display: flex; align-items: center; gap: 8px; color: var(--text-muted); font-size: 13px; font-weight: 500; }
    .addr-check { position: absolute; bottom: 20px; right: 20px; opacity: 0; transform: scale(0.5); transition: var(--transition); }
    .address-card.selected .addr-check { opacity: 1; transform: scale(1); }
    .check-circle { width: 24px; height: 24px; background: var(--primary); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; box-shadow: 0 4px 10px rgba(99,102,241,0.4); }

    /* Forms */
    .add-address-form { background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: 16px; padding: 24px; margin-bottom: 24px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
    .form-group.full-width { grid-column: 1 / -1; }
    .form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--border-default); }

    /* Review Items */
    .review-items { display: flex; flex-direction: column; gap: 16px; margin-bottom: 32px; }
    .review-item { display: flex; align-items: center; gap: 20px; padding: 16px; background: var(--bg-surface); border-radius: 12px; border: 1px solid var(--border-subtle); }
    .item-img-wrapper { width: 80px; height: 80px; border-radius: 10px; overflow: hidden; flex-shrink: 0; background: var(--bg-card); border: 1px solid var(--border-default); }
    .item-img-wrapper img { width: 100%; height: 100%; object-fit: contain; }
    .item-details { flex: 1; }
    .item-name { font-weight: 700; font-size: 16px; color: var(--text-primary); margin-bottom: 4px; }
    .item-meta { font-size: 13px; color: var(--text-muted); margin-bottom: 8px; }
    .item-qty { font-size: 14px; color: var(--text-secondary); }
    .item-price { font-weight: 800; font-size: 18px; color: var(--primary); }

    .delivery-estimate { display: flex; align-items: flex-start; gap: 16px; padding: 20px; background: rgba(16, 185, 129, 0.05); border: 1px solid rgba(16, 185, 129, 0.2); border-radius: 12px; margin-bottom: 32px; }
    .est-icon { font-size: 24px; }
    .est-text strong { display: block; color: var(--success); margin-bottom: 4px; font-size: 15px; }
    .est-text div { color: var(--text-muted); font-size: 14px; }

    /* Payment Methods */
    .secure-banner { display: flex; align-items: center; gap: 12px; padding: 16px; background: rgba(99, 102, 241, 0.1); border: 1px solid rgba(99, 102, 241, 0.2); border-radius: 12px; color: var(--primary); font-size: 14px; font-weight: 500; margin-bottom: 32px; }
    .payment-method { display: flex; align-items: center; gap: 20px; padding: 20px; background: var(--bg-surface); border: 2px solid var(--border-default); border-radius: 16px; cursor: pointer; transition: var(--transition); }
    .payment-method.selected { border-color: var(--primary); background: rgba(99,102,241,0.05); }
    .pm-icon { width: 56px; height: 56px; border-radius: 12px; background: var(--bg-card); border: 1px solid var(--border-subtle); display: flex; align-items: center; justify-content: center; }
    .pm-info { flex: 1; }
    .pm-info h4 { margin: 0 0 4px 0; color: var(--text-primary); font-size: 16px; }
    .pm-info p { margin: 0; color: var(--text-muted); font-size: 13px; }
    .pm-radio { width: 24px; height: 24px; border-radius: 50%; border: 2px solid var(--primary); display: flex; align-items: center; justify-content: center; }
    .radio-dot { width: 12px; height: 12px; background: var(--primary); border-radius: 50%; box-shadow: 0 0 10px var(--primary); }

    /* Summary Sidebar */
    .summary-card { padding: 40px; }
    .summary-title { font-family: var(--font-head); font-size: 1.5rem; color: var(--text-primary); margin-bottom: 32px; letter-spacing: 0.5px; }
    .summary-content { display: flex; flex-direction: column; gap: 20px; }
    .summary-row { display: flex; justify-content: space-between; align-items: center; font-size: 15px; }
    .summary-row .label { color: var(--text-muted); font-weight: 500; }
    .summary-row .value { color: var(--text-primary); font-weight: 700; font-size: 16px; }
    .discount-row .value { color: var(--success); }
    .summary-row .value.free { color: var(--success); background: rgba(16,185,129,0.15); padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: 800; letter-spacing: 1px; border: 1px solid rgba(16,185,129,0.3); }
    .summary-divider { height: 1px; background: var(--border-default); margin: 16px 0; border-radius: 1px; }
    .total-row { padding-top: 12px; align-items: flex-end; }
    .total-row .label { font-size: 18px; font-weight: 800; color: var(--text-primary); }
    .total-price { font-family: var(--font-head); font-size: 2.2rem !important; font-weight: 900 !important; color: var(--primary) !important; line-height: 1; }
    .secure-badge { margin-top: 40px; padding: 16px; background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: 12px; display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 13px; font-weight: 600; color: var(--text-muted); }

    /* Buttons */
    .step-footer { display: flex; justify-content: flex-end; margin-top: 32px; padding-top: 24px; border-top: 1px solid var(--border-default); }
    .step-footer.dual { justify-content: space-between; }
    .step-btn { display: inline-flex; align-items: center; gap: 12px; font-weight: 700; letter-spacing: 0.5px; padding: 16px 32px; border-radius: 14px; }
    
    .pulse-glow { animation: pulseGlow 2s infinite; }
    @keyframes pulseGlow { 0% { box-shadow: 0 0 0 0 rgba(99,102,241,0.4); } 70% { box-shadow: 0 0 0 15px rgba(99,102,241,0); } 100% { box-shadow: 0 0 0 0 rgba(99,102,241,0); } }

    /* Animations */
    .animate-fade-in { animation: fadeIn 0.4s ease-out forwards; }
    .slide-down { animation: slideDown 0.3s ease-out forwards; overflow: hidden; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
    @keyframes slideDown { from { opacity: 0; max-height: 0; transform: translateY(-10px); } to { opacity: 1; max-height: 600px; transform: translateY(0); } }

    /* Payment Animations */
    .payment-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.8); backdrop-filter: blur(8px); display: flex; align-items: center; justify-content: center; z-index: 9999; }
    .payment-modal { background: var(--bg-card); border: 1px solid var(--border); border-radius: 20px; padding: 40px; text-align: center; max-width: 400px; width: 90%; animation: popIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275); box-shadow: 0 25px 50px rgba(0,0,0,0.5); }
    @keyframes popIn { 0% { transform: scale(0.8); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }
    .spinner-xl { width: 64px; height: 64px; border: 4px solid rgba(99,102,241,0.2); border-top-color: var(--primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 24px; }
    
    .success-icon svg { width: 72px; height: 72px; border-radius: 50%; display: block; stroke-width: 3; stroke: #fff; stroke-miterlimit: 10; margin: 0 auto 24px; box-shadow: inset 0px 0px 0px var(--success); animation: fill .4s ease-in-out .4s forwards, scale .3s ease-in-out .9s both; }
    .success-icon svg circle { stroke-dasharray: 166; stroke-dashoffset: 166; stroke-width: 3; stroke-miterlimit: 10; stroke: var(--success); fill: none; animation: stroke 0.6s cubic-bezier(0.65, 0, 0.45, 1) forwards; }
    .success-icon svg path { transform-origin: 50% 50%; stroke-dasharray: 48; stroke-dashoffset: 48; animation: stroke 0.3s cubic-bezier(0.65, 0, 0.45, 1) 0.8s forwards; }
    
    .error-icon svg { width: 72px; height: 72px; display: block; stroke-width: 3; stroke: var(--danger); stroke-miterlimit: 10; margin: 0 auto 24px; }
    .error-icon svg circle { stroke-dasharray: 166; stroke-dashoffset: 166; stroke-width: 3; stroke: var(--danger); fill: none; animation: stroke 0.6s cubic-bezier(0.65, 0, 0.45, 1) forwards; }
    .error-icon svg path { stroke-dasharray: 60; stroke-dashoffset: 60; animation: stroke 0.3s cubic-bezier(0.65, 0, 0.45, 1) 0.6s forwards; }
    
    @keyframes stroke { 100% { stroke-dashoffset: 0; } }
    @keyframes scale { 0%, 100% { transform: none; } 50% { transform: scale3d(1.1, 1.1, 1); } }
    @keyframes fill { 100% { box-shadow: inset 0px 0px 0px 40px var(--success); } }

    @media (max-width: 1024px) { .checkout-layout { grid-template-columns: 1fr; } .checkout-summary { position: static; margin-top: 32px; } .step-line { margin: 0 8px; } }
    @media (max-width: 600px) { .form-grid { grid-template-columns: 1fr; } .step-text { display: none; } }
  `]
})
export class CheckoutComponent implements OnInit {
  step = 1;
  addresses: any[] = [];
  selectedAddressId = '';
  cart: any = null;
  placingOrder = false;
  paymentStatus: 'idle' | 'processing' | 'success' | 'failed' = 'idle';

  showAddAddress = false;
  savingAddress = false;
  newAddress = { fullName: '', phone: '', addressLine1: '', addressLine2: '', city: '', state: '', pincode: '', country: 'India' };
  isVerified = true;
  userEmail = '';

  constructor(
    private addressService: AddressService,
    private orderService: OrderService,
    private cartService: CartService,
    private authService: AuthService,
    private toast: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.addressService.getAll().subscribe({ next: a => {
      this.addresses = a;
      const def = a.find((x: any) => x.isDefault);
      if (def) this.selectedAddressId = def.publicAddressId;
    }, error: () => {} });
    this.cartService.cart$.subscribe(c => this.cart = c);
    this.authService.currentUser$.subscribe(u => {
      if (u) {
        this.isVerified = u.emailVerified !== false; // handle true or undefined
        this.userEmail = u.email;
      }
    });
  }

  getSubtotal(): number {
    if (!this.cart || !this.cart.items) return 0;
    return this.cart.items.reduce((acc: number, item: any) => acc + (item.price * item.quantity), 0);
  }

  saveNewAddress(): void {
    if (!this.newAddress.fullName || !this.newAddress.phone || !this.newAddress.addressLine1 || !this.newAddress.city || !this.newAddress.state || !this.newAddress.pincode) {
      this.toast.warning('Please fill all required fields');
      return;
    }
    this.savingAddress = true;
    this.addressService.add(this.newAddress).subscribe({
      next: (addr) => {
        this.savingAddress = false;
        this.showAddAddress = false;
        this.addresses.push(addr);
        this.selectedAddressId = addr.publicAddressId;
        this.newAddress = { fullName: '', phone: '', addressLine1: '', addressLine2: '', city: '', state: '', pincode: '', country: 'India' };
        this.toast.success('Address saved');
        // Auto-advance to next step
        this.step = 2;
      },
      error: () => {
        this.savingAddress = false;
        this.toast.error('Failed to save address');
      }
    });
  }

  verifyEmail(): void {
    if (!this.userEmail) return;
    this.authService.resendOtp(this.userEmail).subscribe({
      next: () => {
        this.toast.success('Verification OTP sent');
        this.router.navigate(['/verify-email'], { queryParams: { email: this.userEmail, returnUrl: '/checkout' } });
      },
      error: (e: any) => this.toast.error(e.error?.message || 'Failed to send OTP')
    });
  }

  placeOrder(): void {
    if (!this.selectedAddressId) { this.toast.warning('Select delivery address'); this.step = 1; return; }
    if (!this.isVerified) { this.toast.warning('Verify your email first'); return; }
    this.placingOrder = true;
    this.paymentStatus = 'processing';

    this.orderService.checkout({ addressPublicId: this.selectedAddressId }).subscribe({
      next: (order) => {
        this.initRazorpay(order);
      },
      error: (e) => {
        this.placingOrder = false;
        this.paymentStatus = 'failed';
        this.toast.error(e.error?.message || 'Failed to place order');
      }
    });
  }

  private initRazorpay(order: any): void {
    const options = {
      key: order.razorpayKeyId || 'rzp_test_xxx',
      amount: order.razorpayAmount,
      currency: 'INR',
      name: 'CognitoCart',
      description: `Order #${order.orderNumber || order.publicId}`,
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
      // Razorpay not loaded — redirect to orders (dev mode)
      this.toast.warning('Payment gateway not loaded. Order placed in pending state.');
      this.paymentStatus = 'success';
      setTimeout(() => {
        this.placingOrder = false;
        this.paymentStatus = 'idle';
        this.router.navigate(['/orders']);
      }, 2000);
    }
  }

  private verifyPayment(response: any, order: any): void {
    this.orderService.verifyPayment({
      razorpayOrderId: response.razorpay_order_id,
      razorpayPaymentId: response.razorpay_payment_id,
      razorpaySignature: response.razorpay_signature
    }).subscribe({
      next: () => {
        this.paymentStatus = 'success';
        this.cartService.refresh().subscribe();
        setTimeout(() => {
          this.placingOrder = false;
          this.paymentStatus = 'idle';
          this.router.navigate(['/orders']);
        }, 2500);
      },
      error: (e) => {
        this.paymentStatus = 'failed';
        this.placingOrder = false;
        this.toast.error(e.error?.message || 'Payment verification failed');
      }
    });
  }
}
