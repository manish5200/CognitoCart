import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { CartService } from '../../services/cart.service';
import { ToastService } from '../../services/toast.service';
import { ProductReturnStatusComponent } from '../shared/product-return-status/product-return-status.component';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ProductReturnStatusComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <h1 class="page-title">&#128722; Shopping Cart</h1>
        <p class="page-subtitle" *ngIf="cart?.items?.length">{{cart.items.length}} item(s) in your cart</p>
      </div>

      <div class="loading-center" *ngIf="loading">
        <div class="spinner"></div>
      </div>

      <div *ngIf="!loading">
        <div *ngIf="cart?.items?.length > 0" class="cart-layout">
          <!-- Cart Items -->
          <div class="cart-items-panel fade-up">
            <div class="glass-card" style="padding:0;">
              <div class="cart-header">
                <h3 class="table-title">Cart Items</h3>
                <button class="btn btn-outline btn-sm" (click)="clearCart()" style="color:var(--danger); border-color:rgba(239, 68, 68, 0.3);">
                  🗑️ Clear All
                </button>
              </div>

              <div *ngFor="let item of cart.items" class="cart-item">
                <img [src]="item.productImageUrl || item.imageUrl || 'https://via.placeholder.com/80'" class="cart-item-img" [alt]="item.productName" />

                <div class="cart-item-info">
                  <div class="cart-item-title">{{item.productName}}</div>
                  <div class="cart-item-variant">
                    {{item.variantInfo || item.color || item.size || 'Standard'}}
                  </div>
                  <div class="cart-item-price">\u20B9{{item.price | number:'1.0-0'}}</div>
                  
                  <div style="margin-top:8px;">
                    <app-product-return-status [isReturnable]="item.policyType !== 'NON_RETURNABLE'"></app-product-return-status>
                  </div>
                </div>

                <div class="cart-item-actions">
                  <div class="quantity-control">
                    <button class="qty-btn" (click)="updateQty(item, item.quantity - 1)">&#8722;</button>
                    <div class="qty-value">{{item.quantity}}</div>
                    <button class="qty-btn" (click)="updateQty(item, item.quantity + 1)">+</button>
                  </div>

                  <div class="cart-item-total">
                    \u20B9{{item.price * item.quantity | number:'1.0-0'}}
                  </div>

                  <button class="btn-icon delete-btn" (click)="removeItem(item)" title="Remove">
                    <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none"><path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Order Summary -->
          <div class="cart-summary fade-up fade-up-delay-1">
            <div class="glass-card summary-card">
              <h3 style="margin-bottom:24px; font-family:var(--font-head); font-size:1.4rem;">Order Summary</h3>

              <!-- Coupon -->
              <div class="form-group">
                <label class="form-label" style="font-size:13px;">Coupon Code</label>
                <div style="display:flex; gap:8px;">
                  <input type="text" [(ngModel)]="couponCode" class="form-control" placeholder="Enter code" style="padding:10px 14px;" />
                  <button class="btn btn-secondary" (click)="applyCoupon()" style="padding:10px 16px;">Apply</button>
                </div>
                <div *ngIf="cart.appliedCoupon" class="coupon-success">
                  <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none"><path d="M5 13l4 4L19 7" stroke-linecap="round" stroke-linejoin="round"/></svg>
                  Coupon "{{cart.appliedCoupon}}" applied!
                </div>
              </div>

              <hr class="divider" style="margin: 24px 0;" />

              <div class="summary-row">
                <span>Subtotal ({{cart.items.length}} items)</span>
                <span>\u20B9{{cart.subtotalAmount | number:'1.0-0'}}</span>
              </div>
              <div class="summary-row" *ngIf="cart.discountAmount > 0" style="color:var(--success);">
                <span>Discount</span>
                <span>&#8722;\u20B9{{cart.discountAmount | number:'1.0-0'}}</span>
              </div>
              <div class="summary-row">
                <span>Shipping</span>
                <span style="color:var(--success); font-weight:600;">FREE</span>
              </div>

              <hr class="divider" style="margin: 24px 0;" />

              <div class="summary-row total">
                <span>Total</span>
                <span class="total-price">\u20B9{{cart.totalAmount | number:'1.0-0'}}</span>
              </div>

              <button class="btn btn-primary btn-full btn-lg" (click)="checkout()" style="margin-top:28px; font-size:16px;">
                Proceed to Checkout
                <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none" style="margin-left:8px;"><path stroke-linecap="round" stroke-linejoin="round" d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
              </button>

              <div style="text-align:center; margin-top:16px;">
                <a routerLink="/products" class="auth-link" style="font-size:14px; font-weight:600;">
                  Continue Shopping
                </a>
              </div>
            </div>
          </div>
        </div>

        <!-- Empty Cart -->
        <div *ngIf="!cart?.items?.length" class="empty-state fade-up">
          <div class="empty-icon" style="font-size:3rem; margin-bottom:16px;">🛍️</div>
          <div class="empty-title" style="font-family:var(--font-head); font-size:1.8rem; margin-bottom:8px; color:var(--text-primary);">Your cart is empty</div>
          <div class="empty-subtitle" style="color:var(--text-muted); margin-bottom:24px;">Looks like you haven't added anything to your cart yet.</div>
          <a routerLink="/products" class="btn btn-primary btn-lg" style="padding: 12px 32px;">Start Shopping</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .cart-layout { display: grid; grid-template-columns: 1fr 380px; gap: 32px; align-items: start; }
    .cart-items-panel { min-width: 0; }
    .cart-summary { position: sticky; top: 100px; }
    
    .cart-header { display: flex; justify-content: space-between; align-items: center; padding: 24px; border-bottom: 1px solid var(--border-default); }
    .table-title { font-family: var(--font-head); font-size: 1.25rem; font-weight: 700; color: var(--text-primary); margin: 0; }
    
    .cart-item { display: flex; align-items: center; gap: 24px; padding: 24px; border-bottom: 1px solid var(--border-subtle); transition: var(--transition); }
    .cart-item:last-child { border-bottom: none; }
    .cart-item:hover { background: rgba(99, 102, 241, 0.03); }
    
    .cart-item-img { width: 96px; height: 96px; object-fit: contain; border-radius: 12px; background: var(--bg-surface); border: 1px solid var(--border-subtle); padding: 8px; }
    
    .cart-item-info { flex: 1; min-width: 0; }
    .cart-item-title { font-weight: 700; font-size: 15px; color: var(--text-primary); margin-bottom: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .cart-item-variant { font-size: 13px; color: var(--text-muted); margin-bottom: 8px; }
    .cart-item-price { font-weight: 800; font-size: 15px; color: var(--primary); }
    
    .cart-item-actions { display: flex; align-items: center; gap: 24px; }
    
    .quantity-control { display: flex; align-items: center; background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: 8px; overflow: hidden; }
    .qty-btn { background: transparent; border: none; color: var(--text-secondary); width: 36px; height: 36px; font-size: 16px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: var(--transition); }
    .qty-btn:hover { background: rgba(99, 102, 241, 0.1); color: var(--primary); }
    .qty-value { width: 40px; text-align: center; font-size: 14px; font-weight: 700; color: var(--text-primary); }
    
    .cart-item-total { font-weight: 800; font-size: 16px; width: 90px; text-align: right; color: var(--text-primary); }
    
    .delete-btn { color: var(--text-muted); padding: 8px; border-radius: 8px; transition: var(--transition); display: flex; align-items: center; justify-content: center; }
    .delete-btn:hover { background: rgba(239, 68, 68, 0.1); color: var(--danger); }
    
    .summary-card { padding: 32px; }
    .summary-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; font-size: 14px; color: var(--text-secondary); font-weight: 500; }
    .summary-row.total { font-size: 18px; font-weight: 700; color: var(--text-primary); }
    .total-price { font-size: 1.8rem; font-weight: 900; color: var(--primary); font-family: var(--font-head); letter-spacing: -0.02em; }
    
    .coupon-success { margin-top: 10px; font-size: 13px; color: var(--success); display: flex; align-items: center; gap: 6px; font-weight: 500; background: rgba(16, 185, 129, 0.1); padding: 8px 12px; border-radius: 8px; }
    
    .auth-link { color: var(--text-muted); text-decoration: none; transition: var(--transition); }
    .auth-link:hover { color: var(--primary); }
    
    @media (max-width: 900px) { 
      .cart-layout { grid-template-columns: 1fr; gap: 24px; } 
      .cart-summary { position: static; } 
    }
    @media (max-width: 600px) {
      .cart-item { flex-direction: column; align-items: flex-start; gap: 16px; }
      .cart-item-actions { width: 100%; justify-content: space-between; }
    }
  `]
})
export class CartComponent implements OnInit {
  cart: any = null;
  loading = true;
  couponCode = '';

  constructor(
    private cartService: CartService,
    private toast: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cartService.cart$.subscribe(cart => {
      this.cart = cart;
      this.loading = false;
    });
    this.cartService.refresh().subscribe();
  }

  updateQty(item: any, newQty: number): void {
    if (newQty < 1) { this.removeItem(item); return; }
    this.cartService.update(item.variantPublicId || item.publicId, newQty).subscribe({
      error: (e) => this.toast.error(e.error?.message || 'Failed to update')
    });
  }

  removeItem(item: any): void {
    this.cartService.removeItem(item.variantPublicId || item.publicId).subscribe({
      next: () => this.toast.success('Item removed'),
      error: () => this.toast.error('Failed to remove item')
    });
  }

  clearCart(): void {
    if (!confirm('Clear all items from cart?')) return;
    this.cartService.clear().subscribe({ next: () => this.toast.success('Cart cleared') });
  }

  applyCoupon(): void {
    if (!this.couponCode.trim()) return;
    this.cartService.applyCoupon(this.couponCode.trim()).subscribe({
      next: () => { this.toast.success('Coupon applied!'); this.couponCode = ''; },
      error: (e) => this.toast.error(e.error?.message || 'Invalid coupon code')
    });
  }

  checkout(): void { this.router.navigate(['/checkout']); }
}
