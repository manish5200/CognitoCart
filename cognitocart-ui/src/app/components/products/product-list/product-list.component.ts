import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { ProductService } from '../../../services/product.service';
import { WishlistService } from '../../../services/wishlist.service';
import { ToastService } from '../../../services/toast.service';
import { AuthService } from '../../../services/auth.service';
import { debounceTime, Subject } from 'rxjs';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="plp-root">

      <!-- â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226; TOP BAR â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226; -->
      <div class="plp-topbar">
        <div class="plp-topbar-left">
          <h1 class="plp-heading">
            <span *ngIf="isAiSearch">ðŸ¤– AI Search</span>
            <span *ngIf="!isAiSearch && searchQuery">Results for "{{searchQuery}}"</span>
            <span *ngIf="!isAiSearch && !searchQuery">Browse Products</span>
          </h1>
          <span class="plp-count" *ngIf="!loading">{{total}} products</span>
          <span *ngIf="isAiSearch" class="plp-ai-badge">âœ¨ HuggingFace AI</span>
        </div>
        <div class="plp-topbar-right">
          <select [(ngModel)]="sortBy" class="plp-sort-select" (change)="applyFilters()">
            <option value="createdAt">Newest First</option>
            <option value="basePrice">Price: Low to High</option>
            <option value="basePrice-desc">Price: High to Low</option>
            <option value="averageRating">Best Rated</option>
          </select>
          <button class="plp-filter-toggle" (click)="filtersOpen = !filtersOpen">
            ðŸŽ›ï¸ Filters {{filtersOpen ? 'â–²' : 'â–¼'}}
          </button>
        </div>
      </div>

      <div class="plp-layout">

        <!-- â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226; SIDEBAR FILTERS â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226; -->
        <aside class="plp-sidebar" [class.open]="filtersOpen">
          <div class="plp-sidebar-inner">
            <div class="plp-filter-hdr">
              <span>ðŸŽ›ï¸ Filters</span>
              <button class="plp-clear-btn" (click)="resetFilters()">Clear All</button>
            </div>

            <!-- Search -->
            <div class="plp-filter-section">
              <div class="plp-filter-title">Search</div>
              <div class="plp-search-wrap">
                <span class="plp-search-icon">ðŸ”</span>
                <input
                  type="text"
                  [(ngModel)]="searchQuery"
                  class="plp-search-input"
                  placeholder="Search products..."
                  (ngModelChange)="onSearchChange()"
                />
                <button *ngIf="searchQuery" class="plp-search-clear" (click)="searchQuery=''; applyFilters()">âœ&#8226;</button>
              </div>
            </div>

            <!-- Category -->
            <div class="plp-filter-section">
              <div class="plp-filter-title">Category</div>
              <div class="plp-cat-pills">
                <button
                  class="plp-cat-pill"
                  [class.active]="!categoryId"
                  (click)="categoryId=''; applyFilters()"
                >All</button>
                <button
                  *ngFor="let c of categories"
                  class="plp-cat-pill"
                  [class.active]="categoryId === c.publicId"
                  (click)="categoryId = c.publicId; applyFilters()"
                >{{c.name}}</button>
              </div>
            </div>

            <!-- Price Range -->
            <div class="plp-filter-section">
              <div class="plp-filter-title">Price Range</div>
              <div style="display:flex; gap:8px;">
                <input type="number" [(ngModel)]="minPrice" class="plp-price-input" placeholder="Min &#8377;" (change)="applyFilters()" />
                <span style="color:var(--text-dim); align-self:center;">â€”</span>
                <input type="number" [(ngModel)]="maxPrice" class="plp-price-input" placeholder="Max &#8377;" (change)="applyFilters()" />
              </div>
            </div>

            <div class="plp-divider"></div>

            <!-- Rating Filter -->
            <div class="plp-filter-section">
              <div class="plp-filter-title">Minimum Rating</div>
              <div class="plp-rating-pills">
                <button class="plp-rating-pill" [class.active]="!minRating" (click)="minRating=null; applyFilters()">All</button>
                <button class="plp-rating-pill" [class.active]="minRating === 4" (click)="minRating=4; applyFilters()">4&#9733; & up</button>
                <button class="plp-rating-pill" [class.active]="minRating === 3" (click)="minRating=3; applyFilters()">3&#9733; & up</button>
              </div>
            </div>

            <div class="plp-divider"></div>

            <!-- AI Semantic Search -->
            <div class="plp-filter-section">
              <div class="plp-filter-title" style="display:flex; align-items:center; gap:6px;">
                ðŸ¤– AI Semantic Search
                <span class="plp-ai-chip">NEW</span>
              </div>
              <p class="plp-ai-desc">Find by meaning â€” try "earphones for focus" or "cozy winter clothes"</p>
              <div class="plp-ai-wrap">
                <input type="text" [(ngModel)]="aiQuery" class="plp-search-input" placeholder="Describe what you need..."
                  (keyup.enter)="aiSearch()" />
                <button class="plp-ai-btn" (click)="aiSearch()" [disabled]="!aiQuery.trim() || loading">
                  <span *ngIf="!loading">âœ¨</span>
                  <span *ngIf="loading" class="mini-spinner"></span>
                </button>
              </div>
              <!-- Quick suggestion chips â€” one click and search runs -->
              <div class="plp-ai-suggestion-chips">
                <button *ngFor="let s of aiSuggestions" class="plp-ai-chip-btn"
                  (click)="aiQuery = s; aiSearch()">{{s}}</button>
              </div>
            </div>

            <button class="plp-reset-btn" (click)="resetFilters()">Reset All Filters</button>
          </div>
        </aside>

        <!-- â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226; PRODUCT GRID â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226;â&#8226; -->
        <main class="plp-main">

          <!-- AI Result Banner â€” shown only when AI search is active -->
          <!-- Shows query, total found, and a clear button -->
          <div class="plp-ai-result-banner" *ngIf="isAiSearch && !loading">
            <span class="plp-ai-banner-icon">ðŸ¤–</span>
            <span>AI found <strong>{{total}}</strong> product{{total !== 1 ? 's' : ''}} for: <em>"{{aiQuery}}"</em></span>
            <button class="plp-ai-banner-clear" (click)="resetFilters()">âœ&#8226; Clear</button>
          </div>

          <!-- Skeleton -->
          <div *ngIf="loading" class="plp-grid">
            <div *ngFor="let s of [1,2,3,4,5,6,7,8]" class="plp-skeleton">
              <div class="skeleton plp-sk-img"></div>
              <div style="padding:14px; display:flex; flex-direction:column; gap:8px;">
                <div class="skeleton" style="height:14px; width:60%;"></div>
                <div class="skeleton" style="height:18px; width:90%;"></div>
                <div class="skeleton" style="height:22px; width:40%;"></div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div *ngIf="!loading && products.length === 0" class="empty-state" style="min-height:400px;">
            <div class="empty-icon">{{isAiSearch ? 'ðŸ¤–' : 'ðŸ”'}}</div>
            <div class="empty-title">{{isAiSearch ? 'No AI matches found' : 'No products found'}}</div>
            <div class="empty-subtitle">{{isAiSearch ? 'Try rephrasing your query or use a different description' : 'Try a different search query or clear your filters'}}</div>
            <button class="btn btn-primary" (click)="resetFilters()">Clear Filters</button>
          </div>

          <!-- Grid -->
          <div *ngIf="!loading && products.length > 0" class="plp-grid">
            <div
              *ngFor="let p of products"
              class="plp-card"
              [routerLink]="['/product', p.slug || p.productPublicId]"
            >
              <!-- Image -->
              <div class="plp-card-img-wrap">
                <img
                  [src]="p.mediaGallery?.[0]?.mediaUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&auto=format&fit=crop'"
                  [alt]="p.productName"
                  class="plp-card-img"
                  loading="lazy"
                />
                <div class="plp-card-badges">
                  <span *ngIf="p.discountPercentage > 0" class="plp-badge-off">-{{p.discountPercentage}}%</span>
                  <span *ngIf="p.flashSaleActive" class="plp-badge-flash">âš¡ FLASH</span>
                </div>
                <!-- AI relevance badge â€” only shown when in AI search mode -->
                <!-- e.g. "âœ¨ 94% match" â€” tells user WHY this product was surfaced -->
                <div class="plp-relevance-badge" *ngIf="isAiSearch && p._relevanceLabel">
                  âœ¨ {{p._relevanceLabel}} match
                </div>
                <button
                  class="plp-wishlist-btn"
                  [class.active]="p._wishlisted"
                  (click)="toggleWishlist($event, p)"
                  title="Toggle wishlist"
                >{{ p._wishlisted ? 'â¤ï¸' : 'ðŸ¤' }}</button>
              </div>

              <!-- Info -->
              <div class="plp-card-body">
                <div class="plp-card-cat">{{p.categoryName || 'General'}}</div>
                <div class="plp-card-name">{{p.productName}}</div>
                <div class="plp-card-rating" *ngIf="p.averageRating">
                  <span class="plp-stars">{{getStars(p.averageRating)}}</span>
                  <span class="plp-rating-val">{{p.averageRating | number:'1.1-1'}}</span>
                  <span class="plp-review-count">({{p.totalReviews || 0}})</span>
                </div>
                <div class="plp-card-price">
                  <span class="plp-price-now">&#8377;{{(p.discountPrice || p.price) | number:'1.0-0'}}</span>
                  <span *ngIf="p.discountPrice && p.price > p.discountPrice" class="plp-price-was">&#8377;{{p.price | number:'1.0-0'}}</span>
                </div>
              </div>

              <!-- CTA Footer -->
              <div class="plp-card-footer">
                <button class="plp-card-btn" (click)="$event.preventDefault(); $event.stopPropagation(); goToProduct(p)">
                  View Options &#8594;
                </button>
                <button
                  *ngIf="auth.isCustomer()"
                  class="plp-wishlist-footer"
                  (click)="toggleWishlist($event, p)"
                  [title]="p._wishlisted ? 'Remove from wishlist' : 'Add to wishlist'"
                >{{ p._wishlisted ? 'â¤ï¸' : 'ðŸ¤' }}</button>
              </div>
            </div>
          </div>

          <!-- Pagination -->
          <div class="plp-pagination" *ngIf="totalPages > 1 && !loading">
            <button class="plp-page-btn" (click)="changePage(page-1)" [disabled]="page === 0">â† Prev</button>
            <button
              *ngFor="let pg of pageArray"
              class="plp-page-btn"
              [class.active]="pg === page"
              (click)="changePage(pg)"
            >{{pg + 1}}</button>
            <button class="plp-page-btn" (click)="changePage(page+1)" [disabled]="page >= totalPages-1">Next &#8594;</button>
          </div>
        </main>
      </div>
    </div>
  `,
  styles: [`
    .plp-root { max-width: 1400px; margin: 0 auto; padding: 100px 24px 60px; }

    /* Topbar */
    .plp-topbar {
      display: flex; align-items: center; justify-content: space-between;
      flex-wrap: wrap; gap: 12px; margin-bottom: 24px;
    }
    .plp-topbar-left { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
    .plp-topbar-right { display: flex; align-items: center; gap: 10px; }
    .plp-heading { font-family: var(--font-head); font-size: 1.4rem; font-weight: 800; color: var(--text-primary); }
    .plp-count { font-size: 13px; color: var(--text-muted); background: var(--bg-card); border: 1px solid var(--border-subtle); padding: 3px 10px; border-radius: 20px; }
    .plp-ai-badge { font-size: 11px; font-weight: 700; background: rgba(139,92,246,0.12); color: var(--secondary); border: 1px solid rgba(139,92,246,0.3); padding: 3px 10px; border-radius: 20px; }
    .plp-sort-select { background: var(--bg-card); border: 1px solid var(--border-default); color: var(--text-primary); padding: 8px 12px; border-radius: 10px; font-size: 13px; cursor: pointer; }
    .plp-sort-select option { background: var(--bg-card); color: var(--text-primary); }
    .plp-filter-toggle { background: var(--bg-card); border: 1px solid var(--border-default); color: var(--text-muted); padding: 8px 14px; border-radius: 10px; font-size: 13px; cursor: pointer; transition: var(--transition); display: none; }
    @media (max-width: 900px) { .plp-filter-toggle { display: flex; align-items: center; gap: 6px; } }

    /* Layout */
    .plp-layout { display: grid; grid-template-columns: 260px 1fr; gap: 24px; align-items: start; }
    @media (max-width: 900px) { .plp-layout { grid-template-columns: 1fr; } }

    /* Sidebar */
    .plp-sidebar {
      position: sticky; top: 90px;
      max-height: calc(100vh - 110px); overflow-y: auto;
      scrollbar-width: thin;
    }
    @media (max-width: 900px) {
      .plp-sidebar { position: static; max-height: none; display: none; }
      .plp-sidebar.open { display: block; }
    }
    .plp-sidebar-inner {
      background: var(--bg-card); border: 1px solid var(--border-default);
      border-radius: 16px; padding: 20px;
      display: flex; flex-direction: column; gap: 0;
    }
    .plp-filter-hdr { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; font-weight: 700; color: var(--text-primary); font-size: 14px; }
    .plp-clear-btn { font-size: 12px; color: var(--primary); background: none; border: none; cursor: pointer; }
    .plp-filter-section { margin-bottom: 20px; }
    .plp-filter-title { font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--text-muted); margin-bottom: 10px; display: flex; align-items: center; gap: 6px; }
    .plp-divider { border-top: 1px solid var(--border-subtle); margin: 4px 0 20px; }
    .plp-ai-chip { font-size: 10px; font-weight: 800; background: rgba(139,92,246,0.15); color: var(--secondary); padding: 2px 6px; border-radius: 4px; }
    .plp-ai-desc { font-size: 12px; color: var(--text-dim); line-height: 1.6; margin-bottom: 10px; }
    .plp-ai-wrap { display: flex; gap: 8px; }
    .plp-ai-btn { width: 40px; height: 40px; border-radius: 10px; background: var(--primary); border: none; cursor: pointer; font-size: 16px; flex-shrink: 0; display: flex; align-items: center; justify-content: center; transition: var(--transition); }
    .plp-ai-btn:hover { opacity: 0.85; }
    .plp-ai-btn:disabled { opacity: 0.4; cursor: not-allowed; }

    /* Search input */
    .plp-search-wrap { position: relative; display: flex; align-items: center; }
    .plp-search-icon { position: absolute; left: 10px; font-size: 14px; pointer-events: none; }
    .plp-search-input { width: 100%; background: var(--bg-glass); border: 1px solid var(--border-default); color: var(--text-primary); padding: 9px 36px 9px 32px; border-radius: 10px; font-size: 13px; transition: var(--transition); }
    .plp-search-input:focus { border-color: var(--primary); outline: none; box-shadow: 0 0 0 3px rgba(96,165,250,0.15); }
    .plp-search-clear { position: absolute; right: 10px; background: none; border: none; cursor: pointer; color: var(--text-dim); font-size: 14px; }

    /* Category pills */
    .plp-cat-pills { display: flex; flex-wrap: wrap; gap: 6px; }
    .plp-cat-pill { padding: 5px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; background: var(--bg-glass); border: 1px solid var(--border-subtle); color: var(--text-muted); cursor: pointer; transition: all 0.2s; }
    .plp-cat-pill.active { background: rgba(96,165,250,0.12); border-color: rgba(96,165,250,0.4); color: var(--primary); }
    .plp-cat-pill:hover:not(.active) { border-color: var(--border-default); color: var(--text-primary); }

    /* Price inputs */
    .plp-price-input { flex: 1; min-width: 0; background: var(--bg-glass); border: 1px solid var(--border-default); color: var(--text-primary); padding: 8px 10px; border-radius: 10px; font-size: 13px; }
    .plp-price-input:focus { border-color: var(--primary); outline: none; }

    /* Rating pills */
    .plp-rating-pills { display: flex; flex-direction: column; gap: 6px; }
    .plp-rating-pill { text-align: left; padding: 6px 12px; border-radius: 8px; font-size: 13px; font-weight: 600; background: transparent; border: none; color: var(--text-muted); cursor: pointer; transition: var(--transition); }
    .plp-rating-pill.active { background: rgba(245,158,11,0.15); color: #f59e0b; }
    .plp-rating-pill:hover:not(.active) { background: var(--glass-md); color: var(--text-primary); }

    .plp-reset-btn { width: 100%; padding: 10px; border-radius: 10px; background: var(--bg-glass); border: 1px solid var(--border-subtle); color: var(--text-muted); cursor: pointer; font-size: 13px; font-weight: 600; transition: var(--transition); }
    .plp-reset-btn:hover { border-color: var(--border-default); color: var(--text-primary); }

    /* Product Grid */
    .plp-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
      gap: 18px;
    }

    /* Skeleton */
    .plp-skeleton { background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: 14px; overflow: hidden; }
    .plp-sk-img { height: 220px; }

    /* Product Card */
    .plp-card {
      background: var(--bg-card);
      border: 1px solid var(--border-default);
      border-radius: 14px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      cursor: pointer;
      transition: all 0.3s ease;
      position: relative;
    }
    .plp-card:hover {
      transform: translateY(-5px);
      border-color: rgba(96,165,250,0.3);
      box-shadow: 0 16px 40px rgba(0,0,0,0.25), 0 0 0 1px rgba(96,165,250,0.1);
    }
    .plp-card-img-wrap {
      position: relative;
      aspect-ratio: 1/1;
      overflow: hidden;
      background: var(--bg-glass);
    }
    .plp-card-img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s ease; }
    .plp-card:hover .plp-card-img { transform: scale(1.07); }

    .plp-card-badges { position: absolute; top: 8px; left: 8px; display: flex; flex-direction: column; gap: 4px; }
    .plp-badge-off { background: linear-gradient(135deg, #10b981, #059669); color: #fff; font-size: 10px; font-weight: 800; padding: 3px 8px; border-radius: 20px; }
    .plp-badge-flash { background: linear-gradient(135deg, #f59e0b, #ef4444); color: #fff; font-size: 10px; font-weight: 800; padding: 3px 8px; border-radius: 20px; }

    .plp-wishlist-btn {
      position: absolute; top: 8px; right: 8px;
      width: 32px; height: 32px; border-radius: 50%;
      background: rgba(15,23,42,0.65); border: 1px solid var(--border-subtle);
      backdrop-filter: blur(8px); cursor: pointer; font-size: 14px;
      display: flex; align-items: center; justify-content: center;
      transition: all 0.2s; opacity: 0;
    }
    .plp-card:hover .plp-wishlist-btn { opacity: 1; }
    .plp-wishlist-btn.active { opacity: 1; background: rgba(239,68,68,0.15); border-color: rgba(239,68,68,0.3); }
    .plp-wishlist-btn:hover { transform: scale(1.1); }

    .plp-card-body { padding: 14px; flex: 1; display: flex; flex-direction: column; gap: 5px; }
    .plp-card-cat { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--primary); }
    .plp-card-name {
      font-size: 13px; font-weight: 700; color: var(--text-primary); line-height: 1.4;
      display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
    }
    .plp-card-rating { display: flex; align-items: center; gap: 4px; font-size: 12px; }
    .plp-stars { color: #f59e0b; font-size: 12px; }
    .plp-rating-val { color: var(--text-muted); font-weight: 600; }
    .plp-review-count { color: var(--text-dim); }
    .plp-card-price { display: flex; align-items: baseline; gap: 8px; margin-top: auto; padding-top: 6px; }
    .plp-price-now { font-family: var(--font-head); font-size: 1rem; font-weight: 800; color: var(--primary); }
    .plp-price-was { font-size: 11px; color: var(--text-dim); text-decoration: line-through; }

    .plp-card-footer {
      padding: 10px 14px;
      border-top: 1px solid var(--border-subtle);
      display: flex; gap: 8px; align-items: center;
    }
    .plp-card-btn {
      flex: 1; padding: 8px 12px; border-radius: 8px;
      background: linear-gradient(135deg, var(--primary), var(--secondary));
      border: none; color: #fff; font-size: 12px; font-weight: 700;
      cursor: pointer; transition: all 0.2s;
    }
    .plp-card-btn:hover { opacity: 0.9; transform: scale(1.02); }
    .plp-wishlist-footer {
      width: 34px; height: 34px; border-radius: 8px;
      background: var(--bg-glass); border: 1px solid var(--border-subtle);
      cursor: pointer; font-size: 15px; display: flex; align-items: center; justify-content: center;
      transition: var(--transition);
    }
    .plp-wishlist-footer:hover { border-color: rgba(239,68,68,0.3); background: rgba(239,68,68,0.06); }

    /* Pagination */
    .plp-pagination { display: flex; justify-content: center; align-items: center; gap: 6px; margin-top: 32px; flex-wrap: wrap; }
    .plp-page-btn { padding: 8px 14px; border-radius: 8px; background: var(--bg-card); border: 1px solid var(--border-default); color: var(--text-muted); cursor: pointer; font-size: 13px; font-weight: 600; transition: var(--transition); }
    .plp-page-btn:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
    .plp-page-btn.active { background: linear-gradient(135deg, var(--primary), var(--secondary)); color: #fff; border-color: transparent; }
    .plp-page-btn:disabled { opacity: 0.3; cursor: not-allowed; }

    .mini-spinner { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite; display: inline-block; }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* â”€â”€ AI Suggestion Chips â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    /* Pre-made search queries so users know what they can type */
    .plp-ai-suggestion-chips { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 10px; }
    .plp-ai-chip-btn {
      font-size: 0.7rem; padding: 3px 9px; border-radius: 20px;
      background: rgba(99,102,241,0.12); border: 1px solid rgba(99,102,241,0.3);
      color: #818cf8; cursor: pointer; transition: all 0.2s; white-space: nowrap;
    }
    .plp-ai-chip-btn:hover { background: rgba(99,102,241,0.3); transform: scale(1.04); }

    /* â”€â”€ AI Result Banner â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    /* The strip shown above the grid when AI mode is active */
    .plp-ai-result-banner {
      display: flex; align-items: center; gap: 10px;
      padding: 10px 16px; margin-bottom: 14px;
      background: linear-gradient(135deg, rgba(26,26,46,0.95), rgba(22,33,62,0.95));
      border: 1px solid rgba(99,102,241,0.35); border-radius: 10px;
      color: #e2e8f0; font-size: 0.875rem;
      animation: fadeIn 0.3s ease;
    }
    .plp-ai-banner-icon { font-size: 1.2rem; }
    .plp-ai-result-banner em { color: #a5b4fc; }
    .plp-ai-result-banner strong { color: #fff; }
    .plp-ai-banner-clear {
      margin-left: auto; background: rgba(99,102,241,0.15);
      border: 1px solid rgba(99,102,241,0.4); color: #818cf8;
      border-radius: 6px; padding: 3px 10px; cursor: pointer; font-size: 0.78rem;
      transition: all 0.2s;
    }
    .plp-ai-banner-clear:hover { background: rgba(99,102,241,0.35); }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: translateY(0); } }

    /* â”€â”€ Relevance Score Badge â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
    /* Floats on top of the product card image, shows AI confidence */
    .plp-relevance-badge {
      position: absolute; bottom: 8px; left: 8px;
      background: linear-gradient(135deg, #4f46e5, #7c3aed);
      color: white; font-size: 0.67rem; font-weight: 700;
      padding: 3px 8px; border-radius: 10px;
      letter-spacing: 0.3px; z-index: 2;
      box-shadow: 0 2px 8px rgba(79,70,229,0.4);
    }
  `]
})
export class ProductListComponent implements OnInit {
  products: any[] = [];
  categories: any[] = [];
  loading = false;
  searchQuery = '';
  aiQuery = '';
  categoryId = '';
  minPrice: number | null = null;
  maxPrice: number | null = null;
  minRating: number | null = null;
  sortBy = 'createdAt';
  page = 0;
  size = 20;
  total = 0;
  totalPages = 0;
  isAiSearch = false;
  filtersOpen = false;
  private searchSubject = new Subject<string>();

  // Stores the last AI search ranked results (with relevance scores)
  aiResults: any[] = [];

  // Pre-made search suggestions â€” gives users inspiration on what to type
  aiSuggestions = [
    'Best for gaming ðŸŽ®',
    'Under &#8377;1,000 ðŸŽ',
    'Wireless & compact',
    'Top rated â­',
    'Noise cancelling ðŸŽ§',
  ];

  get pageArray(): number[] {
    const start = Math.max(0, this.page - 3);
    const end = Math.min(this.totalPages, start + 7);
    return Array.from({ length: end - start }, (_, i) => start + i);
  }

  constructor(
    private productService: ProductService,
    private wishlistService: WishlistService,
    private toast: ToastService,
    public auth: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.productService.getCategories().subscribe({ next: c => this.categories = c, error: () => {} });

    this.route.queryParams.subscribe(params => {
      this.searchQuery = params['q'] || '';
      this.categoryId = params['categoryId'] || '';
      
      const aiq = params['aiq'] || '';
      if (aiq) {
        this.aiQuery = aiq;
        this.aiSearch();
      } else {
        this.isAiSearch = false;
        this.aiQuery = '';
        this.applyFilters();
      }
    });

    this.searchSubject.pipe(debounceTime(500)).subscribe(() => this.applyFilters());
  }

  onSearchChange(): void { this.searchSubject.next(this.searchQuery); }

  applyFilters(): void {
    this.page = 0;
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.isAiSearch = false;
    const sortDir = this.sortBy.endsWith('-desc') ? 'desc' : 'asc';
    const sortField = this.sortBy.replace('-desc', '');

    this.productService.search({
      name: this.searchQuery || undefined,
      categoryId: this.categoryId || undefined,
      minPrice: this.minPrice ?? undefined,
      maxPrice: this.maxPrice ?? undefined,
      minRating: this.minRating ?? undefined,
      page: this.page,
      size: this.size,
      sortBy: sortField,
      direction: sortDir
    }).subscribe({
      next: (res) => {
        const data = (res as any)['Search result'] || res;
        this.products = (data as any).content ?? data ?? [];
        this.total = (data as any).totalElements ?? this.products.length;
        this.totalPages = (data as any).totalPages ?? 1;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load products');
      }
    });
  }

  aiSearch(): void {
    if (!this.aiQuery.trim()) return;
    this.loading = true;
    this.isAiSearch = true;
    this.aiResults = [];

    // Pass current sidebar filters alongside the AI query â€” hybrid search!
    // So "wireless headphones under &#8377;5000" works with both AI + price filter active
    this.productService.semanticSearch(this.aiQuery, 20, {
      minPrice: this.minPrice ?? undefined,
      maxPrice: this.maxPrice ?? undefined,
      minRating: this.minRating ?? undefined
    }).subscribe({
      next: (res: any) => {
        // Handle both response shapes:
        // New shape: { query, totalFound, results: [{product, relevanceScore, relevanceLabel, rank}] }
        // Old shape (fallback): plain array
        if (res && Array.isArray(res.results)) {
          // New backend response with relevance scores
          this.aiResults = res.results;
          this.products = res.results.map((r: any) => ({
            ...r.product,
            _relevanceLabel: r.relevanceLabel,  // e.g. "94%" â€” shown as badge on card
            _rank: r.rank                        // position 1 = best match
          }));
          this.total = res.totalFound ?? this.products.length;
        } else if (Array.isArray(res)) {
          // Fallback: old plain array response (backwards compatible)
          this.products = res;
          this.total = res.length;
        } else {
          this.products = [];
          this.total = 0;
        }
        this.totalPages = 1;  // AI results are ranked â€” no pagination
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('AI Search failed. Please try again.');
      }
    });
  }

  resetFilters(): void {
    this.searchQuery = ''; this.categoryId = ''; this.minPrice = null;
    this.maxPrice = null; this.minRating = null; this.sortBy = 'createdAt'; this.isAiSearch = false;
    this.aiQuery = ''; this.applyFilters();
  }

  changePage(p: number): void {
    if (p < 0 || p >= this.totalPages) return;
    this.page = p;
    this.loadProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  goToProduct(product: any): void {
    this.router.navigate(['/product', product.slug || product.productPublicId]);
  }

  toggleWishlist(e: Event, product: any): void {
    e.preventDefault(); e.stopPropagation();
    if (!this.auth.isLoggedIn()) { this.toast.warning('Login to wishlist'); return; }
    this.wishlistService.toggle(product.productPublicId).subscribe({
      next: (res: any) => {
        product._wishlisted = !product._wishlisted;
        this.toast.info(res.Status || 'Wishlist updated');
      },
      error: () => this.toast.error('Failed to update wishlist')
    });
  }

  getStars(rating: number): string {
    const full = Math.round(rating);
    return '&#9733;'.repeat(Math.min(full, 5)) + 'â˜†'.repeat(Math.max(5 - full, 0));
  }
}

