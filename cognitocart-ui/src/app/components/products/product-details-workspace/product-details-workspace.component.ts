import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProductService } from '../../../services/product.service';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { ProductOverviewTabComponent } from './tabs/product-overview-tab/product-overview-tab.component';
import { ProductReturnsTabComponent } from './tabs/product-returns-tab/product-returns-tab.component';
import { ProductInfoTabComponent } from './tabs/product-info-tab/product-info-tab.component';
import { ProductMediaTabComponent } from './tabs/product-media-tab/product-media-tab.component';
import { ProductVariantsTabComponent } from './tabs/product-variants-tab/product-variants-tab.component';
import { ProductAdminControlsComponent } from './tabs/product-admin-controls/product-admin-controls.component';
import { ProductAiInsightsComponent } from './tabs/product-ai-insights/product-ai-insights.component';
import { ProductReturnStatusComponent } from '../../shared/product-return-status/product-return-status.component';

@Component({
  selector: 'app-product-details-workspace',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductOverviewTabComponent, ProductReturnsTabComponent, ProductInfoTabComponent, ProductMediaTabComponent, ProductVariantsTabComponent, ProductAdminControlsComponent, ProductAiInsightsComponent, ProductReturnStatusComponent],
  template: `
    <div class="workspace-page">
      <!-- Top Navigation Bar -->
      <div class="workspace-header">
        <div class="breadcrumb">
          <a [routerLink]="isAdmin ? '/admin/products' : '/seller/products'">{{ isAdmin ? 'All Products' : 'My Products' }}</a>
          <span class="sep">&#8250;</span>
          <span class="current">{{ product?.productName || 'Loading...' }}</span>
          <app-product-return-status *ngIf="product" [isReturnable]="product.isReturnable" style="margin-left: 12px;"></app-product-return-status>
        </div>
        <div class="actions" *ngIf="product">
          <button class="btn btn-ghost" *ngIf="product.status === 'DRAFT'">Discard Draft</button>
          <a [routerLink]="['/product', product.slug]" target="_blank" class="btn btn-secondary">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
            Preview Live
          </a>
          <button class="btn btn-primary" (click)="saveChanges()" [disabled]="isSaving">
            <span *ngIf="isSaving" class="spinner spinner-sm"></span>
            <svg *ngIf="!isSaving" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1-2-2h11l5 5v11a2 2 0 0 1-2 2z"></path><polyline points="17 21 17 13 7 13 7 21"></polyline><polyline points="7 3 7 8 15 8"></polyline></svg>
            {{ isSaving ? 'Saving...' : 'Save Changes' }}
          </button>
        </div>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div class="workspace-content" *ngIf="!loading && product">
        <!-- Sidebar Navigation -->
        <div class="workspace-sidebar">
          <div class="nav-section">
            <h4 class="nav-heading">Product Details</h4>
            <a class="nav-item" [class.active]="activeTab === 'overview'" (click)="activeTab = 'overview'">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><line x1="3" y1="9" x2="21" y2="9"></line><line x1="9" y1="21" x2="9" y2="9"></line></svg>
              Overview
            </a>
            <a class="nav-item" [class.active]="activeTab === 'info'" (click)="activeTab = 'info'">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
              Basic Info
            </a>
            <a class="nav-item" [class.active]="activeTab === 'media'" (click)="activeTab = 'media'">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
              Media & Images
            </a>
          </div>

          <div class="nav-section">
            <h4 class="nav-heading">Inventory & Sales</h4>
            <a class="nav-item" [class.active]="activeTab === 'variants'" (click)="activeTab = 'variants'">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>
              Variants & SKUs
            </a>
            <a class="nav-item" [class.active]="activeTab === 'returns'" (click)="activeTab = 'returns'">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
              Return Policy
            </a>
          </div>

          <div class="nav-section" *ngIf="isAdmin">
            <h4 class="nav-heading">Admin Controls</h4>
            <a class="nav-item" [class.active]="activeTab === 'admin'" (click)="activeTab = 'admin'">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
              Moderation
            </a>
            <a class="nav-item ai-nav-item" [class.active]="activeTab === 'ai'" (click)="activeTab = 'ai'">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a10 10 0 1 0 10 10H12V2z"></path><path d="M21.18 8.02c-1-2.3-2.85-4.17-5.16-5.18"></path><path d="M21.18 15.98a10.02 10.02 0 0 1-9.2 5.92"></path></svg>
              AI Insights
            </a>
          </div>
        </div>

        <!-- Main Workspace Area -->
        <div class="workspace-main">
          <!-- Overview Tab -->
          <div *ngIf="activeTab === 'overview'" class="tab-content">
            <app-product-overview-tab [product]="product"></app-product-overview-tab>
          </div>

          <!-- Basic Info Tab -->
          <div *ngIf="activeTab === 'info'" class="tab-content">
            <app-product-info-tab [product]="product"></app-product-info-tab>
          </div>
          
          <!-- Media Tab -->
          <div *ngIf="activeTab === 'media'" class="tab-content">
            <app-product-media-tab [product]="product"></app-product-media-tab>
          </div>
          
          <!-- Variants Tab -->
          <div *ngIf="activeTab === 'variants'" class="tab-content">
            <app-product-variants-tab [product]="product"></app-product-variants-tab>
          </div>
          
          <!-- Returns Tab -->
          <div *ngIf="activeTab === 'returns'" class="tab-content">
            <app-product-returns-tab [product]="product"></app-product-returns-tab>
          </div>
          
          <!-- Admin Moderation Tab -->
          <div *ngIf="activeTab === 'admin'" class="tab-content">
            <app-product-admin-controls [product]="product"></app-product-admin-controls>
          </div>

          <!-- AI Tab -->
          <div *ngIf="activeTab === 'ai'" class="tab-content">
            <app-product-ai-insights [product]="product"></app-product-ai-insights>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .workspace-page { min-height: 100vh; background: var(--bg-body); color: var(--text-main); display: flex; flex-direction: column; padding-top: 70px; }
    
    /* Header */
    .workspace-header { 
      display: flex; justify-content: space-between; align-items: center; 
      padding: 16px 32px; background: var(--bg-card); 
      border-bottom: 1px solid var(--border-default);
      position: sticky; top: 0; z-index: 100;
    }
    .breadcrumb { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 500; }
    .breadcrumb a { color: var(--text-muted); text-decoration: none; transition: color 0.2s; }
    .breadcrumb a:hover { color: #fff; }
    .breadcrumb .sep { color: var(--text-dim); }
    .breadcrumb .current { color: #fff; }
    
    .actions { display: flex; gap: 12px; align-items: center; }
    .actions .btn { display: inline-flex; align-items: center; gap: 8px; padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; border: none; }
    .btn-ghost { background: transparent; color: var(--text-muted); }
    .btn-ghost:hover { background: rgba(255,255,255,0.05); color: #fff; }
    .btn-secondary { background: rgba(255,255,255,0.05); color: #fff; border: 1px solid rgba(255,255,255,0.1); }
    .btn-secondary:hover { background: rgba(255,255,255,0.1); }
    .btn-primary { background: var(--primary); color: #fff; }
    .btn-primary:hover { background: var(--primary-hover); transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.3); }

    /* Content Layout */
    .workspace-content { display: flex; flex: 1; overflow: hidden; }
    
    /* Sidebar */
    .workspace-sidebar {
      width: 260px; background: rgba(15,23,42,0.6); 
      border-right: 1px solid var(--border-default);
      padding: 24px 16px; display: flex; flex-direction: column; gap: 32px;
      overflow-y: auto;
    }
    .nav-section { display: flex; flex-direction: column; gap: 4px; }
    .nav-heading { font-size: 11px; text-transform: uppercase; letter-spacing: 1px; color: var(--text-dim); margin-bottom: 8px; padding-left: 12px; }
    .nav-item {
      display: flex; align-items: center; gap: 12px; padding: 10px 12px;
      color: var(--text-secondary); text-decoration: none; font-size: 14px; font-weight: 500;
      border-radius: 8px; cursor: pointer; transition: all 0.2s;
    }
    .nav-item svg { color: var(--text-dim); transition: color 0.2s; }
    .nav-item:hover { background: rgba(255,255,255,0.05); color: #fff; }
    .nav-item:hover svg { color: var(--text-secondary); }
    
    .nav-item.active { background: rgba(99,102,241,0.1); color: var(--primary); }
    .nav-item.active svg { color: var(--primary); }

    .ai-nav-item.active { background: rgba(236,72,153,0.1); color: #ec4899; }
    .ai-nav-item.active svg { color: #ec4899; }

    /* Main Area */
    .workspace-main { flex: 1; padding: 32px; overflow-y: auto; background: var(--bg-body); }
    .tab-content { max-width: 1000px; margin: 0 auto; animation: fadeIn 0.3s ease; }
    
    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class ProductDetailsWorkspaceComponent implements OnInit {
  product: any = null;
  loading = true;
  isAdmin = false;
  activeTab = 'overview';

  isSaving = false;

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    private auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    // Check if we're in the admin area based on URL or Role
    const url = this.route.snapshot.url.map(s => s.path).join('/');
    this.isAdmin = url.includes('admin/') || this.auth.isAdmin();

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadProduct(id);
    }
  }

  loadProduct(id: string): void {
    this.productService.getBySlug(id).subscribe({
      next: (res: any) => {
        this.product = res;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  saveChanges(): void {
    if (!this.product || !this.product.productPublicId) return;
    this.isSaving = true;
    
    const payload = {
      productName: this.product.productName,
      description: this.product.description,
      price: this.product.price,
      discountPrice: this.product.discountPrice,
      categoryId: this.product.categoryId || 1, // Fallback if missing
      tags: this.product.tags || [],
      stockQuantity: 0 // Mocked since variants handle actual stock
    };

    this.productService.update(this.product.productPublicId, payload).subscribe({
      next: (res) => {
        this.toast.success('Product saved successfully');
        this.product = res; // Update local state with response
        this.isSaving = false;
      },
      error: (e) => {
        this.toast.error(e.error?.message || 'Failed to save product');
        this.isSaving = false;
      }
    });
  }
}
