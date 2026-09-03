import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { ProductService } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { WishlistService } from '../../services/wishlist.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <!-- Loading State -->
    <div *ngIf="loading" class="pdp-loading">
      <div class="pdp-skeleton-wrap">
        <div class="skeleton pdp-sk-img"></div>
        <div style="flex:1; display:flex; flex-direction:column; gap:16px;">
          <div class="skeleton" style="height:32px; width:70%;"></div>
          <div class="skeleton" style="height:20px; width:40%;"></div>
          <div class="skeleton" style="height:48px; width:50%;"></div>
          <div class="skeleton" style="height:120px;"></div>
          <div style="display:flex; gap:10px;">
            <div class="skeleton" style="height:48px; flex:1;"></div>
            <div class="skeleton" style="height:48px; flex:1;"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Not Found -->
    <div *ngIf="!loading && !product" class="empty-state" style="min-height:60vh;">
      <div class="empty-icon">ðŸ”</div>
      <div class="empty-title">Product not found</div>
      <div class="empty-subtitle">It may have been removed or the link is incorrect.</div>
      <a routerLink="/products" class="btn btn-primary">Browse Products</a>
    </div>

    <!-- Product Detail -->
    <div *ngIf="!loading && product" class="pdp-page">
      <!-- Breadcrumb -->
      <div class="pdp-breadcrumb">
        <a routerLink="/" style="color:var(--text-muted); text-decoration:none;">Home</a>
        <span style="color:var(--border-default);">â€º</span>
        <a routerLink="/products" style="color:var(--text-muted); text-decoration:none;">Products</a>
        <span style="color:var(--border-default);">â€º</span>
        <span style="color:var(--text-primary);">{{product.productName}}</span>
      </div>

      <div class="pdp-layout">
        <!-- â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226; IMAGE GALLERY â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226; -->
        <div class="pdp-gallery">
          <!-- Thumbnail Strip -->
          <div class="pdp-thumbs" *ngIf="product.mediaGallery && product.mediaGallery?.length > 1">
            <div
              *ngFor="let media of product.mediaGallery"
              class="pdp-thumb"
              [class.active]="selectedImage === img"
              (click)="selectedImage = img"
            >
              <img [src]="media.mediaUrl" [alt]="product.productName" />
            </div>
          </div>

          <!-- Main Image -->
          <div class="pdp-main-img-wrap">
            <div class="pdp-badge-wrap">
              <span *ngIf="selectedVariant?.discountPercentage > 0" class="pdp-discount-badge">
                &#8722;{{selectedVariant.discountPercentage}}% OFF
              </span>
              <span *ngIf="product.flashSaleActive" class="pdp-flash-badge">âš¡ FLASH SALE</span>
            </div>
            <img [src]="selectedImage" [alt]="product.productName" class="pdp-main-img" />
            <button
              class="pdp-wishlist-float"
              (click)="addToWishlist()"
              [class.active]="isWishlisted"
              [title]="isWishlisted ? 'Remove from wishlist' : 'Add to wishlist'"
            >
              {{ isWishlisted ? 'â¤ï¸' : 'ðŸ¤' }}
            </button>
          </div>
        </div>

        <!-- â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226; PRODUCT INFO â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226; -->
        <div class="pdp-info">
          <!-- Category -->
          <div class="pdp-category" *ngIf="product.categoryName">
            {{product.categoryName}}
          </div>

          <!-- Title -->
          <h1 class="pdp-title">{{product.productName}}</h1>

          <!-- Rating Row -->
          <div class="pdp-meta-row">
            <div class="pdp-stars">
              <span *ngFor="let s of getStarArray(product.averageRating || 0)" class="star" [class.filled]="s">&#9733;</span>
              <span class="pdp-rating-text">
                {{(product.averageRating || 0) | number:'1.1-1'}}
                <span style="color:var(--text-dim);">({{product.totalReviews || 0}} reviews)</span>
              </span>
            </div>
            <div class="pdp-stock-badge" [class.in-stock]="isInStock()" [class.low-stock]="isLowStock()" [class.out-stock]="isOutOfStock()">
              <span *ngIf="isInStock() && !isLowStock()">&#10003; In Stock</span>
              <span *ngIf="isLowStock()">âš ï¸ Only {{getAvailableStock()}} left</span>
              <span *ngIf="isOutOfStock()">âœ&#8226; Out of Stock</span>
            </div>
          </div>

          <hr class="pdp-divider" />

          <!-- Price -->
          <div class="pdp-price-block">
            <div class="pdp-price-main">
              &#8377;{{ getPriceDisplay() | number:'1.0-0' }}
            </div>
            <div *ngIf="getOriginalPrice() > getPriceDisplay()" class="pdp-price-sub">
              <span class="pdp-mrp">M.R.P.: &#8377;{{getOriginalPrice() | number:'1.0-0'}}</span>
              <span class="pdp-save">Save &#8377;{{(getOriginalPrice() - getPriceDisplay()) | number:'1.0-0'}}
                ({{getDiscountPct()}}% off)</span>
            </div>
            <div class="pdp-tax-note">Inclusive of all taxes</div>
          </div>

          <hr class="pdp-divider" />

          <!-- Variant Selector -->
          <div *ngIf="variants.length > 0" class="pdp-variants">

            <!-- Color -->
            <div *ngIf="colors.length > 0" class="pdp-variant-group">
              <div class="pdp-variant-label">
                Color:
                <strong *ngIf="selectedVariant">{{selectedVariant.color}}</strong>
              </div>
              <div class="pdp-variant-pills">
                <button
                  *ngFor="let c of colors"
                  class="pdp-color-pill"
                  [class.active]="selectedVariant?.color === c"
                  [class.disabled]="isColorOOS(c)"
                  (click)="selectByColor(c)"
                  [title]="c"
                >
                  <span class="color-dot" [style.background]="getColorHex(c)"></span>
                  {{c}}
                </button>
              </div>
            </div>

            <!-- Size -->
            <div *ngIf="sizes.length > 0" class="pdp-variant-group">
              <div class="pdp-variant-label">
                Size:
                <strong *ngIf="selectedVariant">{{selectedVariant.size}}</strong>
                <a style="margin-left:auto; font-size:12px; color:var(--primary); cursor:pointer;">Size Guide</a>
              </div>
              <div class="pdp-variant-pills">
                <button
                  *ngFor="let s of sizes"
                  class="pdp-size-pill"
                  [class.active]="selectedVariant?.size === s"
                  [class.disabled]="isSizeOOS(s)"
                  (click)="selectBySize(s)"
                >
                  {{s}}
                </button>
              </div>
            </div>

            <!-- Variant Cards (for items like storage, RAM etc.) -->
            <div *ngIf="colors.length === 0 && sizes.length === 0" class="pdp-variant-group">
              <div class="pdp-variant-label">Select Option:</div>
              <div class="pdp-variant-pills">
                <button
                  *ngFor="let v of variants"
                  class="pdp-option-pill"
                  [class.active]="selectedVariant?.variantPublicId === v.variantPublicId"
                  [class.disabled]="getVariantStock(v) <= 0"
                  (click)="selectVariant(v)"
                >
                  <span style="font-weight:700;">SKU: {{v.sku}}</span>
                  <span style="font-size:11px; opacity:0.7;">&#8377;{{v.salePrice || v.price | number:'1.0-0'}}</span>
                </button>
              </div>
            </div>

          </div>

          <!-- No variants yet -->
          <div *ngIf="!loadingVariants && variants.length === 0" class="pdp-no-variants">
            â„¹ï¸ No variants available for this product yet.
          </div>

          <!-- Quantity -->
          <div class="pdp-qty-row" *ngIf="selectedVariant && isInStock()">
            <div class="pdp-variant-label">Quantity:</div>
            <div class="pdp-qty-control">
              <button class="pdp-qty-btn" (click)="quantity > 1 ? quantity = quantity - 1 : null">&#8722;</button>
              <span class="pdp-qty-val">{{quantity}}</span>
              <button class="pdp-qty-btn" (click)="quantity < getAvailableStock() ? quantity = quantity + 1 : null">+</button>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="pdp-actions">
            <div *ngIf="!selectedVariant && variants.length > 0" class="pdp-select-hint">
              ðŸ‘† Please select a variant above before adding to cart
            </div>

            <button
              class="btn btn-primary pdp-btn-cart"
              (click)="addToCart()"
              [disabled]="addingToCart || !selectedVariant || isOutOfStock()"
            >
              <span *ngIf="!addingToCart">&#128722; Add to Cart</span>
              <span *ngIf="addingToCart" class="pdp-btn-loading">
                <span class="pdp-spinner"></span> Adding...
              </span>
            </button>

            <button
              class="btn pdp-btn-wishlist"
              (click)="addToWishlist()"
              [disabled]="addingToWishlist"
            >
              {{ isWishlisted ? 'â¤ï¸ Wishlisted' : 'ðŸ¤ Wishlist' }}
            </button>
          </div>

          <!-- Delivery Info -->
          <div class="pdp-delivery-block">
            <div class="pdp-delivery-row">
              <span class="pdp-delivery-icon">&#128666;</span>
              <div>
                <div class="pdp-delivery-title">Free Delivery</div>
                <div class="pdp-delivery-sub">On orders above &#8377;599</div>
              </div>
            </div>
            <div class="pdp-delivery-row">
              <span class="pdp-delivery-icon">â†©ï¸</span>
              <div>
                <div class="pdp-delivery-title">Easy Returns</div>
                <div class="pdp-delivery-sub">7-day return policy</div>
              </div>
            </div>
            <div class="pdp-delivery-row">
              <span class="pdp-delivery-icon">&#128274;</span>
              <div>
                <div class="pdp-delivery-title">Secure Payment</div>
                <div class="pdp-delivery-sub">Razorpay encrypted checkout</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Description -->
      <div class="pdp-section" style="margin-top:40px;">
        <h2 class="pdp-section-title">ðŸ“‹ Product Description</h2>
        <div class="pdp-description-body">
          <p style="white-space:pre-line; color:var(--text-muted); line-height:1.8;">{{product.description}}</p>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .pdp-page { max-width: 1200px; margin: 0 auto; padding: 100px 24px 60px; }

    /* Breadcrumb */
    .pdp-breadcrumb { display: flex; align-items: center; gap: 8px; font-size: 13px; margin-bottom: 24px; }

    /* Loading */
    .pdp-loading { max-width: 1200px; margin: 0 auto; padding: 120px 24px 60px; }
    .pdp-skeleton-wrap { display: flex; gap: 48px; }
    .pdp-sk-img { width: 480px; height: 480px; border-radius: 20px; flex-shrink: 0; }

    /* Layout */
    .pdp-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 48px; align-items: start; }
    @media (max-width: 900px) { .pdp-layout { grid-template-columns: 1fr; } .pdp-sk-img { width: 100%; } }

    /* Gallery */
    .pdp-gallery { display: flex; gap: 16px; }
    .pdp-thumbs { display: flex; flex-direction: column; gap: 10px; }
    .pdp-thumb {
      width: 72px; height: 72px; border-radius: 10px; overflow: hidden; cursor: pointer;
      border: 2px solid var(--border-default); transition: var(--transition);
      background: var(--bg-card);
    }
    .pdp-thumb img { width: 100%; height: 100%; object-fit: contain; padding: 4px; }
    .pdp-thumb.active { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(96,165,250,0.2); }

    .pdp-main-img-wrap {
      flex: 1; background: var(--bg-card); border-radius: 20px; overflow: hidden;
      border: 1px solid var(--border-default); position: relative;
      display: flex; align-items: center; justify-content: center;
      min-height: 420px; padding: 24px;
      transition: box-shadow 0.3s;
    }
    .pdp-main-img-wrap:hover { box-shadow: 0 8px 40px rgba(96,165,250,0.12); }
    .pdp-main-img { max-width: 100%; max-height: 380px; object-fit: contain; }

    .pdp-badge-wrap { position: absolute; top: 12px; left: 12px; display: flex; flex-direction: column; gap: 6px; }
    .pdp-discount-badge {
      background: linear-gradient(135deg, #10b981, #059669); color: #fff;
      font-size: 12px; font-weight: 800; padding: 4px 10px; border-radius: 20px;
    }
    .pdp-flash-badge {
      background: linear-gradient(135deg, #f59e0b, #ef4444); color: #fff;
      font-size: 12px; font-weight: 800; padding: 4px 10px; border-radius: 20px;
    }
    .pdp-wishlist-float {
      position: absolute; top: 12px; right: 12px; width: 42px; height: 42px;
      border-radius: 50%; background: var(--bg-glass); border: 1px solid var(--border-default);
      cursor: pointer; font-size: 18px; display: flex; align-items: center; justify-content: center;
      transition: var(--transition); backdrop-filter: blur(8px);
    }
    .pdp-wishlist-float:hover { transform: scale(1.1); background: rgba(239,68,68,0.1); border-color: rgba(239,68,68,0.3); }

    /* Info panel */
    .pdp-category { font-size: 12px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: var(--primary); margin-bottom: 10px; }
    .pdp-title { font-family: var(--font-head); font-size: 1.7rem; font-weight: 800; color: var(--text-primary); line-height: 1.25; margin-bottom: 16px; letter-spacing: -0.02em; }

    .pdp-meta-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; flex-wrap: wrap; gap: 10px; }
    .pdp-stars { display: flex; align-items: center; gap: 3px; }
    .star { font-size: 18px; color: var(--border-default); }
    .star.filled { color: #f59e0b; }
    .pdp-rating-text { font-size: 14px; color: var(--text-muted); margin-left: 6px; }

    .pdp-stock-badge { font-size: 13px; font-weight: 700; padding: 4px 12px; border-radius: 20px; }
    .pdp-stock-badge.in-stock { background: rgba(16,185,129,0.1); color: #10b981; border: 1px solid rgba(16,185,129,0.3); }
    .pdp-stock-badge.low-stock { background: rgba(245,158,11,0.1); color: #f59e0b; border: 1px solid rgba(245,158,11,0.3); }
    .pdp-stock-badge.out-stock { background: rgba(239,68,68,0.1); color: #ef4444; border: 1px solid rgba(239,68,68,0.3); }

    .pdp-divider { border: none; border-top: 1px solid var(--border-subtle); margin: 20px 0; }

    /* Price */
    .pdp-price-block { margin-bottom: 4px; }
    .pdp-price-main { font-family: var(--font-head); font-size: 2.2rem; font-weight: 900; color: var(--primary); letter-spacing: -0.03em; }
    .pdp-price-sub { display: flex; align-items: center; gap: 12px; margin-top: 6px; flex-wrap: wrap; }
    .pdp-mrp { font-size: 14px; color: var(--text-dim); text-decoration: line-through; }
    .pdp-save { font-size: 14px; font-weight: 700; color: #10b981; background: rgba(16,185,129,0.1); padding: 2px 10px; border-radius: 20px; }
    .pdp-tax-note { font-size: 12px; color: var(--text-dim); margin-top: 6px; }

    /* Variants */
    .pdp-variants { display: flex; flex-direction: column; gap: 18px; margin-bottom: 20px; }
    .pdp-variant-group { display: flex; flex-direction: column; gap: 10px; }
    .pdp-variant-label { font-size: 13px; color: var(--text-muted); display: flex; align-items: center; gap: 6px; }
    .pdp-variant-label strong { color: var(--text-primary); }
    .pdp-variant-pills { display: flex; flex-wrap: wrap; gap: 8px; }

    .pdp-color-pill, .pdp-size-pill, .pdp-option-pill {
      display: flex; align-items: center; gap: 6px; padding: 7px 14px; border-radius: 10px;
      border: 1.5px solid var(--border-default); background: var(--bg-card);
      cursor: pointer; font-size: 13px; font-weight: 600; color: var(--text-muted);
      transition: all 0.2s; white-space: nowrap;
    }
    .pdp-option-pill { flex-direction: column; align-items: flex-start; padding: 10px 14px; }
    .pdp-color-pill:hover, .pdp-size-pill:hover, .pdp-option-pill:hover {
      border-color: var(--primary); color: var(--primary);
    }
    .pdp-color-pill.active, .pdp-size-pill.active, .pdp-option-pill.active {
      border-color: var(--primary); color: var(--primary);
      background: rgba(96,165,250,0.08); box-shadow: 0 0 0 3px rgba(96,165,250,0.15);
    }
    .pdp-color-pill.disabled, .pdp-size-pill.disabled {
      opacity: 0.35; cursor: not-allowed; text-decoration: line-through;
    }
    .color-dot { width: 14px; height: 14px; border-radius: 50%; border: 1px solid rgba(255,255,255,0.3); flex-shrink: 0; }
    .pdp-no-variants { font-size: 13px; color: var(--text-dim); padding: 12px; background: var(--bg-card); border-radius: 10px; border: 1px solid var(--border-subtle); }

    /* Qty */
    .pdp-qty-row { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
    .pdp-qty-control { display: flex; align-items: center; border: 1.5px solid var(--border-default); border-radius: 10px; overflow: hidden; }
    .pdp-qty-btn { width: 40px; height: 40px; background: var(--bg-glass); border: none; color: var(--text-primary); font-size: 18px; cursor: pointer; transition: var(--transition); }
    .pdp-qty-btn:hover { background: var(--primary); color: #fff; }
    .pdp-qty-val { width: 48px; text-align: center; font-weight: 700; font-size: 16px; color: var(--text-primary); }

    /* Actions */
    .pdp-actions { display: flex; flex-direction: column; gap: 12px; margin-bottom: 28px; }
    .pdp-select-hint { font-size: 13px; color: var(--warning); background: rgba(245,158,11,0.08); border: 1px solid rgba(245,158,11,0.2); padding: 10px 14px; border-radius: 10px; }
    .pdp-btn-cart { height: 52px; font-size: 16px; font-weight: 700; border-radius: 14px; display: flex; align-items: center; justify-content: center; gap: 8px; }
    .pdp-btn-wishlist { height: 48px; font-size: 14px; font-weight: 600; border-radius: 14px; background: var(--bg-card); border: 1.5px solid var(--border-default); color: var(--text-muted); transition: var(--transition); }
    .pdp-btn-wishlist:hover, .pdp-btn-wishlist.active { border-color: rgba(239,68,68,0.4); color: #ef4444; background: rgba(239,68,68,0.06); }
    .pdp-btn-loading { display: flex; align-items: center; gap: 10px; }
    .pdp-spinner { width: 18px; height: 18px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; }

    /* Delivery */
    .pdp-delivery-block { background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: 14px; padding: 16px; display: flex; flex-direction: column; gap: 14px; }
    .pdp-delivery-row { display: flex; align-items: flex-start; gap: 14px; }
    .pdp-delivery-icon { font-size: 20px; flex-shrink: 0; margin-top: 2px; }
    .pdp-delivery-title { font-size: 13px; font-weight: 700; color: var(--text-primary); margin-bottom: 2px; }
    .pdp-delivery-sub { font-size: 12px; color: var(--text-dim); }

    /* Description */
    .pdp-section { background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: 16px; padding: 28px; }
    .pdp-section-title { font-size: 1.1rem; font-weight: 800; color: var(--text-primary); margin-bottom: 16px; }
    .pdp-description-body { max-width: 800px; }

    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class ProductDetailComponent implements OnInit {
  product: any = null;
  variants: any[] = [];
  selectedVariant: any = null;
  selectedImage = '';
  loading = true;
  loadingVariants = false;
  addingToCart = false;
  addingToWishlist = false;
  isWishlisted = false;
  quantity = 1;

  get colors(): string[] {
    const all = this.variants.map(v => v.color).filter(Boolean);
    return [...new Set(all)] as string[];
  }

  get sizes(): string[] {
    const all = this.variants.map(v => v.size).filter(Boolean);
    return [...new Set(all)] as string[];
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private productService: ProductService,
    private cartService: CartService,
    private wishlistService: WishlistService,
    private auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.loading = false; return; }

    this.productService.getBySlug(id).subscribe({
      next: (res: any) => {
        this.product = res;
        this.selectedImage = res.mediaGallery?.[0]?.mediaUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop';
        this.loading = false;
        this.loadVariants(id);
      },
      error: () => {
        this.loading = false;
        this.toast.error('Product not found.');
      }
    });
  }

  loadVariants(productId: string) {
    this.loadingVariants = true;
    this.productService.getVariants(productId).subscribe({
      next: (res: any) => {
        this.variants = Array.isArray(res) ? res : (res.content ?? []);
        // Auto-select first in-stock variant
        const firstAvailable = this.variants.find(v => this.getVariantStock(v) > 0);
        if (firstAvailable) this.selectVariant(firstAvailable);
        else if (this.variants.length > 0) this.selectVariant(this.variants[0]);
        this.loadingVariants = false;
      },
      error: () => { this.loadingVariants = false; }
    });
  }

  selectVariant(v: any) {
    this.selectedVariant = v;
    this.quantity = 1;
    // If variant has its own image, show it
    if (v.imageUrl) this.selectedImage = v.imageUrl;
    else if (this.product?.mediaGallery?.[0]?.mediaUrl) this.selectedImage = this.product.mediaGallery[0]?.mediaUrl;
  }

  selectByColor(color: string) {
    const match = this.variants.find(v => v.color === color &&
      (!this.selectedVariant?.size || v.size === this.selectedVariant.size));
    if (match) this.selectVariant(match);
    else {
      const any = this.variants.find(v => v.color === color);
      if (any) this.selectVariant(any);
    }
  }

  selectBySize(size: string) {
    const match = this.variants.find(v => v.size === size &&
      (!this.selectedVariant?.color || v.color === this.selectedVariant.color));
    if (match) this.selectVariant(match);
    else {
      const any = this.variants.find(v => v.size === size);
      if (any) this.selectVariant(any);
    }
  }

  getVariantStock(v: any): number {
    return (v.stockQuantity ?? 0) - (v.reservedQuantity ?? 0);
  }

  isColorOOS(color: string): boolean {
    return this.variants.filter(v => v.color === color).every(v => this.getVariantStock(v) <= 0);
  }

  isSizeOOS(size: string): boolean {
    return this.variants.filter(v => v.size === size).every(v => this.getVariantStock(v) <= 0);
  }

  isInStock(): boolean {
    if (!this.selectedVariant) return false;
    return this.getVariantStock(this.selectedVariant) > 0;
  }

  isLowStock(): boolean {
    if (!this.selectedVariant) return false;
    const stock = this.getVariantStock(this.selectedVariant);
    return stock > 0 && stock <= 5;
  }

  isOutOfStock(): boolean {
    if (!this.selectedVariant) return false;
    return this.getVariantStock(this.selectedVariant) <= 0;
  }

  getAvailableStock(): number {
    if (!this.selectedVariant) return 0;
    return this.getVariantStock(this.selectedVariant);
  }

  getPriceDisplay(): number {
    if (this.selectedVariant) {
      return this.selectedVariant.salePrice || this.selectedVariant.price || 0;
    }
    return this.product?.discountPrice || this.product?.price || 0;
  }

  getOriginalPrice(): number {
    if (this.selectedVariant) return this.selectedVariant.price || 0;
    return this.product?.price || 0;
  }

  getDiscountPct(): number {
    const orig = this.getOriginalPrice();
    const curr = this.getPriceDisplay();
    if (!orig || orig <= curr) return 0;
    return Math.round(((orig - curr) / orig) * 100);
  }

  getStarArray(rating: number): boolean[] {
    return Array.from({ length: 5 }, (_, i) => i < Math.round(rating));
  }

  getColorHex(color: string): string {
    const map: Record<string, string> = {
      red: '#ef4444', blue: '#3b82f6', green: '#10b981', black: '#1f2937',
      white: '#f9fafb', yellow: '#f59e0b', orange: '#f97316', pink: '#ec4899',
      purple: '#8b5cf6', gray: '#6b7280', grey: '#6b7280', silver: '#9ca3af',
      gold: '#d97706', brown: '#92400e', navy: '#1e3a5f', cyan: '#06b6d4'
    };
    return map[color.toLowerCase()] || '#6b7280';
  }

  addToCart() {
    if (!this.auth.isLoggedIn()) { this.toast.warning('Please login first'); this.router.navigate(['/login']); return; }
    if (!this.auth.isCustomer()) { this.toast.warning('Only customers can add to cart'); return; }
    if (!this.selectedVariant) { this.toast.warning('Please select a variant first'); return; }
    if (this.isOutOfStock()) { this.toast.error('This variant is out of stock'); return; }

    // ðŸ”‘ ALWAYS send variantPublicId â€” NEVER productPublicId
    const variantPublicId = this.selectedVariant.variantPublicId || this.selectedVariant.publicId;
    if (!variantPublicId) { this.toast.error('Could not identify variant. Please try again.'); return; }

    this.addingToCart = true;
    this.cartService.add(variantPublicId, this.quantity).subscribe({
      next: () => {
        this.toast.success(`${this.product.productName} added to cart! &#128722;`);
        this.addingToCart = false;
      },
      error: (e: any) => {
        this.toast.error(e?.error?.message || 'Failed to add to cart');
        this.addingToCart = false;
      }
    });
  }

  addToWishlist() {
    if (!this.auth.isLoggedIn()) { this.toast.warning('Please login first'); this.router.navigate(['/login']); return; }
    const id = this.product?.productPublicId || this.product?.publicId;
    if (!id) return;
    this.addingToWishlist = true;
    this.wishlistService.toggle(id).subscribe({
      next: () => {
        this.isWishlisted = !this.isWishlisted;
        this.toast.success(this.isWishlisted ? 'Added to wishlist â¤ï¸' : 'Removed from wishlist');
        this.addingToWishlist = false;
      },
      error: (e: any) => {
        this.toast.error(e?.error?.message || 'Wishlist update failed');
        this.addingToWishlist = false;
      }
    });
  }
}

