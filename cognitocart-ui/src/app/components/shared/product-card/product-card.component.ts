import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div
      class="product-card group"
      [routerLink]="['/product', product.slug || product.productPublicId || product.publicId]"
    >
      <!-- Image -->
      <div class="pc-img-wrap">
        <img
          [src]="product.mediaGallery?.[0]?.mediaUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&auto=format&fit=crop'"
          [alt]="product.productName || product.name"
          class="pc-img main-img"
          loading="lazy"
        />
        <img
          *ngIf="product.mediaGallery?.[1]?.mediaUrl"
          [src]="product.mediaGallery[1]?.mediaUrl"
          [alt]="product.productName || product.name"
          class="pc-img hover-img"
          loading="lazy"
        />
        <div class="pc-badges">
          <span *ngIf="product.discountPercentage > 0" class="pc-badge pc-badge-discount">-{{product.discountPercentage}}%</span>
          <span *ngIf="product.flashSaleActive" class="pc-badge pc-badge-flash">âš¡ FLASH</span>
        </div>
        <button
          *ngIf="showWishlist"
          class="pc-wishlist-btn"
          [class.active]="product.isWishlisted || product._wishlisted"
          (click)="$event.preventDefault(); $event.stopPropagation(); toggleWishlist.emit(product)"
          [title]="(product.isWishlisted || product._wishlisted) ? 'Remove from wishlist' : 'Add to wishlist'"
        >
          <svg *ngIf="!(product.isWishlisted || product._wishlisted)" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" /></svg>
          <svg *ngIf="product.isWishlisted || product._wishlisted" fill="currentColor" viewBox="0 0 24 24"><path d="m11.645 20.91-.007-.003-.022-.012a15.247 15.247 0 0 1-.383-.218 25.18 25.18 0 0 1-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.688 3A5.5 5.5 0 0 1 12 5.052 5.5 5.5 0 0 1 16.313 3c2.973 0 5.437 2.322 5.437 5.25 0 3.925-2.438 7.111-4.739 9.256a25.175 25.175 0 0 1-4.244 3.17 15.247 15.247 0 0 1-.383.219l-.022.012-.007.004-.003.001a.752.752 0 0 1-.704 0l-.003-.001Z" /></svg>
        </button>
      </div>

      <!-- Info -->
      <div class="pc-body">
        <div class="pc-brand-row">
          <span class="pc-brand">{{product.brand || product.categoryName || 'General'}}</span>
          <div class="pc-rating" *ngIf="product.averageRating">
            <span class="pc-stars">â˜…</span>
            <span class="pc-rating-val">{{product.averageRating | number:'1.1-1'}}</span>
            <span class="pc-review-count">({{product.totalReviews || 0}})</span>
          </div>
        </div>
        
        <h3 class="pc-name">{{product.productName || product.name}}</h3>
        
        <div class="pc-price">
          <span class="pc-price-now">â‚¹{{(product.discountPrice || product.price) | number:'1.0-0'}}</span>
          <span *ngIf="product.discountPrice && product.price > product.discountPrice" class="pc-price-was">â‚¹{{product.price | number:'1.0-0'}}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .product-card {
      background: var(--bg-surface);
      border: 1px solid rgba(255,255,255,0.03);
      border-radius: 12px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      cursor: pointer;
      transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      position: relative;
      height: 100%;
    }
    .product-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 32px rgba(0,0,0,0.4);
      border-color: rgba(255,255,255,0.08);
    }
    
    .pc-img-wrap {
      position: relative;
      aspect-ratio: 1/1;
      overflow: hidden;
      background: var(--bg-glass);
      border-radius: 12px 12px 0 0;
    }
    .pc-img {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: opacity 0.4s ease, transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .hover-img {
      opacity: 0;
    }
    .product-card:hover .main-img {
      transform: scale(1.05);
    }
    .product-card:hover .hover-img {
      opacity: 1;
      transform: scale(1.05);
    }
    
    .pc-badges {
      position: absolute;
      top: 12px;
      left: 12px;
      display: flex;
      flex-direction: column;
      gap: 6px;
      z-index: 2;
    }
    .pc-badge {
      color: #fff;
      font-size: 11px;
      font-weight: 700;
      padding: 4px 10px;
      border-radius: 6px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.2);
    }
    .pc-badge-discount {
      background: rgba(4, 6, 15, 0.7);
      backdrop-filter: blur(8px);
      border: 1px solid rgba(255,255,255,0.1);
      color: var(--success);
    }
    .pc-badge-flash {
      background: linear-gradient(135deg, #ef4444, #f97316);
    }
    
    .pc-wishlist-btn {
      position: absolute;
      top: 12px;
      right: 12px;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: rgba(4, 6, 15, 0.5);
      border: 1px solid rgba(255,255,255,0.1);
      backdrop-filter: blur(8px);
      cursor: pointer;
      color: var(--text-primary);
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
      opacity: 0;
      transform: translateY(-5px);
      z-index: 2;
    }
    .pc-wishlist-btn svg {
      width: 16px;
      height: 16px;
    }
    .product-card:hover .pc-wishlist-btn {
      opacity: 1;
      transform: translateY(0);
    }
    .pc-wishlist-btn.active {
      opacity: 1;
      transform: translateY(0);
      color: #ef4444;
      background: rgba(4, 6, 15, 0.8);
    }
    .pc-wishlist-btn:hover {
      transform: scale(1.1) !important;
      background: rgba(4, 6, 15, 0.8);
    }
    
    .pc-body {
      padding: 16px;
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    
    .pc-brand-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2px;
    }
    
    .pc-brand {
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.05em;
      color: var(--text-dim);
      text-transform: uppercase;
    }
    
    .pc-name {
      font-size: 15px;
      font-weight: 600;
      color: var(--text-primary);
      line-height: 1.4;
      margin: 0;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    
    .pc-rating {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
    }
    .pc-stars {
      color: var(--warning);
      font-size: 10px;
    }
    .pc-rating-val {
      color: var(--text-primary);
      font-weight: 700;
    }
    .pc-review-count {
      color: var(--text-dim);
      font-size: 11px;
    }
    
    .pc-price {
      display: flex;
      align-items: baseline;
      gap: 8px;
      margin-top: auto;
      padding-top: 12px;
    }
    .pc-price-now {
      font-family: var(--font-head);
      font-size: 1.1rem;
      font-weight: 700;
      color: var(--text-primary);
    }
    .pc-price-was {
      font-size: 13px;
      color: var(--text-dim);
      text-decoration: line-through;
    }

  `]
})
export class ProductCardComponent {
  @Input() product: any;
  @Input() showWishlist: boolean = true;
  @Output() toggleWishlist = new EventEmitter<any>();
}

