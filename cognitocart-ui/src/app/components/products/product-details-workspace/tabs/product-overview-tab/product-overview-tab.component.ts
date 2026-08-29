import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductReturnStatusComponent } from '../../../../shared/product-return-status/product-return-status.component';
import { ProductService } from '../../../../../services/product.service';

@Component({
  selector: 'app-product-overview-tab',
  standalone: true,
  imports: [CommonModule, ProductReturnStatusComponent],
  template: `
    <div class="dashboard-grid" *ngIf="product">
      <!-- Main Content Column -->
      <div class="main-col">
        <!-- Hero Section with Glassmorphism -->
        <div class="hero-card">
          <div class="hero-image-container">
            <img *ngIf="product.imageUrls && product.imageUrls.length > 0" [src]="product.imageUrls[0]" alt="Product Cover" class="hero-image">
            <div *ngIf="!product.imageUrls || product.imageUrls.length === 0" class="hero-placeholder">
              <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
            </div>
          </div>
          
          <div class="hero-info">
            <div class="badge-row">
              <span class="badge" [ngClass]="'status-' + (product.status ? product.status.toLowerCase() : 'active')">
                <span class="status-dot"></span> {{ product.status || 'ACTIVE' }}
              </span>
              <span class="badge category-badge">{{ product.categoryName || 'Uncategorized' }}</span>
              <app-product-return-status [isReturnable]="product.isReturnable"></app-product-return-status>
            </div>
            
            <h1 class="product-title">{{ product.productName || 'Unnamed Product' }}</h1>
            
            <div class="price-container">
              <span class="current-price">₹{{ (product.discountPrice || product.price || 0) | number:'1.0-2' }}</span>
              <span class="original-price" *ngIf="product.discountPrice && product.price > product.discountPrice">₹{{ product.price | number:'1.0-2' }}</span>
            </div>
            
            <p class="product-desc">{{ product.description || 'No description provided.' }}</p>
          </div>
        </div>

        <!-- AI Insights Panel -->
        <div class="ai-panel">
          <div class="ai-header">
            <div class="ai-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a10 10 0 1 0 10 10H12V2z"></path><path d="M21.18 8.02c-1-2.3-2.85-4.17-5.16-5.18"></path><path d="M21.18 15.98a10.02 10.02 0 0 1-9.2 5.92"></path></svg>
            </div>
            <h3>Cognito AI Insights</h3>
          </div>
          <div class="ai-content">
            <p *ngIf="product.aiSummary">{{ product.aiSummary }}</p>
            <p *ngIf="!product.aiSummary" class="ai-placeholder">No AI insights generated yet. AI analyzes customer reviews, semantic tags, and performance to generate insights.</p>
          </div>
          <div class="ai-footer" *ngIf="product.insightLastGenerated">
            Last updated: {{ product.insightLastGenerated | date:'medium' }}
          </div>
        </div>
      </div>

      <!-- Side Column for Stats & Charts -->
      <div class="side-col">
        <!-- Sales Trend Mock Chart -->
        <div class="glass-card">
          <div class="card-header">
            <h3>Sales Velocity (30 Days)</h3>
            <span class="trend-up">+14.2%</span>
          </div>
          <div class="chart-container">
            <svg viewBox="0 0 300 100" class="sparkline">
              <defs>
                <linearGradient id="lineGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" stop-color="#4f46e5" />
                  <stop offset="100%" stop-color="#ec4899" />
                </linearGradient>
                <linearGradient id="fillGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stop-color="#ec4899" stop-opacity="0.2" />
                  <stop offset="100%" stop-color="#ec4899" stop-opacity="0" />
                </linearGradient>
              </defs>
              <path d="M 0,80 Q 20,70 40,85 T 80,60 T 120,75 T 160,30 T 200,45 T 240,20 T 300,10" fill="none" stroke="url(#lineGrad)" stroke-width="3" stroke-linecap="round" />
              <path d="M 0,80 Q 20,70 40,85 T 80,60 T 120,75 T 160,30 T 200,45 T 240,20 T 300,10 L 300,100 L 0,100 Z" fill="url(#fillGrad)" />
            </svg>
          </div>
          <div class="chart-footer">
            <span>Aug 1</span>
            <span>Aug 30</span>
          </div>
        </div>

        <!-- Inventory Summary -->
        <div class="glass-card">
          <div class="card-header">
            <h3>Inventory Snapshot</h3>
          </div>
          <div class="stat-list">
            <div class="stat-item">
              <div class="stat-label">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect></svg>
                Total Variants
              </div>
              <div class="stat-value">{{ totalVariants }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                Total Orders
              </div>
              <div class="stat-value">{{ product.totalSold || 0 }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
                Avg. Rating
              </div>
              <div class="stat-value" [style.color]="product.averageRating > 4 ? '#10b981' : '#f59e0b'">
                {{ product.averageRating ? (product.averageRating | number:'1.1-1') : 'N/A' }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-grid {
      display: grid;
      grid-template-columns: 2fr 1fr;
      gap: 32px;
      animation: fadeIn 0.4s cubic-bezier(0.16, 1, 0.3, 1);
    }
    @media (max-width: 1024px) {
      .dashboard-grid { grid-template-columns: 1fr; }
    }

    /* Main Column - Hero Card */
    .hero-card {
      background: rgba(30, 41, 59, 0.4);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 20px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      box-shadow: 0 20px 40px -10px rgba(0,0,0,0.5);
      backdrop-filter: blur(20px);
      margin-bottom: 24px;
    }
    .hero-image-container {
      width: 100%;
      height: 280px;
      background: #0f172a;
      position: relative;
    }
    .hero-image {
      width: 100%;
      height: 100%;
      object-fit: contain;
      opacity: 0.9;
      padding-bottom: 20px;
      mask-image: linear-gradient(to bottom, black 60%, transparent 100%);
      -webkit-mask-image: linear-gradient(to bottom, black 60%, transparent 100%);
    }
    .hero-placeholder {
      width: 100%; height: 100%;
      display: flex; align-items: center; justify-content: center;
      color: rgba(255,255,255,0.1);
      background: linear-gradient(135deg, rgba(30,41,59,1) 0%, rgba(15,23,42,1) 100%);
    }
    
    .hero-info {
      padding: 32px;
      margin-top: -80px; /* Pull content up over the faded image */
      position: relative;
      z-index: 10;
    }
    .badge-row {
      display: flex; gap: 12px; align-items: center; margin-bottom: 16px;
    }
    .badge {
      display: inline-flex; align-items: center; gap: 6px;
      padding: 6px 12px; border-radius: 20px; font-size: 11px; font-weight: 700;
      letter-spacing: 0.5px; text-transform: uppercase;
      background: rgba(255,255,255,0.1); color: #fff;
      border: 1px solid rgba(255,255,255,0.1);
      backdrop-filter: blur(10px);
    }
    .status-active { background: rgba(16,185,129,0.15); color: #10b981; border-color: rgba(16,185,129,0.3); }
    .status-draft { background: rgba(245,158,11,0.15); color: #f59e0b; border-color: rgba(245,158,11,0.3); }
    .status-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; box-shadow: 0 0 8px currentColor; }
    
    .product-title {
      font-size: 2.5rem; font-weight: 800; color: #fff; margin-bottom: 12px;
      letter-spacing: -0.5px; line-height: 1.1;
      text-shadow: 0 2px 10px rgba(0,0,0,0.5);
    }
    
    .price-container {
      display: flex; align-items: baseline; gap: 12px; margin-bottom: 24px;
    }
    .current-price {
      font-size: 2rem; font-weight: 700; color: #10b981;
      font-family: var(--font-heading);
    }
    .original-price {
      font-size: 1.25rem; font-weight: 500; color: var(--text-muted);
      text-decoration: line-through;
    }
    
    .product-desc {
      font-size: 16px; color: var(--text-secondary); line-height: 1.6;
      max-width: 100%;
    }

    /* AI Insights Panel */
    .ai-panel {
      background: linear-gradient(145deg, rgba(236,72,153,0.1) 0%, rgba(99,102,241,0.05) 100%);
      border: 1px solid rgba(236,72,153,0.2);
      border-radius: 20px; padding: 24px;
      position: relative; overflow: hidden;
    }
    .ai-panel::before {
      content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 2px;
      background: linear-gradient(90deg, #ec4899, #6366f1);
    }
    .ai-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
    .ai-icon {
      width: 32px; height: 32px; border-radius: 8px; background: rgba(236,72,153,0.2);
      color: #ec4899; display: flex; align-items: center; justify-content: center;
    }
    .ai-header h3 { font-size: 16px; font-weight: 700; color: #fff; margin: 0; }
    .ai-content p { font-size: 15px; color: var(--text-secondary); line-height: 1.6; margin: 0; }
    .ai-placeholder { font-style: italic; opacity: 0.7; }
    .ai-footer { margin-top: 16px; font-size: 12px; color: var(--text-dim); }

    /* Side Column Cards */
    .glass-card {
      background: rgba(30, 41, 59, 0.4);
      border: 1px solid rgba(255, 255, 255, 0.05);
      border-radius: 20px; padding: 24px;
      margin-bottom: 24px;
      backdrop-filter: blur(20px);
    }
    .card-header {
      display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
    }
    .card-header h3 { font-size: 15px; font-weight: 600; color: #fff; margin: 0; }
    .trend-up { font-size: 13px; font-weight: 700; color: #10b981; background: rgba(16,185,129,0.15); padding: 4px 8px; border-radius: 6px; }
    
    .chart-container { height: 100px; width: 100%; margin-bottom: 12px; }
    .sparkline { width: 100%; height: 100%; overflow: visible; }
    .chart-footer { display: flex; justify-content: space-between; font-size: 11px; color: var(--text-dim); font-weight: 600; text-transform: uppercase; }

    /* Stats List */
    .stat-list { display: flex; flex-direction: column; gap: 16px; }
    .stat-item {
      display: flex; justify-content: space-between; align-items: center;
      padding-bottom: 16px; border-bottom: 1px solid rgba(255,255,255,0.05);
    }
    .stat-item:last-child { border-bottom: none; padding-bottom: 0; }
    .stat-label { display: flex; align-items: center; gap: 12px; font-size: 14px; color: var(--text-secondary); }
    .stat-label svg { color: var(--text-dim); }
    .stat-value { font-size: 16px; font-weight: 700; color: #fff; font-family: var(--font-heading); }

    @keyframes fadeIn { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class ProductOverviewTabComponent implements OnInit, OnChanges {
  @Input() product: any;
  totalVariants = 0;

  constructor(private productService: ProductService) {}

  ngOnInit() {
    this.loadVariants();
  }

  ngOnChanges() {
    this.loadVariants();
  }

  private loadVariants() {
    if (this.product && this.product.productPublicId) {
      this.productService.getVariants(this.product.productPublicId).subscribe({
        next: (variants: any) => {
          this.totalVariants = variants?.length || 0;
        },
        error: () => {}
      });
    }
  }
}
