import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Product } from '../../services/product.service';
import { CartService } from '../../services/cart.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { WishlistService } from '../../services/wishlist.service';
import { ProductCardComponent } from '../shared/product-card/product-card.component';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, RouterModule, ProductCardComponent],
  template: `
    <div class="container section">
      <main class="main-content" style="width: 100%;">
        
        <div class="search-header glass-card fade-up">
          <div class="search-icon-wrap">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 0 0-3.09 3.09ZM18.259 8.715 18 9.75l-.259-1.035a3.375 3.375 0 0 0-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 0 0 2.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 0 0 2.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 0 0-2.456 2.456ZM16.894 20.567 16.5 21.75l-.394-1.183a2.25 2.25 0 0 0-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 0 0 1.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 0 0 1.423 1.423l1.183.394-1.183.394a2.25 2.25 0 0 0-1.423 1.423Z" /></svg>
          </div>
          <div>
            <h2 class="search-title">Results for <span class="text-glow">"{{ query }}"</span></h2>
            <p class="search-subtitle">Powered by AI Semantic Search. Displaying {{ products.length }} exact and contextual matches.</p>
          </div>
        </div>

        <div *ngIf="loading" class="search-loading">
          <div class="spinner"></div>
          <p class="text-glow" style="font-weight: 600;">Processing neural query...</p>
        </div>

        <div *ngIf="!loading && products.length === 0" class="empty-state">
          <div class="empty-icon">ðŸ”</div>
          <h3 class="empty-title">No direct matches found</h3>
          <p class="empty-subtitle">We couldn't find anything matching your exact query. Try using more general terms.</p>
        </div>

        <div class="search-grid" *ngIf="!loading && products.length > 0">
          <app-product-card
            *ngFor="let product of products; let i=index"
            [product]="product"
            [showWishlist]="auth.isCustomer()"
            (toggleWishlist)="toggleWishlist($event)"
            class="fade-up"
            [class]="'fade-up-delay-'+((i%3)+1)"
          ></app-product-card>
        </div>
      </main>
    </div>
  `,
  styles: [`
    .search-header {
      margin-bottom: 40px;
      padding: 32px;
      display: flex;
      align-items: center;
      gap: 24px;
    }
    .search-icon-wrap {
      width: 56px;
      height: 56px;
      background: rgba(99, 102, 241, 0.15);
      border: 1px solid rgba(99, 102, 241, 0.3);
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--primary);
    }
    .search-icon-wrap svg { width: 32px; height: 32px; }
    .search-title {
      font-family: var(--font-head);
      font-size: 2rem;
      font-weight: 800;
      color: var(--text-primary);
      margin-bottom: 8px;
    }
    .search-subtitle {
      color: var(--text-muted);
      font-size: 15px;
      font-weight: 500;
    }
    .text-glow {
      background: linear-gradient(135deg, #e0e7ff, #a5b4fc);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      filter: drop-shadow(0 0 16px rgba(165,180,252,0.3));
    }
    
    .search-loading {
      text-align: center;
      padding: 80px 20px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
    }
    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid rgba(99, 102, 241, 0.2);
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }
    @keyframes spin { 100% { transform: rotate(360deg); } }
    
    .search-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 24px;
    }
  `]
})
export class SearchComponent implements OnInit {
  query = '';
  products: Product[] = [];
  loading = false;

  constructor(
    private route: ActivatedRoute, 
    private http: HttpClient,
    private cartService: CartService,
    private toast: ToastService,
    public auth: AuthService,
    private wishlistService: WishlistService
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.query = params['q'] || '';
      if (this.query) {
        this.performSemanticSearch();
      }
    });
  }

  performSemanticSearch() {
    this.loading = true;
    // Semantic Vector AI Search Call
    this.http.get<any>(`https://cognitocart-api.onrender.com/api/v1/products/search/semantic?q=${encodeURIComponent(this.query)}&limit=10`).subscribe({
      next: (res: any) => {
        // AI endpoint returns a flat List<ProductResponse>
        this.products = res;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Semantic search error', err);
        this.toast.show('Neural Search failed.', 'error');
        this.loading = false;
      }
    });
  }

  addToCart(product: Product) {
    this.cartService.addToCart(product);
  }

  toggleWishlist(product: any): void {
    if (!this.auth.isLoggedIn()) {
      this.toast.error('Please login to wishlist items');
      return;
    }
    const id = product.productPublicId || product.publicId;
    if (!id) return;
    this.wishlistService.toggle(id).subscribe({
      next: () => {
        product.isWishlisted = !product.isWishlisted;
        this.toast.success(product.isWishlisted ? 'Added to wishlist â¤ï¸' : 'Removed from wishlist');
      },
      error: (e: any) => this.toast.error(e?.error?.message || 'Wishlist update failed')
    });
  }
}
