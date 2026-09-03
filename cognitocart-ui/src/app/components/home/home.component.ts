import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ProductService } from '../../services/product.service';
import { SaleService } from '../../services/sale.service';
import { WishlistService } from '../../services/wishlist.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { ProductCardComponent } from '../shared/product-card/product-card.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ProductCardComponent],
  template: `
    <!-- ═══════════════════════════════════ HERO ═══════════════════════════════════ -->
    <section class="hero-premium">
      <div class="hero-premium-bg"></div>
      
      <div class="hero-premium-inner">
        <div class="hero-badge fade-up">
          <span class="sparkle">✨</span> AI-Powered Product Discovery
        </div>
        
        <h1 class="hero-title fade-up fade-up-delay-1">
          Find what you need.<br/>
          <span class="text-glow">Discover what you didn't know you needed.</span>
        </h1>
        
        <p class="hero-subtitle fade-up fade-up-delay-2">
          Experience the next generation of commerce. Search naturally, explore curated collections, and let intelligence guide your journey.
        </p>

        <!-- Premium AI Search Bar -->
        <div class="hero-search-container fade-up fade-up-delay-3">
          <div class="search-box">
            <svg class="search-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
            <input
              class="search-input"
              type="text"
              [(ngModel)]="searchQuery"
              (keyup.enter)="search()"
              placeholder='Try "Best phone under \u20B930,000" or "laptop for programming"'
            />
            <button class="search-submit" (click)="search()" [disabled]="!searchQuery.trim()">
              Search
            </button>
          </div>
          
          <div class="hero-quick-actions">
            <span style="color: var(--text-dim); font-size: 13px; font-weight: 500;">Popular right now:</span>
            <div class="suggestion-chips">
              <span class="chip" *ngFor="let tag of trendingTags" (click)="searchTag(tag)">{{tag}}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════ LIVE FLASH SALES ═══════════════════ -->
    <section class="container section" *ngIf="liveSales.length > 0" style="padding-top:32px;">
      <div class="section-header" style="margin-bottom: 24px;">
        <h2 class="section-title" style="display:flex; align-items:center; gap:12px;">
          <span class="live-pulse"></span>
          <span class="gradient-text-fire" style="font-weight: 800;">Live Sales</span>
        </h2>
        <a routerLink="/flash-sales" class="btn btn-ghost btn-sm">Explore All →</a>
      </div>
      <div class="grid-2" style="gap: 16px;">
        <div *ngFor="let sale of liveSales; let i=index" class="sale-card fade-up" [class]="'fade-up-delay-'+((i%3)+1)">
          <div class="sale-card-content">
            <h3 class="sale-title">{{sale.eventName}}</h3>
            <p class="sale-desc">{{sale.description || 'Exclusive discounts for a limited time.'}}</p>
            <div class="sale-timer">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg>
              Ends: {{sale.endTime | date:'medium'}}
            </div>
          </div>
          <a routerLink="/flash-sales" class="btn btn-danger btn-sm" style="align-self: flex-start; margin-top: auto;">Shop Now</a>
        </div>
      </div>
    </section>

    <!-- ═══════════════════ UPCOMING SALES ═══════════════════ -->
    <section class="container" *ngIf="upcomingSales.length > 0" style="margin-top:16px;">
      <div class="upcoming-scroll">
        <div *ngFor="let sale of upcomingSales" class="upcoming-sale-card">
          <div class="usc-info">
            <span class="usc-label">Coming Soon</span>
            <span class="usc-title">{{sale.eventName}}</span>
          </div>
          <div class="usc-time">
            Starts {{sale.startTime | date:'mediumDate'}}
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════ CATEGORIES ═══════════════════ -->
    <section class="container section" *ngIf="categories.length > 0" style="padding:48px 0 24px;">
      <div class="section-header" style="margin-bottom: 24px;">
        <h2 class="section-title">Explore by <span>Category</span></h2>
      </div>
      <div class="category-grid">
        <div
          *ngFor="let cat of categories; let i=index"
          class="category-card fade-up"
          [class]="'fade-up-delay-'+((i%3)+1)"
          (click)="browseCategory(cat)"
        >
          <div class="cat-icon-wrap">
            {{getCatIcon(cat.name)}}
          </div>
          <div class="cat-name">{{cat.name}}</div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════ FEATURED PRODUCTS ═══════════════════ -->
    <section class="container section">
      <div class="section-header">
        <h2 class="section-title">⭐ Featured <span>Products</span></h2>
        <a routerLink="/products" class="btn btn-ghost btn-sm">View All →</a>
      </div>

      <!-- Skeleton Loading -->
      <div *ngIf="loadingProducts" style="display:grid; grid-template-columns:repeat(auto-fill,minmax(240px,1fr)); gap:16px;">
        <div *ngFor="let s of [1,2,3,4]" class="skeleton" style="height:360px; border-radius:var(--r-lg);"></div>
      </div>

      <!-- Empty State -->
      <div *ngIf="!loadingProducts && products.length === 0" class="empty-state">
        <div class="empty-icon">🛍️</div>
        <div class="empty-title">No products yet</div>
        <div class="empty-subtitle">Check back soon — sellers are stocking up!</div>
      </div>

      <!-- Product Grid -->
      <div *ngIf="!loadingProducts && products.length > 0" class="home-product-grid">
        <app-product-card
          *ngFor="let p of products; let i=index"
          [product]="p"
          [showWishlist]="auth.isCustomer()"
          (toggleWishlist)="toggleWishlist($event)"
          class="fade-up"
          [class]="'fade-up-delay-'+((i%3)+1)"
        ></app-product-card>
      </div>
    </section>

    <!-- ═══════════════════ VALUE PROPS ═══════════════════ -->
    <section class="container section" style="padding-top:24px;">
      <h2 class="section-title" style="text-align:center; margin-bottom:12px;">
        Designed for <span class="text-glow">Intelligence</span>
      </h2>
      <p style="text-align:center; color:var(--text-muted); font-size:14px; margin-bottom:48px; max-width:540px; margin-left:auto; margin-right:auto;">
        A premium ecosystem that adapts to your preferences, providing a seamless and secure shopping experience.
      </p>
      <div class="grid-4">
        <div *ngFor="let feat of features; let i=index" class="card fade-up" [class]="'fade-up-delay-'+((i%3)+1)" style="background: var(--bg-glass); border: 1px solid rgba(255,255,255,0.03);">
          <div class="card-body" style="padding:32px 24px;">
            <div style="width: 48px; height: 48px; border-radius: 12px; background: rgba(255,255,255,0.05); display: flex; align-items: center; justify-content: center; font-size: 24px; margin-bottom: 20px;">
              {{feat.icon}}
            </div>
            <h3 style="font-size:15px; font-weight:700; color:var(--text-primary); margin-bottom:8px;">{{feat.title}}</h3>
            <p style="font-size:13px; color:var(--text-muted); line-height:1.6;">{{feat.desc}}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════════ SELLER CTA ═══════════════════ -->
    <section class="container" style="margin:48px auto 80px;">
      <div class="card" style="background:var(--bg-card); border: 1px solid rgba(255,255,255,0.05); padding:0; overflow:hidden;">
        <div class="card-body" style="padding:48px; display:flex; align-items:center; justify-content: space-between; gap:32px; flex-wrap:wrap;">
          <div style="flex:1; min-width:300px;">
            <div style="font-size:11px; font-weight:700; letter-spacing:0.15em; text-transform:uppercase; color:var(--text-dim); margin-bottom:12px;">For Merchants</div>
            <h2 style="font-family:var(--font-head); font-size:1.8rem; font-weight:700; color:var(--text-primary); margin-bottom:12px; line-height:1.2;">
              Elevate your business with<br/>CognitoCart Commerce
            </h2>
            <p style="font-size:14px; color:var(--text-muted); line-height:1.6; margin-bottom:0; max-width: 500px;">
              Join a curated ecosystem of premium sellers. Access advanced analytics, AI-driven insights, and seamless fulfillment tools designed for scale.
            </p>
          </div>
          <div style="display:flex; flex-direction:column; gap:16px; flex-shrink:0;">
            <a routerLink="/register/seller" class="btn btn-primary btn-lg" style="padding: 0 32px;">Partner With Us</a>
            <a routerLink="/login" class="btn btn-ghost btn-sm" style="text-align:center; color: var(--text-muted);">Sign in to Seller Hub →</a>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    /* Premium Hero Design */
    .hero-premium {
      position: relative;
      min-height: 85vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: var(--navbar-h) 20px 80px;
      overflow: hidden;
      background: radial-gradient(circle at top, rgba(99, 102, 241, 0.05), transparent 60%),
                  radial-gradient(circle at bottom right, rgba(56, 189, 248, 0.03), transparent 50%);
    }
    .hero-premium-bg {
      position: absolute;
      inset: 0;
      background: url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MCIgaGVpZ2h0PSI0MCI+PGNpcmNsZSBjeD0iMjAiIGN5PSIyMCIgcj0iMC41IiBmaWxsPSJyZ2JhKDI1NSwyNTUsMjU1LDAuMDUpIi8+PC9zdmc+') repeat;
      opacity: 0.5;
      z-index: 0;
    }
    .hero-premium-inner {
      position: relative;
      z-index: 1;
      max-width: 800px;
      width: 100%;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .hero-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 6px 16px;
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 100px;
      font-size: 12px;
      font-weight: 600;
      color: var(--text-secondary);
      margin-bottom: 32px;
      backdrop-filter: blur(10px);
    }
    .hero-badge .sparkle {
      font-size: 14px;
      filter: drop-shadow(0 0 8px rgba(99, 102, 241, 0.5));
    }
    .hero-title {
      font-family: var(--font-head);
      font-size: 4rem;
      font-weight: 800;
      line-height: 1.1;
      letter-spacing: -0.02em;
      color: var(--text-primary);
      margin-bottom: 24px;
    }
    .text-glow {
      background: linear-gradient(135deg, #e0e7ff, #a5b4fc);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      filter: drop-shadow(0 0 32px rgba(165,180,252,0.2));
    }
    .hero-subtitle {
      font-size: 1.1rem;
      color: var(--text-muted);
      line-height: 1.6;
      max-width: 600px;
      margin-bottom: 48px;
    }
    
    /* Search Container */
    .hero-search-container {
      width: 100%;
      max-width: 680px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 24px;
    }
    .search-box {
      width: 100%;
      display: flex;
      align-items: center;
      background: rgba(15, 23, 42, 0.4);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 100px;
      padding: 8px 8px 8px 24px;
      box-shadow: 0 12px 40px rgba(0,0,0,0.1);
      backdrop-filter: blur(20px);
      transition: all 0.3s ease;
    }
    .search-box:focus-within {
      border-color: rgba(99, 102, 241, 0.5);
      background: rgba(15, 23, 42, 0.7);
      box-shadow: 0 12px 40px rgba(0,0,0,0.2), 0 0 0 4px rgba(99, 102, 241, 0.1);
    }
    .search-icon {
      width: 20px;
      height: 20px;
      color: var(--text-dim);
      flex-shrink: 0;
    }
    .search-input {
      flex: 1;
      background: transparent;
      border: none;
      outline: none;
      color: var(--text-primary);
      font-size: 15px;
      padding: 0 16px;
    }
    .search-input::placeholder {
      color: var(--text-dim);
    }
    .search-submit {
      background: var(--text-primary);
      color: var(--bg-base);
      border: none;
      border-radius: 100px;
      padding: 12px 28px;
      font-weight: 700;
      font-size: 14px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .search-submit:hover:not(:disabled) {
      transform: scale(1.02);
      box-shadow: 0 4px 12px rgba(255,255,255,0.1);
    }
    .search-submit:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .hero-quick-actions {
      display: flex;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
      justify-content: center;
    }
    .suggestion-chips {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
      justify-content: center;
    }
    .chip {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.08);
      color: var(--text-secondary);
      font-size: 12px;
      padding: 6px 14px;
      border-radius: 100px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .chip:hover {
      background: rgba(255,255,255,0.08);
      color: var(--text-primary);
      border-color: rgba(255,255,255,0.2);
    }

    @media (max-width: 768px) {
      .hero-title { font-size: 2.5rem; }
      .search-box { padding: 6px 6px 6px 16px; }
      .search-submit { padding: 10px 20px; }
      .hero-premium { padding-top: calc(var(--navbar-h) + 20px); min-height: 75vh; }
    }
    
    /* Live Pulse Dot */
    .live-pulse {
      display: inline-block;
      width: 12px;
      height: 12px;
      background-color: var(--danger);
      border-radius: 50%;
      box-shadow: 0 0 0 rgba(239, 68, 68, 0.4);
      animation: pulse-danger 2s infinite;
    }
    @keyframes pulse-danger {
      0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7); }
      70% { box-shadow: 0 0 0 10px rgba(239, 68, 68, 0); }
      100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
    }
    
    /* Sale Cards */
    .sale-card {
      background: linear-gradient(145deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.9) 100%);
      border: 1px solid rgba(255, 255, 255, 0.05);
      border-radius: 16px;
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
      backdrop-filter: blur(12px);
      transition: all 0.3s ease;
    }
    .sale-card:hover {
      border-color: rgba(239, 68, 68, 0.3);
      box-shadow: 0 12px 30px rgba(0, 0, 0, 0.2), inset 0 0 0 1px rgba(239, 68, 68, 0.1);
      transform: translateY(-2px);
    }
    .sale-title {
      font-family: var(--font-head);
      font-size: 1.25rem;
      font-weight: 800;
      color: var(--text-primary);
      margin: 0 0 4px 0;
    }
    .sale-desc {
      font-size: 14px;
      color: var(--text-muted);
      margin: 0 0 12px 0;
    }
    .sale-timer {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 600;
      color: var(--danger);
      background: rgba(239, 68, 68, 0.1);
      padding: 6px 12px;
      border-radius: 100px;
      width: fit-content;
    }
    .sale-timer svg { width: 14px; height: 14px; }
    
    /* Upcoming Sales */
    .upcoming-scroll {
      display: flex;
      gap: 12px;
      overflow-x: auto;
      padding-bottom: 8px;
      scrollbar-width: none; /* Firefox */
    }
    .upcoming-scroll::-webkit-scrollbar { display: none; }
    .upcoming-sale-card {
      flex: 0 0 auto;
      background: var(--bg-surface);
      border: 1px solid rgba(255, 255, 255, 0.04);
      padding: 12px 20px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      gap: 20px;
    }
    .usc-info { display: flex; flex-direction: column; }
    .usc-label { font-size: 10px; text-transform: uppercase; letter-spacing: 0.1em; color: var(--primary); font-weight: 700; margin-bottom: 2px;}
    .usc-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
    .usc-time { font-size: 12px; color: var(--text-muted); background: var(--bg-glass); padding: 4px 10px; border-radius: 6px; }

    /* Category Cards */
    .category-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
      gap: 16px;
    }
    .category-card {
      background: var(--bg-surface);
      border: 1px solid rgba(255, 255, 255, 0.03);
      border-radius: 16px;
      padding: 24px 16px;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
    }
    .category-card:hover {
      transform: translateY(-6px);
      background: rgba(30, 41, 59, 0.6);
      border-color: rgba(99, 102, 241, 0.3);
      box-shadow: 0 10px 25px rgba(0,0,0,0.2);
    }
    .cat-icon-wrap {
      width: 56px;
      height: 56px;
      background: var(--bg-glass);
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      transition: transform 0.3s ease;
    }
    .category-card:hover .cat-icon-wrap {
      transform: scale(1.1) rotate(5deg);
      background: rgba(99, 102, 241, 0.15);
    }
    .cat-name {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-primary);
      letter-spacing: 0.02em;
    }

    /* Home Product Cards */
    .home-product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
      gap: 20px;
    }
  `]
})
export class HomeComponent implements OnInit {
  products: any[] = [];
  liveSales: any[] = [];
  upcomingSales: any[] = [];
  categories: any[] = [];
  searchQuery = '';
  loadingProducts = true;

