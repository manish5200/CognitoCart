import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { WishlistService } from '../../../services/wishlist.service';
import { CartService } from '../../../services/cart.service';
import { ToastService } from '../../../services/toast.service';
import { ProductCardComponent } from '../../shared/product-card/product-card.component';

@Component({
  selector: 'app-wishlist',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <h1 class="page-title" style="display:flex; align-items:center; gap:12px;">
          <div style="background:var(--grad-brand); border-radius:8px; padding:6px; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 14px rgba(99,102,241,0.3);">
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          </div>
          My Wishlist
        </h1>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div *ngIf="!loading">
        <!-- Summary Card -->
        <div class="card" style="margin-bottom:24px; padding:20px;" *ngIf="summary">
          <div class="card-body" style="display:flex; gap:32px;">
            <div>
              <div class="stat-value" style="font-size:1.5rem;">{{summary.itemCount || 0}}</div>
              <div class="stat-label">Items</div>
            </div>
            <div>
              <div class="stat-value" style="font-size:1.5rem; color:var(--primary);">\u20B9{{summary.totalValue | number:'1.0-0'}}</div>
              <div class="stat-label">Total Value</div>
            </div>
            <div *ngIf="summary.potentialSavings > 0">
              <div class="stat-value" style="font-size:1.5rem; color:var(--accent);">\u20B9{{summary.potentialSavings | number:'1.0-0'}}</div>
              <div class="stat-label">Potential Savings</div>
            </div>
          </div>
        </div>

        <div class="grid-4" *ngIf="items.length > 0">
          <div *ngFor="let item of items" style="position: relative; display: flex; flex-direction: column; gap: 8px;">
            <app-product-card 
              [product]="item" 
              [showWishlist]="true" 
              (toggleWishlist)="remove(item, $event)">
            </app-product-card>
            <button class="btn btn-primary btn-sm" style="width: 100%; display:flex; align-items:center; justify-content:center; gap:6px;" (click)="moveToCart(item)">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z" /></svg>
              Move to Cart
            </button>
          </div>
        </div>

        <div class="empty-state" *ngIf="items.length === 0" style="min-height:400px; display:flex; flex-direction:column; align-items:center; justify-content:center; color:var(--text-muted);">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:48px;height:48px;margin-bottom:16px;color:var(--text-dim);"><path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" /></svg>
          <div class="empty-title" style="font-size:18px; font-weight:600;">Your wishlist is empty</div>
          <div class="empty-subtitle">Save items you love for later</div>
          <a routerLink="/products" class="btn btn-primary">Browse Products</a>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class WishlistComponent implements OnInit {
  items: any[] = [];
  summary: any = null;
  loading = true;

  constructor(private wishlistService: WishlistService, private cartService: CartService, private toast: ToastService) {}

  ngOnInit(): void {
    this.load();
    this.wishlistService.getSummary().subscribe({ next: s => this.summary = s['Wishlist Summary'] || s, error: () => {} });
  }

  load(): void {
    this.wishlistService.getWishlist().subscribe({
      next: (res) => { 
        this.items = (res as any).items ?? res ?? [];
        // Wishlist API returns ProductResponse instances, map `isWishlisted` explicitly
        // so the heart button on the app-product-card is correctly filled in.
        this.items = this.items.map(item => ({...item, isWishlisted: true}));
        this.loading = false; 
      },
      error: () => { this.loading = false; }
    });
  }

  remove(item: any, e: Event): void {
    e.stopPropagation();
    this.wishlistService.toggle(item.productPublicId || item.productId).subscribe({
      next: () => { this.items = this.items.filter(i => i !== item); this.toast.info('Removed from wishlist'); },
      error: () => this.toast.error('Failed to remove')
    });
  }

  moveToCart(item: any): void {
    this.wishlistService.moveToCart(item.productPublicId || item.productId, 1).subscribe({
      next: () => { this.toast.success('Moved to cart!'); this.cartService.refresh().subscribe(); this.load(); },
      error: (e) => this.toast.error(e.error?.message || 'Failed to move')
    });
  }
}
