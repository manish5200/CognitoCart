import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { OrderService } from '../../../services/order.service';
import { WishlistService } from '../../../services/wishlist.service';
import { CartService } from '../../../services/cart.service';
import { ProductService } from '../../../services/product.service';
import { ProductCardComponent } from '../../shared/product-card/product-card.component';
import { Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-customer-home',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent, FormsModule],
  templateUrl: './customer-home.component.html',
  styleUrls: ['./customer-home.component.css']
})
export class CustomerHomeComponent implements OnInit, OnDestroy {
  userName = '';
  aiQuery = '';

  // Cycling placeholder — rotates every 2.5s to show users what they can search
  placeholderTexts = [
    'Best headphones for gym... 🎧',
    'Laptop under ₹50,000... 💻',
    'Cozy winter jacket... 🧥',
    'Gift ideas under ₹500... 🎁',
    'Wireless earbuds for calls... 📞',
    'Gaming gear for beginners... 🎮',
  ];
  currentPlaceholder = this.placeholderTexts[0];
  private placeholderInterval?: ReturnType<typeof setInterval>;

  // Stores last 3 searches in browser localStorage so user can retap quickly
  searchHistory: string[] = [];

  // States
  activeOrder: any = null;
  loadingOrder = true;

  cartItems: any[] = [];
  loadingCart = true;

  wishlistItems: any[] = [];
  loadingWishlist = true;

  // E-commerce Sections
  recommendedProducts: any[] = [];
  deals: any[] = [];
  categories: any[] = [];
  recentlyViewed: any[] = [];
  loadingProducts = true;
  loadingCategories = true;

  private cartSub?: Subscription;

  constructor(
    private auth: AuthService,
    private orderService: OrderService,
    private wishlistService: WishlistService,
    private cartService: CartService,
    private productService: ProductService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentUser();
    this.userName = user?.name?.split(' ')[0] || 'Shopper';

    this.fetchActiveOrder();
    this.setupCartSubscription();
    this.fetchWishlist();
    this.fetchProductsAndCategories();
    this.loadRecentlyViewed();
    this.loadSearchHistory();    // Load previous searches from browser
    this.startPlaceholderCycle(); // Start animated placeholder
  }

  ngOnDestroy(): void {
    if (this.cartSub) this.cartSub.unsubscribe();
    // Stop the placeholder rotation timer to avoid memory leaks
    if (this.placeholderInterval) clearInterval(this.placeholderInterval);
  }

  // Cycles through placeholder texts every 2.5s so users see search ideas
  private startPlaceholderCycle(): void {
    let i = 0;
    this.placeholderInterval = setInterval(() => {
      i = (i + 1) % this.placeholderTexts.length;
      this.currentPlaceholder = this.placeholderTexts[i];
    }, 2500);
  }

  // Reads the last 3 searches saved in the browser's localStorage
  private loadSearchHistory(): void {
    try {
      const stored = localStorage.getItem('cognito_search_history');
      this.searchHistory = stored ? JSON.parse(stored) : [];
    } catch { this.searchHistory = []; }
  }

  private fetchActiveOrder(): void {
    this.orderService.getMyOrders(0, 1).subscribe({
      next: (res: any) => {
        const orders = res.content || res || [];
        if (orders.length > 0) {
          const latest = orders[0];
          // Check if active (not delivered, not cancelled)
          if (!['DELIVERED', 'CANCELLED'].includes(latest.status)) {
            this.activeOrder = latest;
          }
        }
        this.loadingOrder = false;
      },
      error: () => {
        this.loadingOrder = false;
      }
    });
  }

  private setupCartSubscription(): void {
    this.cartSub = this.cartService.cart$.subscribe({
      next: (cart) => {
        this.cartItems = cart?.items || [];
        this.loadingCart = false;
      },
      error: () => {
        this.loadingCart = false;
      }
    });
    // Fetch cart to check if items exist
    this.cartService.refresh().subscribe();
  }

  private fetchWishlist(): void {
    this.wishlistService.getAll().subscribe({
      next: (w: any) => {
        this.wishlistItems = Array.isArray(w) ? w : (w.content || []);
        this.loadingWishlist = false;
      },
      error: () => {
        this.loadingWishlist = false;
      }
    });
  }

  private fetchProductsAndCategories(): void {
    // Fetch products for recommendations and deals
    this.productService.getAll(0, 10).subscribe({
      next: (res: any) => {
        const prods = res.content || res || [];
        // Split for demo purposes
        this.recommendedProducts = prods.slice(0, 5);
        this.deals = prods.slice(5, 10);
        this.loadingProducts = false;
      },
      error: () => {
        this.loadingProducts = false;
      }
    });

    // Fetch categories
    this.productService.getCategories().subscribe({
      next: (res: any) => {
        this.categories = res || [];
        this.loadingCategories = false;
      },
      error: () => {
        this.loadingCategories = false;
      }
    });
  }

  private loadRecentlyViewed(): void {
    try {
      const stored = localStorage.getItem('cognito_recent_products');
      if (stored) {
        this.recentlyViewed = JSON.parse(stored);
      }
    } catch (e) {
      console.error('Failed to load recent products', e);
    }
  }

  askCognito(chipQuery?: string): void {
    const q = chipQuery || this.aiQuery;
    if (!q.trim()) return;

    // Save to search history (max 3 entries, no duplicates)
    this.searchHistory = [q, ...this.searchHistory.filter(h => h !== q)].slice(0, 3);
    localStorage.setItem('cognito_search_history', JSON.stringify(this.searchHistory));

    this.router.navigate(['/products'], { queryParams: { aiq: q.trim() } });
  }
}