  trendingTags = [
    'Smartphones', 'Laptops', 'Gaming', 'Home Decor', 'Audio'
  ];

  heroStats = [];

  features = [
    { icon: '🧠', title: 'Contextual AI Search', desc: 'Describe what you want naturally. Our intelligence engine understands intent, context, and nuance.' },
    { icon: '✨', title: 'Curated Discovery', desc: 'Experience personalized recommendations that adapt to your unique preferences in real-time.' },
    { icon: '⚡', title: 'Seamless Experience', desc: 'From search to checkout, enjoy a frictionless, high-performance interface designed for speed.' },
    { icon: '🛡️', title: 'Secure Ecosystem', desc: 'Enterprise-grade security ensuring your transactions and data remain private and protected.' },
  ];

  constructor(
    private productService: ProductService,
    private saleService: SaleService,
    private wishlistService: WishlistService,
    private toast: ToastService,
    public auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.productService.getAll(0, 12).subscribe({
      next: (r: any) => { this.products = r.content ?? (Array.isArray(r) ? r : []); this.loadingProducts = false; },
      error: () => { this.loadingProducts = false; }
    });

    this.saleService.getLiveSales().subscribe({
      next: (s: any) => this.liveSales = Array.isArray(s) ? s : (s.content ?? []),
      error: () => {}
    });

    this.saleService.getUpcomingSales().subscribe({
      next: (s: any) => this.upcomingSales = Array.isArray(s) ? s : (s.content ?? []),
      error: () => {}
    });

    this.productService.getCategories().subscribe({
      next: (c: any) => this.categories = (Array.isArray(c) ? c : (c.content ?? [])).slice(0, 12),
      error: () => {}
    });
  }

