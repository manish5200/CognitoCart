import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SaleService } from '../../../services/sale.service';
import { ProductService } from '../../../services/product.service';
import { ToastService } from '../../../services/toast.service';
import { AuthService } from '../../../services/auth.service';
import { WishlistService } from '../../../services/wishlist.service';

@Component({
  selector: 'app-flash-sales',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <!-- Hero -->
      <section class="flash-hero">
        <div class="hero-bg" style="background: radial-gradient(circle at center, rgba(239,68,68,0.2) 0%, transparent 60%);"></div>
        <div class="flash-hero-inner">
          <div class="live-badge"><span class="live-dot"></span> LIVE NOW</div>
          <h1 class="flash-title">⚡ <span class="gradient-text-fire">Flash Sales</span></h1>
          <p class="flash-subtitle">Grab the biggest discounts of the season before time runs out!</p>
        </div>
      </section>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div *ngIf="!loading && sales.length === 0" class="empty-state" style="min-height:40vh;">
        <div class="empty-icon">⏳</div>
        <div class="empty-title">No Active Flash Sales</div>
        <div class="empty-subtitle">Check back later for exciting new deals!</div>
        <a routerLink="/products" class="btn btn-primary">Browse All Products</a>
      </div>

      <div *ngIf="!loading && sales.length > 0">
        <!-- Sales Tabs -->
        <div class="sale-tabs container">
          <button
            *ngFor="let sale of sales"
            class="sale-tab"
            [class.active]="selectedSale?.publicId === sale.publicId"
            (click)="selectSale(sale)"
          >
            {{sale.eventName}}
          </button>
        </div>

        <!-- Selected Sale Info -->
        <div class="container" *ngIf="selectedSale" style="margin-bottom:32px;">
          <div class="sale-banner-large">
            <div style="flex:1;">
              <h2 style="font-size:1.8rem; font-weight:900; margin-bottom:8px;">{{selectedSale.eventName}}</h2>
              <p style="color:var(--text-muted); max-width:600px;">{{selectedSale.description || 'Hurry! Limited stock available for these exclusive deals.'}}</p>
            </div>
            <div class="sale-timer">
              <div class="timer-label">Ends In</div>
              <div class="timer-value">{{countdown}}</div>
            </div>
          </div>
        </div>

        <!-- Products -->
        <div class="container section">
          <div class="loading-center" *ngIf="loadingProducts"><div class="spinner"></div></div>
          
          <div *ngIf="!loadingProducts && products.length === 0" class="empty-state" style="min-height:200px;">
            <div class="empty-icon">🛍️</div>
            <div class="empty-title">No products in this sale</div>
          </div>

          <div *ngIf="!loadingProducts && products.length > 0" class="flash-product-grid">
            <div *ngFor="let p of products" class="fpc" [routerLink]="['/product', p.slug || p.productPublicId || p.publicId]">
              <div class="fpc-img-wrap">
                <img [src]="p.imageUrls?.[0] || 'https://via.placeholder.com/400'" [alt]="p.productName || p.name" class="fpc-img"/>
                <div class="fpc-badge-discount">-{{p.discountPercentage || 0}}%</div>
                <button
                  *ngIf="auth.isCustomer()"
                  class="fpc-wishlist"
                  (click)="$event.stopPropagation(); $event.preventDefault(); toggleWishlist(p)"
                >{{ p.isWishlisted ? '❤️' : '🤍' }}</button>
              </div>
              <div class="fpc-body">
                <div class="fpc-name">{{p.productName || p.name}}</div>
                <div class="fpc-price-row">
                  <div class="fpc-price-now">₹{{(p.discountPrice || p.price) | number:'1.0-0'}}</div>
                  <div class="fpc-price-was">₹{{p.price | number:'1.0-0'}}</div>
                </div>
                <!-- Fake progress bar for flash sales -->
                <div class="fpc-stock-bar">
                  <div class="fpc-stock-fill" [style.width]="(p.stock > 100 ? 100 : p.stock) + '%'"></div>
                </div>
                <div class="fpc-stock-text">Only {{p.stock}} left!</div>
                <button class="btn btn-primary btn-sm btn-full" style="margin-top:12px;">Grab Deal →</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .flash-hero { position: relative; padding: 60px 20px; text-align: center; overflow: hidden; border-bottom: 1px solid rgba(239,68,68,0.2); }
    .flash-hero-inner { position: relative; z-index: 10; max-width: 800px; margin: 0 auto; }
    .live-badge { display: inline-flex; align-items: center; gap: 8px; background: rgba(239,68,68,0.15); color: var(--danger); font-size: 12px; font-weight: 800; padding: 6px 12px; border-radius: 20px; margin-bottom: 16px; border: 1px solid rgba(239,68,68,0.3); letter-spacing: 0.1em; }
    .live-dot { width: 8px; height: 8px; background: var(--danger); border-radius: 50%; animation: pulse 1.5s infinite; }
    .flash-title { font-size: 3rem; font-family: var(--font-head); font-weight: 900; margin-bottom: 16px; letter-spacing: -0.02em; }
    .flash-subtitle { font-size: 1.2rem; color: var(--text-muted); }
    
    .sale-tabs { display: flex; gap: 12px; overflow-x: auto; padding: 24px 0; border-bottom: 1px solid var(--border-subtle); margin-bottom: 32px; scrollbar-width: none; }
    .sale-tabs::-webkit-scrollbar { display: none; }
    .sale-tab { background: transparent; border: 2px solid var(--border-subtle); color: var(--text-secondary); padding: 10px 24px; border-radius: 30px; font-weight: 700; font-size: 14px; cursor: pointer; transition: 0.2s; white-space: nowrap; }
    .sale-tab:hover { border-color: var(--border); color: #fff; }
    .sale-tab.active { background: var(--danger); border-color: var(--danger); color: #fff; box-shadow: 0 4px 14px rgba(239,68,68,0.3); }
    
    .sale-banner-large { display: flex; justify-content: space-between; align-items: center; background: linear-gradient(135deg, rgba(239,68,68,0.1) 0%, rgba(245,158,11,0.05) 100%); border: 1px solid rgba(239,68,68,0.2); border-radius: var(--r-xl); padding: 32px; gap: 24px; flex-wrap: wrap; }
    .sale-timer { background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.1); border-radius: var(--r-lg); padding: 16px 24px; text-align: center; min-width: 200px; backdrop-filter: blur(4px); }
    .timer-label { font-size: 11px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.1em; margin-bottom: 4px; }
    .timer-value { font-family: var(--font-mono); font-size: 24px; font-weight: 700; color: var(--danger); letter-spacing: 2px; }
    
    .flash-product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 24px; }
    .fpc { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--r-lg); overflow: hidden; transition: 0.3s; cursor: pointer; position: relative; }
    .fpc:hover { transform: translateY(-4px); border-color: var(--danger); box-shadow: 0 12px 24px rgba(0,0,0,0.2), 0 0 0 1px rgba(239,68,68,0.2); }
    .fpc-img-wrap { position: relative; aspect-ratio: 1; background: var(--bg-glass); overflow: hidden; }
    .fpc-img { width: 100%; height: 100%; object-fit: cover; transition: 0.5s; }
    .fpc:hover .fpc-img { transform: scale(1.05); }
    .fpc-badge-discount { position: absolute; top: 12px; left: 12px; background: var(--danger); color: #fff; font-weight: 800; font-size: 13px; padding: 4px 10px; border-radius: 4px; box-shadow: 0 4px 12px rgba(239,68,68,0.4); }
    .fpc-wishlist { position: absolute; top: 12px; right: 12px; width: 32px; height: 32px; border-radius: 50%; background: rgba(0,0,0,0.5); border: none; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(4px); cursor: pointer; transition: 0.2s; opacity: 0; }
    .fpc:hover .fpc-wishlist { opacity: 1; }
    .fpc-wishlist:hover { background: rgba(239,68,68,0.2); transform: scale(1.1); }
    .fpc-body { padding: 16px; }
    .fpc-name { font-weight: 600; font-size: 14px; margin-bottom: 8px; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    .fpc-price-row { display: flex; align-items: baseline; gap: 8px; margin-bottom: 12px; }
    .fpc-price-now { font-size: 1.2rem; font-weight: 800; color: var(--danger); }
    .fpc-price-was { font-size: 13px; color: var(--text-dim); text-decoration: line-through; }
    .fpc-stock-bar { height: 6px; background: rgba(255,255,255,0.1); border-radius: 3px; overflow: hidden; margin-bottom: 6px; }
    .fpc-stock-fill { height: 100%; background: linear-gradient(90deg, var(--danger), var(--amber-500)); border-radius: 3px; transition: width 1s ease; }
    .fpc-stock-text { font-size: 11px; color: var(--text-muted); font-weight: 500; }
    
    @keyframes pulse { 0% { box-shadow: 0 0 0 0 rgba(239,68,68,0.7); } 70% { box-shadow: 0 0 0 6px rgba(239,68,68,0); } 100% { box-shadow: 0 0 0 0 rgba(239,68,68,0); } }
    @media (max-width: 600px) { .flash-title { font-size: 2rem; } .sale-banner-large { flex-direction: column; text-align: center; } .sale-timer { width: 100%; } }
  `]
})
export class FlashSalesComponent implements OnInit {
  loading = true;
  sales: any[] = [];
  selectedSale: any = null;
  products: any[] = [];
  loadingProducts = false;
  countdown = '00:00:00';
  private timer: any;

  constructor(
    private saleService: SaleService,
    private productService: ProductService,
    private wishlistService: WishlistService,
    public auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.saleService.getLiveSales().subscribe({
      next: (res: any) => {
        this.sales = Array.isArray(res) ? res : (res.content ?? []);
        this.loading = false;
        if (this.sales.length > 0) {
          this.selectSale(this.sales[0]);
        }
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  selectSale(sale: any): void {
    this.selectedSale = sale;
    this.loadProducts(sale.publicId);
    this.startCountdown(sale.endTime);
  }

  loadProducts(saleId: string): void {
    this.loadingProducts = true;
    this.saleService.getLiveSales().subscribe({
      next: (res: any) => {
        // Since we don't have a direct endpoint for "products by sale id",
        // we'll just fetch all products and filter/highlight them.
        // In a real app, this would be a specific API call.
        this.productService.getAll(0, 50).subscribe({
            next: (pRes: any) => {
                let allProds = pRes.content ?? (Array.isArray(pRes) ? pRes : []);
                // Just for demo, we'll show all products that have flashSaleActive or discountPercentage > 0
                this.products = allProds.filter((p: any) => p.flashSaleActive || (p.discountPercentage && p.discountPercentage > 15));
                this.loadingProducts = false;
            },
            error: () => this.loadingProducts = false
        });
      },
      error: () => this.loadingProducts = false
    });
  }

  startCountdown(endTimeStr: string): void {
    if (this.timer) clearInterval(this.timer);
    
    const end = new Date(endTimeStr).getTime();
    
    this.timer = setInterval(() => {
      const now = new Date().getTime();
      const distance = end - now;
      
      if (distance < 0) {
        clearInterval(this.timer);
        this.countdown = 'EXPIRED';
        return;
      }
      
      const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((distance % (1000 * 60)) / 1000);
      
      this.countdown = 
        String(hours).padStart(2, '0') + ':' + 
        String(minutes).padStart(2, '0') + ':' + 
        String(seconds).padStart(2, '0');
    }, 1000);
  }

  toggleWishlist(product: any): void {
    if (!this.auth.isLoggedIn()) return;
    const id = product.productPublicId || product.publicId;
    this.wishlistService.toggle(id).subscribe({
      next: () => {
        product.isWishlisted = !product.isWishlisted;
        this.toast.success(product.isWishlisted ? 'Added to wishlist ❤️' : 'Removed from wishlist');
      },
      error: () => this.toast.error('Failed to update wishlist')
    });
  }
}
