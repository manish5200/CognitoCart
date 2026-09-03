import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProductService } from '../../../services/product.service';
import { ReviewService } from '../../../services/review.service';
import { CartService } from '../../../services/cart.service';
import { WishlistService } from '../../../services/wishlist.service';
import { ToastService } from '../../../services/toast.service';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page" *ngIf="!loading && product">
      <!-- Breadcrumb -->
      <div class="breadcrumb" style="margin-bottom:24px;">
        <a routerLink="/" style="color:var(--text-muted); text-decoration:none; font-size:13px;">Home</a>
        <span class="sep" style="margin:0 8px; color:var(--border);">â€º</span>
        <a routerLink="/products" style="color:var(--text-muted); text-decoration:none; font-size:13px;">Products</a>
        <span class="sep" style="margin:0 8px; color:var(--border);">â€º</span>
        <span class="current" style="color:var(--text-primary); font-size:13px; font-weight:600;">{{product.productName}}</span>
      </div>

      <!-- 3-Column PDP Layout -->
      <div class="pdp-layout">
        
        <!-- COLUMN 1: Gallery -->
        <div class="pdp-gallery">
          <div class="image-thumbs" *ngIf="product.mediaGallery?.length > 1">
            <img *ngFor="let media of product.mediaGallery" [src]="media.mediaUrl"
              [class.active]="media.mediaUrl === selectedImage"
              (click)="selectedImage = media.mediaUrl" />
          </div>
          <div class="main-image">
            <img [src]="selectedImage || 'https://via.placeholder.com/600x600'" [alt]="product.productName" />
            <div class="img-badge" *ngIf="getDiscountPercent() > 0">-{{getDiscountPercent()}}% OFF</div>
          </div>
        </div>

        <!-- COLUMN 2: Product Info -->
        <div class="pdp-info">
          <div class="brand-badge" *ngIf="product.brand">{{product.brand}}</div>
          <h1 class="product-title">{{product.productName}}</h1>
          
          <div class="rating-summary" (click)="scrollToReviews()">
            <div class="stars">
              <span *ngFor="let s of stars(product.averageRating)">{{s}}</span>
            </div>
            <span class="rating-text">{{product.averageRating | number:'1.1-1'}} ({{product.totalReviews}} reviews)</span>
          </div>
          
          <div class="divider"></div>
          
          <div class="price-display">
            <span class="current-price">â‚¹{{(product.discountPrice || product.price) | number:'1.0-0'}}</span>
            <span *ngIf="product.discountPrice && product.discountPrice < product.price" class="original-price">â‚¹{{product.price | number:'1.0-0'}}</span>
            <span *ngIf="product.discountPrice && product.discountPrice < product.price" class="save-tag">You save â‚¹{{(product.price - product.discountPrice) | number:'1.0-0'}}</span>
          </div>

          <p class="product-desc">{{product.description}}</p>
          
          <!-- Variants -->
          <div class="variants-section" *ngIf="product.variants?.length > 0">
            <div class="variant-header">
              <span class="variant-label">Select Variant</span>
              <span class="variant-selected" *ngIf="selectedVariant">
                {{selectedVariant.color ? selectedVariant.color : ''}} 
                {{selectedVariant.size ? (selectedVariant.color ? 'Â· ' : '') + selectedVariant.size : ''}}
                {{!selectedVariant.color && !selectedVariant.size ? (selectedVariant.attributeValue || 'Standard') : ''}}
              </span>
            </div>
            <div class="variant-grid">
              <button *ngFor="let v of product.variants"
                [class.selected]="selectedVariant?.publicId === v.publicId"
                [disabled]="v.stockQuantity === 0"
                class="variant-btn"
                (click)="selectVariant(v)">
                <div class="v-title">
                  {{v.color ? v.color : ''}} 
                  {{v.size ? (v.color ? 'Â· ' : '') + v.size : ''}}
                  {{!v.color && !v.size ? (v.attributeValue || 'Standard') : ''}}
                </div>
                <div class="v-price" *ngIf="v.priceAdjustment">
                  {{v.priceAdjustment > 0 ? '+' : ''}}â‚¹{{v.priceAdjustment}}
                </div>
                <span class="stock-badge" *ngIf="v.stockQuantity === 0">Out of Stock</span>
              </button>
            </div>
          </div>

          <!-- Trust Badges -->
          <div class="trust-badges">
            <div class="trust-badge">
              <span class="icon">ðŸ›¡ï¸</span>
              <div>
                <strong>Secure Transaction</strong>
                <span>SSL encrypted</span>
              </div>
            </div>
            <div class="trust-badge">
              <span class="icon">âœ…</span>
              <div>
                <strong>CognitoCart Verified</strong>
                <span>Quality assured</span>
              </div>
            </div>
          </div>

          <!-- Product Specifications -->
          <details class="vendor-details" *ngIf="product.countryOfOrigin || product.condition || (product.attributes && (product.attributes | keyvalue).length > 0)">
            <summary>Product Specifications</summary>
            <div class="vd-content">
              <div class="vd-item" *ngIf="product.brand">
                <span class="vd-label">Brand</span>
                <span class="vd-value">{{product.brand}}</span>
              </div>
              <div class="vd-item" *ngIf="product.condition">
                <span class="vd-label">Condition</span>
                <span class="vd-value">{{product.condition}}</span>
              </div>
              <div class="vd-item" *ngIf="product.countryOfOrigin">
                <span class="vd-label">Country of Origin</span>
                <span class="vd-value">{{product.countryOfOrigin}}</span>
              </div>
              <div class="vd-item" *ngIf="product.productType">
                <span class="vd-label">Type</span>
                <span class="vd-value">{{product.productType}}</span>
              </div>
              
              <!-- Dynamic Attributes -->
              <ng-container *ngIf="product.attributes">
                <div class="vd-item" *ngFor="let attr of product.attributes | keyvalue">
                  <span class="vd-label">{{attr.key}}</span>
                  <span class="vd-value">{{attr.value}}</span>
                </div>
              </ng-container>
            </div>
          </details>

          <!-- Vendor Details -->
          <details class="vendor-details" *ngIf="product.storeName">
            <summary>Vendor Details</summary>
            <div class="vd-content">
              <div class="vd-item">
                <span class="vd-label">Sold By</span>
                <span class="vd-value">{{product.storeName}}</span>
              </div>
              <div class="vd-item" *ngIf="product.businessAddress">
                <span class="vd-label">Business Address</span>
                <span class="vd-value">{{product.businessAddress}}</span>
              </div>
            </div>
          </details>
        </div>

        <!-- COLUMN 3: Buy Box -->
        <div class="pdp-buybox">
          <div class="buy-card glass-card">
            <div class="buy-price">â‚¹{{(product.discountPrice || product.price) | number:'1.0-0'}}</div>
            
            <div class="stock-status" [class.low]="selectedVariant?.stockQuantity < 10" [class.out]="selectedVariant?.stockQuantity === 0">
              <span class="indicator"></span>
              {{selectedVariant ? (selectedVariant.stockQuantity > 0 ? (selectedVariant.stockQuantity < 10 ? 'Only ' + selectedVariant.stockQuantity + ' left in stock - order soon.' : 'In Stock') : 'Currently Unavailable') : 'Select a variant'}}
            </div>

            <div class="delivery-info">
              <div class="del-row">
                <span class="icon">ðŸšš</span>
                <div>
                  <strong>Free Delivery</strong>
                  <div class="sub">Dispatches within 24 hours</div>
                </div>
              </div>
            </div>
            
            <div class="qty-wrapper">
              <label>Quantity:</label>
              <div class="quantity-control">
                <button class="qty-btn" (click)="qty > 1 ? qty = qty - 1 : null" [disabled]="!selectedVariant || selectedVariant.stockQuantity === 0">âˆ’</button>
                <div class="qty-value">{{qty}}</div>
                <button class="qty-btn" (click)="qty = qty + 1" [disabled]="!selectedVariant || selectedVariant.stockQuantity === 0 || qty >= selectedVariant.stockQuantity">+</button>
              </div>
            </div>

            <div class="actions">
              <button class="btn btn-primary btn-block btn-cart" (click)="addToCart()" [disabled]="!selectedVariant || selectedVariant.stockQuantity === 0 || addingToCart">
                <span class="icon">ðŸ›’</span> {{addingToCart ? 'Adding to Cart...' : 'Add to Cart'}}
              </button>
              <button class="btn btn-secondary btn-block btn-wishlist" (click)="toggleWishlist()">
                <span class="icon">{{wishlisted ? 'â¤ï¸' : 'â™¡'}}</span> {{wishlisted ? 'Saved to Wishlist' : 'Add to Wishlist'}}
              </button>
            </div>

            <div class="rp-box" *ngIf="returnPolicy">
              <div class="rp-title">
                <span class="icon">â†©ï¸</span> {{returnPolicy.policyType}}
              </div>
              <div class="rp-desc">{{returnPolicy.windowDays}} days return window. {{returnPolicy.description}}</div>
            </div>
            <div class="rp-box" *ngIf="!returnPolicy">
              <div class="rp-title" style="color:var(--text-muted);">
                <span class="icon">ðŸš«</span> Non-Returnable
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="section-divider"></div>

      <!-- Reviews Section -->
      <div id="reviews-section" class="reviews-section">
        <h2 class="section-title">Customer Reviews</h2>
        
        <div class="reviews-layout">
          <!-- Left: Analytics -->
          <div class="reviews-analytics">
            <div class="rating-master glass-card">
              <div class="big-score">{{product.averageRating | number:'1.1-1'}}</div>
              <div class="stars-master">
                <span *ngFor="let s of stars(product.averageRating)">{{s}}</span>
              </div>
              <div class="total-ratings">{{product.totalReviews}} global ratings</div>
              
              <div class="dist-bars">
                <div *ngFor="let d of ratingDist" class="dist-row">
                  <div class="dist-label">{{d.stars}} star</div>
                  <div class="dist-bar-bg">
                    <div class="dist-bar-fill" [style.width.%]="d.percent"></div>
                  </div>
                  <div class="dist-pct">{{d.percent | number:'1.0-0'}}%</div>
                </div>
              </div>
              
              <div class="write-review-btn-area" *ngIf="isCustomer">
                <div class="divider"></div>
                <h4 style="margin-bottom:8px;">Review this product</h4>
                <p style="font-size:13px; color:var(--text-muted); margin-bottom:16px;">Share your thoughts with other customers</p>
                <button class="btn btn-secondary btn-block" (click)="showReviewForm = !showReviewForm">Write a product review</button>
              </div>
            </div>
          </div>
          
          <!-- Right: Feed -->
          <div class="reviews-feed">
            <div *ngIf="showReviewForm && isCustomer" class="write-review-form glass-card" style="margin-bottom:24px;">
              <h4 style="margin-bottom:16px;">Create Review</h4>
              <div style="display:flex; gap:8px; margin-bottom:16px;">
                <span *ngFor="let s of [1,2,3,4,5]" (click)="newReview.rating = s"
                  style="font-size:32px; cursor:pointer; transition:0.2s;"
                  [style.opacity]="s <= newReview.rating ? 1 : 0.2"
                  [style.transform]="s <= newReview.rating ? 'scale(1.1)' : 'scale(1)'">â­</span>
              </div>
              <textarea [(ngModel)]="newReview.comment" class="form-textarea" placeholder="What did you like or dislike?" rows="4"></textarea>
              <div style="display:flex; justify-content:flex-end; gap:12px; margin-top:16px;">
                <button class="btn btn-secondary" (click)="showReviewForm = false">Cancel</button>
                <button class="btn btn-primary" (click)="submitReview()" [disabled]="!newReview.rating || !newReview.comment">Submit</button>
              </div>
            </div>
            
            <div *ngIf="reviews.length === 0" class="empty-state glass-card">
              <div class="empty-icon">ðŸ’¬</div>
              <div class="empty-title">No reviews yet</div>
              <div class="empty-subtitle">Be the first to review this product!</div>
            </div>

            <div *ngFor="let r of reviews" class="review-card glass-card">
              <div class="reviewer-header">
                <div class="reviewer-avatar">{{r.reviewerName?.[0] || 'U'}}</div>
                <div class="reviewer-info">
                  <div class="name">{{r.reviewerName || 'Anonymous User'}}</div>
                  <div class="date">Reviewed on {{r.createdAt | date:'mediumDate'}}</div>
                </div>
              </div>
              <div class="review-rating-row">
                <div class="stars"><span *ngFor="let s of stars(r.rating)">{{s}}</span></div>
                <span class="verified-badge" *ngIf="r.verifiedPurchase">âœ… Verified Purchase</span>
              </div>
              <p class="review-body">{{r.comment}}</p>
              <div class="review-images" *ngIf="r.mediaGallery?.length">
                <img *ngFor="let media of r.mediaGallery" [src]="media.mediaUrl" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div class="loading-center" *ngIf="loading" style="min-height:70vh;">
      <div class="spinner"></div>
      <span style="margin-top:16px; color:var(--text-muted); font-weight:500;">Loading product details...</span>
    </div>

    <!-- Error State -->
    <div class="page" *ngIf="!loading && !product">
      <div class="empty-state glass-card" style="margin-top:40px;">
        <div class="empty-icon" style="font-size:64px;">ðŸ“¦</div>
        <div class="empty-title" style="font-size:24px;">Product Not Found</div>
        <div class="empty-subtitle" style="margin-bottom:24px;">The product you are looking for does not exist or has been removed.</div>
        <a routerLink="/products" class="btn btn-primary btn-lg">Back to Products</a>
      </div>
    </div>
  `,
  styles: [`
    /* 3-Column Layout */
    .pdp-layout { display: grid; grid-template-columns: 80px 1fr 1fr 340px; gap: 32px; align-items: start; }
    
    /* Gallery */
    .pdp-gallery { display: contents; }
    .image-thumbs { display: flex; flex-direction: column; gap: 12px; position: sticky; top: 100px; }
    .image-thumbs img { width: 64px; height: 64px; object-fit: cover; border-radius: 8px; border: 2px solid transparent; cursor: pointer; transition: 0.2s; opacity: 0.6; background: var(--bg-surface); }
    .image-thumbs img:hover, .image-thumbs img.active { border-color: var(--primary); opacity: 1; transform: scale(1.05); }
    .main-image { position: sticky; top: 100px; aspect-ratio: 1; border-radius: 16px; overflow: hidden; background: var(--bg-surface); border: 1px solid var(--border-default); display: flex; align-items: center; justify-content: center; cursor: crosshair; }
    .main-image img { width: 100%; height: 100%; object-fit: contain; transition: transform 0.3s ease; }
    .main-image:hover img { transform: scale(1.2); }
    .img-badge { position: absolute; top: 20px; left: 20px; background: linear-gradient(135deg, #ff416c, #ff4b2b); color: #fff; padding: 6px 16px; border-radius: 20px; font-weight: 800; font-size: 14px; box-shadow: 0 4px 12px rgba(255,75,43,0.3); z-index: 2; }
    
    /* Info */
    .pdp-info { display: flex; flex-direction: column; padding-top: 12px; }
    .brand-badge { color: var(--primary); font-weight: 700; text-transform: uppercase; letter-spacing: 1px; font-size: 13px; margin-bottom: 8px; }
    .product-title { font-family: var(--font-head); font-size: 2.2rem; font-weight: 800; line-height: 1.2; margin-bottom: 12px; color: var(--text-primary); }
    .rating-summary { display: flex; align-items: center; gap: 12px; cursor: pointer; margin-bottom: 24px; padding: 6px 12px; border-radius: 8px; margin-left: -12px; transition: 0.2s; }
    .rating-summary:hover { background: var(--bg-surface); }
    .stars { font-size: 16px; }
    .rating-text { color: var(--primary); font-weight: 500; font-size: 14px; }
    .rating-text:hover { text-decoration: underline; }
    .divider { height: 1px; background: var(--border-default); margin: 24px 0; }
    .price-display { display: flex; align-items: baseline; gap: 16px; margin-bottom: 24px; }
    .current-price { font-family: var(--font-head); font-size: 3rem; font-weight: 900; color: var(--text-primary); line-height: 1; }
    .original-price { font-size: 1.4rem; color: var(--text-dim); text-decoration: line-through; }
    .save-tag { font-size: 14px; font-weight: 600; color: var(--success); background: rgba(16,185,129,0.15); border: 1px solid rgba(16,185,129,0.3); padding: 4px 12px; border-radius: 4px; }
    .product-desc { font-size: 15px; line-height: 1.8; color: var(--text-secondary); margin-bottom: 32px; }
    
    .variant-header { display: flex; justify-content: space-between; margin-bottom: 12px; align-items: baseline; }
    .variant-label { font-weight: 700; color: #fff; font-size: 15px; }
    .variant-selected { color: var(--primary); font-size: 14px; font-weight: 600; }
    .variant-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 12px; }
    .variant-btn { background: var(--bg-surface); border: 1px solid var(--border-default); padding: 12px 16px; border-radius: 12px; color: var(--text-primary); cursor: pointer; transition: var(--transition); display: flex; flex-direction: column; align-items: center; gap: 4px; text-align: center; }
    .variant-btn .v-title { font-size: 13px; font-weight: 600; }
    .variant-btn .v-price { font-size: 11px; color: var(--text-muted); }
    .variant-btn:hover:not([disabled]) { border-color: rgba(99,102,241,0.5); transform: translateY(-2px); }
    .variant-btn.selected { border-color: var(--primary); background: rgba(99,102,241,0.15); box-shadow: 0 4px 12px rgba(99,102,241,0.2); }
    .variant-btn[disabled] { opacity: 0.4; cursor: not-allowed; border-style: dashed; }
    .stock-badge { display: block; font-size: 10px; color: var(--danger); text-transform: uppercase; font-weight: 800; background: rgba(239,68,68,0.1); padding: 2px 6px; border-radius: 4px; margin-top: 4px; }
    
    .trust-badges { display: flex; gap: 24px; margin-top: 40px; padding-top: 24px; border-top: 1px solid var(--border-default); }
    .trust-badge { display: flex; gap: 12px; align-items: flex-start; }
    .trust-badge .icon { font-size: 24px; }
    .trust-badge strong { display: block; font-size: 14px; color: #fff; margin-bottom: 2px; }
    .trust-badge span { display: block; font-size: 12px; color: var(--text-muted); }

    .vendor-details { margin-top: 24px; border-top: 1px solid var(--border-default); padding-top: 24px; }
    .vendor-details summary { font-size: 16px; font-weight: 700; color: var(--text-primary); cursor: pointer; list-style: none; display: flex; justify-content: space-between; align-items: center; }
    .vendor-details summary::-webkit-details-marker { display: none; }
    .vendor-details summary::after { content: 'â–¾'; font-size: 18px; color: var(--text-muted); transition: 0.3s; }
    .vendor-details[open] summary::after { transform: rotate(180deg); }
    .vd-content { padding-top: 16px; display: flex; flex-direction: column; gap: 16px; }
    .vd-item { display: flex; flex-direction: column; gap: 4px; }
    .vd-label { font-size: 13px; color: var(--text-muted); }
    .vd-value { font-size: 14px; color: var(--text-primary); font-weight: 500; }

    /* Buy Box */
    .pdp-buybox { position: sticky; top: 100px; }
    .buy-card { padding: 24px; }
    .buy-price { font-family: var(--font-head); font-size: 2rem; font-weight: 800; color: var(--text-primary); margin-bottom: 16px; }
    .stock-status { font-weight: 700; font-size: 15px; margin-bottom: 20px; display: flex; align-items: center; gap: 8px; color: var(--success); }
    .stock-status .indicator { width: 8px; height: 8px; border-radius: 50%; background: var(--success); box-shadow: 0 0 8px var(--success); }
    .stock-status.low { color: var(--warning); }
    .stock-status.low .indicator { background: var(--warning); box-shadow: 0 0 8px var(--warning); }
    .stock-status.out { color: var(--danger); }
    .stock-status.out .indicator { background: var(--danger); box-shadow: 0 0 8px var(--danger); }
    
    .delivery-info { margin-bottom: 24px; }
    .del-row { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 12px; }
    .del-row .icon { font-size: 20px; }
    .del-row strong { font-size: 14px; color: #fff; display: block; margin-bottom: 2px; }
    .del-row .sub { font-size: 13px; color: var(--text-muted); }
    
    .qty-wrapper { margin-bottom: 24px; }
    .qty-wrapper label { display: block; font-size: 13px; font-weight: 600; color: var(--text-muted); margin-bottom: 8px; }
    .quantity-control { display: flex; align-items: center; background: rgba(0,0,0,0.2); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; width: fit-content; overflow: hidden; }
    .qty-btn { background: transparent; border: none; color: #fff; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; font-size: 18px; cursor: pointer; transition: 0.2s; }
    .qty-btn:hover:not([disabled]) { background: rgba(255,255,255,0.1); }
    .qty-btn[disabled] { opacity: 0.3; cursor: not-allowed; }
    .qty-value { padding: 0 16px; font-weight: 600; font-size: 15px; min-width: 40px; text-align: center; }
    
    .actions { display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }
    .btn-block { width: 100%; padding: 14px; font-size: 15px; font-weight: 700; border-radius: 12px; display: flex; align-items: center; justify-content: center; gap: 8px; transition: transform 0.2s, box-shadow 0.2s; }
    .btn-block:active:not([disabled]) { transform: scale(0.98); }
    .btn-cart { box-shadow: 0 4px 14px rgba(99,102,241,0.4); }
    .btn-cart:hover:not([disabled]) { box-shadow: 0 6px 20px rgba(99,102,241,0.6); transform: translateY(-2px); }
    .btn-wishlist { background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); }
    .btn-wishlist:hover { background: rgba(255,255,255,0.1); border-color: rgba(255,255,255,0.2); transform: translateY(-2px); }
    
    .rp-box { padding-top: 20px; border-top: 1px solid var(--border-default); }
    .rp-title { font-weight: 600; font-size: 14px; color: var(--text-primary); display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
    .rp-desc { font-size: 13px; color: var(--text-muted); line-height: 1.5; }

    .section-divider { height: 1px; background: linear-gradient(90deg, transparent, var(--border-default), transparent); margin: 64px 0; }
    .section-title { font-family: var(--font-head); font-size: 2rem; font-weight: 800; text-align: center; margin-bottom: 40px; }

    /* Reviews Section */
    .reviews-layout { display: grid; grid-template-columns: 320px 1fr; gap: 48px; }
    .rating-master { padding: 32px; text-align: center; position: sticky; top: 100px; }
    .big-score { font-family: var(--font-head); font-size: 4.5rem; font-weight: 900; color: var(--text-primary); line-height: 1; margin-bottom: 8px; }
    .stars-master { font-size: 24px; margin-bottom: 12px; }
    .total-ratings { font-size: 14px; color: var(--text-muted); font-weight: 500; margin-bottom: 24px; }
    .dist-bars { display: flex; flex-direction: column; gap: 12px; }
    .dist-row { display: flex; align-items: center; gap: 12px; font-size: 13px; }
    .dist-label { width: 45px; text-align: right; color: var(--primary); font-weight: 600; }
    .dist-bar-bg { flex: 1; height: 8px; background: var(--bg-surface); border-radius: 4px; overflow: hidden; border: 1px solid var(--border-default); }
    .dist-bar-fill { height: 100%; background: var(--warning); border-radius: 4px; }
    .dist-pct { width: 40px; text-align: left; color: var(--text-muted); font-weight: 500; }
    .write-review-btn-area { margin-top: 32px; }

    .reviews-feed { display: flex; flex-direction: column; gap: 20px; }
    .write-review-form { padding: 24px; }
    .review-card { padding: 24px; transition: var(--transition); }
    .review-card:hover { transform: translateY(-2px); border-color: rgba(99,102,241,0.5); box-shadow: var(--shadow-sm); }
    .reviewer-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
    .reviewer-avatar { width: 48px; height: 48px; border-radius: 50%; background: linear-gradient(135deg, var(--primary), var(--secondary)); display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 800; color: #fff; box-shadow: 0 4px 10px rgba(99,102,241,0.3); }
    .reviewer-info .name { font-weight: 700; font-size: 15px; color: var(--text-primary); margin-bottom: 2px; }
    .reviewer-info .date { font-size: 12px; color: var(--text-dim); }
    .review-rating-row { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
    .verified-badge { font-size: 12px; font-weight: 600; color: var(--success); }
    .review-body { font-size: 15px; color: var(--text-secondary); line-height: 1.6; margin-bottom: 16px; }
    .review-images { display: flex; gap: 12px; flex-wrap: wrap; }
    .review-images img { width: 80px; height: 80px; object-fit: cover; border-radius: 8px; border: 1px solid var(--border-default); }

    /* Responsive */
    @media (max-width: 1200px) {
      .pdp-layout { grid-template-columns: 80px 1fr 300px; gap: 24px; }
      .pdp-info { grid-column: 2 / -1; }
      .pdp-buybox { grid-column: 1 / -1; position: static; }
    }
    @media (max-width: 900px) {
      .pdp-layout { grid-template-columns: 1fr; }
      .pdp-gallery { display: flex; flex-direction: column-reverse; }
      .image-thumbs { position: static; flex-direction: row; overflow-x: auto; }
      .main-image { position: static; }
      .pdp-info, .pdp-buybox { grid-column: 1; }
      .reviews-layout { grid-template-columns: 1fr; }
      .rating-master { position: static; }
    }
  `]
})
export class ProductDetailComponent implements OnInit {
  product: any = null;
  reviews: any[] = [];
  ratingDist: any[] = [];
  returnPolicy: any = null;
  loading = true;
  selectedImage = '';
  selectedVariant: any = null;
  qty = 1;
  wishlisted = false;
  addingToCart = false;
  newReview = { rating: 0, comment: '' };

  showReviewForm = false;

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    private reviewService: ReviewService,
    private cartService: CartService,
    private wishlistService: WishlistService,
    private toast: ToastService,
    public auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (slug) this.loadProduct(slug);
  }

  scrollToReviews(): void {
    const element = document.getElementById('reviews-section');
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  loadProduct(slug: string): void {
    this.productService.getBySlug(slug).subscribe({
      next: (p) => {
        this.product = p;
        this.selectedImage = p.mediaGallery?.[0]?.mediaUrl || '';
        if (p.variants?.length) this.selectedVariant = p.variants[0];
        this.loading = false;
        const pubId = p.productPublicId || p.publicId;
        this.loadReviews(pubId);
        this.loadReturnPolicy(pubId);
        this.loadVariants(pubId);
      },
      error: () => { this.loading = false; }
    });
  }

  loadVariants(id: string): void {
    this.productService.getVariants(id).subscribe({
      next: (variants: any[]) => {
        if (!this.product) return;
        this.product.variants = variants;
        if (variants?.length) this.selectedVariant = variants[0];
      },
      error: () => {}
    });
  }

  loadReviews(id: string): void {
    this.reviewService.getReviews(id).subscribe({ next: r => this.reviews = r, error: () => {} });
    this.reviewService.getRatingDistribution(id).subscribe({
      next: (dist) => {
        const total = Object.values(dist as Record<string, number>).reduce((a: number, b: number) => a + b, 0) || 1;
        this.ratingDist = [5,4,3,2,1].map(s => ({
          stars: s, count: (dist as any)[`${s}star`] || 0,
          percent: (((dist as any)[`${s}star`] || 0) / total) * 100
        }));
      }, error: () => {}
    });
  }

  loadReturnPolicy(id: string): void {
    this.productService.getReturnPolicy(id).subscribe({ next: p => this.returnPolicy = p, error: () => {} });
  }

  selectVariant(v: any): void { this.selectedVariant = v; }

  stars(rating: number): string[] {
    if (!rating) return [];
    return Array(5).fill('').map((_, i) => i < Math.round(rating) ? 'â­' : 'â˜†');
  }

  getDiscountPercent(): number {
    if (this.product && this.product.price && this.product.discountPrice && this.product.discountPrice < this.product.price) {
      return Math.round(((this.product.price - this.product.discountPrice) / this.product.price) * 100);
    }
    return 0;
  }

  addToCart(): void {
    if (!this.selectedVariant) { this.toast.warning('Select a variant'); return; }
    this.addingToCart = true;
    const variantId = this.selectedVariant.variantPublicId || this.selectedVariant.publicId;
    
    // Pass product details for guest cart
    const productDetails = {
      productName: this.product.productName,
      productImageUrl: this.product.mediaGallery?.[0]?.mediaUrl,
      price: this.selectedVariant.priceAdjustment ? (this.product.discountPrice || this.product.price) + this.selectedVariant.priceAdjustment : (this.product.discountPrice || this.product.price),
      variantInfo: this.selectedVariant.color || this.selectedVariant.size || this.selectedVariant.attributeValue || 'Standard'
    };

    this.cartService.add(variantId, this.qty, productDetails).subscribe({
      next: () => { this.addingToCart = false; this.toast.success('Added to cart!'); },
      error: (e) => { this.addingToCart = false; this.toast.error(e.error?.message || 'Failed to add'); }
    });
  }

  toggleWishlist(): void {
    if (!this.auth.isLoggedIn()) { this.toast.warning('Login to wishlist'); return; }
    const pubId = this.product.productPublicId || this.product.publicId;
    this.wishlistService.toggle(pubId).subscribe({
      next: (res: any) => { this.wishlisted = !this.wishlisted; this.toast.info(res.Status || 'Wishlist updated'); },
      error: () => this.toast.error('Failed')
    });
  }

  submitReview(): void {
    const pubId = this.product.productPublicId || this.product.publicId;
    this.reviewService.postReview(pubId, this.newReview).subscribe({
      next: () => {
        this.toast.success('Review submitted!');
        this.newReview = { rating: 0, comment: '' };
        this.loadReviews(pubId);
      },
      error: (e) => this.toast.error(e.error?.message || 'Review failed')
    });
  }

  get isCustomer(): boolean { return this.auth.isCustomer(); }
}