  search(): void {
    if (!this.searchQuery.trim()) return;
    this.router.navigate(['/products'], { queryParams: { q: this.searchQuery.trim() } });
  }

  searchTag(tag: string): void {
    const q = tag.replace(/[^\w\s]/g, '').trim();
    this.router.navigate(['/products'], { queryParams: { q } });
  }

  browseCategory(cat: any): void {
    this.router.navigate(['/products'], { queryParams: { categoryId: cat.publicId || cat.name } });
  }

  toggleWishlist(product: any): void {
    if (!this.auth.isLoggedIn()) { this.router.navigate(['/login']); return; }
    const id = product.productPublicId || product.publicId;
    if (!id) return;
    this.wishlistService.toggle(id).subscribe({
      next: () => {
        product.isWishlisted = !product.isWishlisted;
        this.toast.success(product.isWishlisted ? 'Added to wishlist ❤️' : 'Removed from wishlist');
      },
      error: (e: any) => this.toast.error(e?.error?.message || 'Wishlist update failed')
    });
  }

  getCatIcon(name: string): string {
    const n = name?.toLowerCase() || '';
    if (n.includes('electron') || n.includes('gadget')) return '💻';
    if (n.includes('phone') || n.includes('mobile')) return '📱';
    if (n.includes('laptop') || n.includes('computer')) return '🖥️';
    if (n.includes('audio') || n.includes('earphone') || n.includes('headphone')) return '🎧';
    if (n.includes('gaming') || n.includes('game')) return '🎮';
    if (n.includes('fashion') || n.includes('cloth') || n.includes('wear')) return '👗';
    if (n.includes('shoe') || n.includes('footwear')) return '👟';
    if (n.includes('watch') || n.includes('accessory')) return '⌚';
    if (n.includes('home') || n.includes('decor')) return '🏠';
    if (n.includes('kitchen') || n.includes('cook')) return '🍳';
    if (n.includes('book') || n.includes('stationery')) return '📚';
    if (n.includes('sport') || n.includes('fitness')) return '⚽';
    if (n.includes('beauty') || n.includes('skin') || n.includes('cosmetic')) return '💄';
    if (n.includes('toy') || n.includes('kid') || n.includes('baby')) return '🧸';
    if (n.includes('food') || n.includes('grocery')) return '&#128722;';
    if (n.includes('jewel') || n.includes('gold')) return '💍';
    if (n.includes('festival') || n.includes('gift')) return '🎁';
    return '📦';
  }
}
