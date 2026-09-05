import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ProductService } from '../../../services/product.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-seller-products',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="command-center">
      <div class="cc-header">
        <div class="header-left">
          <div class="greeting-block">
            <h1 style="display:flex; align-items:center; gap:12px;">
              <div class="kpi-icon kpi-icon-purple">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:24px;height:24px;color:var(--purple-400);"><path stroke-linecap="round" stroke-linejoin="round" d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z" /></svg>
              </div>
              My Catalog
            </h1>
            <p>Manage your products, inventory, and return policies.</p>
          </div>
        </div>
        <div class="header-right">
          <button class="btn btn-primary cc-btn" *ngIf="!showForm" (click)="startCreation()">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:8px;"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
            Add New Product
          </button>
          <button class="btn cc-btn" *ngIf="showForm" (click)="cancelCreation()" style="background:var(--bg-elevated); color:var(--text-primary); border:1px solid var(--border-default);">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:8px;"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            Discard Draft
          </button>
        </div>
      </div>

      <!-- PROGRESSIVE PRODUCT CREATION FORM -->
      <div class="product-creator-layout" *ngIf="showForm">
        <!-- Left: Stepper Navigation -->
        <div class="stepper-sidebar">
          <div class="stepper-item" *ngFor="let step of steps" [class.active]="currentStep === step.id" [class.completed]="currentStep > step.id" (click)="goToStep(step.id)">
            <div class="step-indicator">
              <svg *ngIf="currentStep > step.id" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
              <span *ngIf="currentStep <= step.id">{{step.id}}</span>
            </div>
            <div class="step-label">
              <div class="step-title">{{step.title}}</div>
              <div class="step-desc" *ngIf="currentStep === step.id">In progress</div>
            </div>
          </div>
        </div>

        <!-- Center: Dynamic Form Content -->
        <div class="form-content-area cc-panel">
          
          <!-- STEP 1: Basic Info -->
          <div class="step-content" *ngIf="currentStep === 1">
            <h2 class="step-header">Basic Information</h2>
            <div class="form-group">
              <label class="form-label">Product Title <span class="text-danger">*</span></label>
              <input type="text" [(ngModel)]="draft.name" class="form-input" placeholder="e.g. Sony WH-1000XM5 Wireless Headphones" maxlength="150" />
              <div class="form-hint">{{draft.name.length}}/150 characters</div>
            </div>
            <div class="form-group">
              <label class="form-label">Full Description <span class="text-danger">*</span></label>
              <textarea [(ngModel)]="draft.description" class="form-textarea" placeholder="Describe the key features, materials, and benefits..." rows="6"></textarea>
            </div>
            <div class="form-row form-row-2">
              <div class="form-group">
                <label class="form-label">Category <span class="text-danger">*</span></label>
                <select [(ngModel)]="draft.categoryPublicId" class="form-select">
                  <option value="">Select Category...</option>
                  <option *ngFor="let c of categories" [value]="c.publicId">{{c.displayName || c.name}}</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Brand</label>
                <input type="text" [(ngModel)]="draft.brand" class="form-input" placeholder="e.g. Sony, Apple, Nike" />
              </div>
            </div>
            <div class="form-row form-row-2">
              <div class="form-group">
                <label class="form-label">SKU (Stock Keeping Unit)</label>
                <input type="text" [(ngModel)]="draft.sku" class="form-input" placeholder="Leave empty to auto-generate" />
              </div>
              <div class="form-group">
                <label class="form-label">Condition</label>
                <select class="form-select">
                  <option value="NEW">New</option>
                  <option value="REFURBISHED">Refurbished</option>
                  <option value="USED">Used</option>
                </select>
              </div>
            </div>
          </div>

          <!-- STEP 2: Media -->
          <div class="step-content" *ngIf="currentStep === 2">
            <h2 class="step-header">Product Media</h2>
            <p style="color:var(--text-muted); font-size:14px; margin-bottom:20px;">Upload high-quality images. The first image will be your primary product image.</p>
            
            <div class="media-upload-zone">
              <input type="file" id="bulk-img-upload" (change)="onDraftImagesSelect($event)" accept="image/*" multiple style="display:none;" />
              <label for="bulk-img-upload" class="upload-label">
                <div class="upload-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
                </div>
                <div class="upload-text">Click to browse or drag & drop</div>
                <div class="upload-hint">PNG, JPG, WEBP up to 5MB</div>
              </label>
            </div>

            <div class="image-gallery" *ngIf="draft.imageFiles.length > 0">
              <div class="gallery-item" *ngFor="let file of draft.imageFiles; let i = index">
                <div class="img-preview-box">
                  <span class="primary-badge" *ngIf="i === 0">Primary</span>
                  <!-- We would ideally use FileReader to show a local preview here -->
                  <div class="img-placeholder">{{file.name}}</div>
                  <button class="btn-remove" (click)="removeDraftImage(i)">âœ&#8226;</button>
                </div>
              </div>
            </div>
          </div>

          <!-- STEP 3: Pricing -->
          <div class="step-content" *ngIf="currentStep === 3">
            <h2 class="step-header">Pricing</h2>
            <div class="form-row form-row-2">
              <div class="form-group">
                <label class="form-label">Base Price (MRP) (&#8377;) <span class="text-danger">*</span></label>
                <input type="number" [(ngModel)]="draft.basePrice" (input)="calculatePrice()" class="form-input" placeholder="0.00" min="0" />
              </div>
              <div class="form-group">
                <label class="form-label">Discount Percentage (%)</label>
                <input type="number" [(ngModel)]="draft.discountPercentage" (input)="calculatePrice()" class="form-input" placeholder="0" min="0" max="99" />
              </div>
            </div>
            
            <div class="pricing-summary-box">
              <div class="pricing-row">
                <span>Base Price:</span>
                <span>&#8377;{{draft.basePrice || 0}}</span>
              </div>
              <div class="pricing-row text-success">
                <span>Discount Applied:</span>
                <span>- &#8377;{{discountAmount | number:'1.2-2'}}</span>
              </div>
              <div class="pricing-divider"></div>
              <div class="pricing-row final">
                <span>Final Selling Price:</span>
                <span>&#8377;{{finalPrice | number:'1.2-2'}}</span>
              </div>
            </div>
          </div>

          <!-- STEP 4: Inventory -->
          <div class="step-content" *ngIf="currentStep === 4">
            <h2 class="step-header">Inventory</h2>
            <p style="color:var(--text-muted); font-size:14px; margin-bottom:20px;">If this product has variants (like Size/Color), you can override inventory per variant in the next step.</p>
            
            <div class="form-row form-row-2">
              <div class="form-group">
                <label class="form-label">Total Stock Quantity <span class="text-danger">*</span></label>
                <input type="number" [(ngModel)]="draft.stockQuantity" class="form-input" placeholder="0" min="0" />
              </div>
              <div class="form-group">
                <label class="form-label">Low Stock Threshold</label>
                <input type="number" [(ngModel)]="draft.lowStockThreshold" class="form-input" placeholder="5" min="0" />
                <div class="form-hint">You will be notified when stock falls below this.</div>
              </div>
            </div>
          </div>

          <!-- STEP 5: Variants -->
          <div class="step-content" *ngIf="currentStep === 5">
            <h2 class="step-header">Variants & Attributes</h2>
            <p style="color:var(--text-muted); font-size:14px; margin-bottom:20px;">Does this product come in different sizes, colors, or materials?</p>
            
            <button class="btn btn-outline" (click)="addVariant()" style="margin-bottom:20px;">+ Add Variant Option</button>
            
            <div class="variant-card" *ngFor="let v of draft.variants; let i = index">
              <div class="variant-card-header">
                <h4 style="margin:0; font-size:14px;">Variant #{{i+1}}</h4>
                <button class="btn-icon text-danger" (click)="removeVariant(i)">âœ&#8226;</button>
              </div>
              <div class="form-row form-row-3" style="margin-top:16px;">
                <div class="form-group">
                  <label class="form-label">SKU</label>
                  <input type="text" [(ngModel)]="v.sku" class="form-input" placeholder="e.g. BLK-M" />
                </div>
                <div class="form-group">
                  <label class="form-label">Stock Override</label>
                  <input type="number" [(ngModel)]="v.stock" class="form-input" placeholder="Leave empty for global" />
                </div>
                <div class="form-group">
                  <label class="form-label">Price Modifier (Â± &#8377;)</label>
                  <input type="number" [(ngModel)]="v.priceModifier" class="form-input" placeholder="e.g. 500" />
                </div>
              </div>
              <div class="form-group" style="margin-top:12px;">
                <label class="form-label">Attributes (e.g. Size: M, Color: Black)</label>
                <input type="text" [(ngModel)]="v.attributesStr" class="form-input" placeholder="Key:Value, Key:Value" />
              </div>
            </div>
            
            <div *ngIf="draft.variants.length === 0" class="empty-variants">
              This product will be created as a Simple Product without variants.
            </div>
          </div>

          <!-- STEP 6: Shipping -->
          <div class="step-content" *ngIf="currentStep === 6">
            <h2 class="step-header">Shipping & Logistics</h2>
            <p style="color:var(--text-muted); font-size:14px; margin-bottom:20px;">Required for accurate volumetric freight calculations.</p>
            
            <div class="form-group">
              <label class="form-label">Package Weight (kg)</label>
              <input type="number" [(ngModel)]="draft.weight" class="form-input" placeholder="0.00" min="0" step="0.01" />
            </div>
            <div class="form-row form-row-3">
              <div class="form-group">
                <label class="form-label">Length (cm)</label>
                <input type="number" [(ngModel)]="draft.lengthCm" class="form-input" placeholder="0" min="0" />
              </div>
              <div class="form-group">
                <label class="form-label">Width (cm)</label>
                <input type="number" [(ngModel)]="draft.widthCm" class="form-input" placeholder="0" min="0" />
              </div>
              <div class="form-group">
                <label class="form-label">Height (cm)</label>
                <input type="number" [(ngModel)]="draft.heightCm" class="form-input" placeholder="0" min="0" />
              </div>
            </div>
          </div>

          <!-- STEP 7: Returns & Warranty -->
          <div class="step-content" *ngIf="currentStep === 7">
            <h2 class="step-header">Returns & Warranty</h2>
            
            <div class="form-group">
              <label class="form-label">Return Policy <span class="text-danger">*</span></label>
              <select [(ngModel)]="draft.policyType" class="form-select">
                <option value="NON_RETURNABLE">Non-Returnable</option>
                <option value="RETURN_ONLY">Return Only</option>
                <option value="EXCHANGE_ONLY">Exchange Only</option>
                <option value="RETURN_AND_EXCHANGE">Return & Exchange</option>
                <option value="REPLACEMENT_ONLY">Replacement Only</option>
              </select>
            </div>
            
            <div class="form-group" *ngIf="draft.policyType !== 'NON_RETURNABLE'">
              <label class="form-label">Return Window (Days) <span class="text-danger">*</span></label>
              <input type="number" [(ngModel)]="draft.returnWindowDays" class="form-input" placeholder="e.g. 7" min="1" max="30" />
              <div class="form-hint">Number of days after delivery the customer can initiate a return.</div>
            </div>
          </div>

          <!-- STEP 8: SEO & Visibility -->
          <div class="step-content" *ngIf="currentStep === 8">
            <h2 class="step-header">SEO & Visibility</h2>
            
            <div class="form-group">
              <label class="form-label">URL Slug</label>
              <input type="text" [(ngModel)]="draft.seoSlug" class="form-input" placeholder="Will be auto-generated from title if left blank" />
              <div class="form-hint">cognitocart.vercel.app/products/<strong>{{draft.seoSlug || 'your-product-name'}}</strong></div>
            </div>
          </div>

          <!-- STEP 9: Review & Publish -->
          <div class="step-content" *ngIf="currentStep === 9">
            <h2 class="step-header">Review & Publish</h2>
            
            <div class="review-block">
              <div class="review-item">
                <span class="review-label">Product Name</span>
                <span class="review-val" [class.text-danger]="!draft.name">{{draft.name || 'Missing!'}}</span>
              </div>
              <div class="review-item">
                <span class="review-label">Category</span>
                <span class="review-val" [class.text-danger]="!draft.categoryPublicId">{{draft.categoryPublicId ? 'Selected' : 'Missing!'}}</span>
              </div>
              <div class="review-item">
                <span class="review-label">Selling Price</span>
                <span class="review-val" [class.text-danger]="finalPrice <= 0">{{finalPrice > 0 ? '&#8377;' + finalPrice : 'Invalid!'}}</span>
              </div>
              <div class="review-item">
                <span class="review-label">Stock Quantity</span>
                <span class="review-val" [class.text-danger]="draft.stockQuantity === null || draft.stockQuantity < 0">{{draft.stockQuantity}}</span>
              </div>
              <div class="review-item">
                <span class="review-label">Images</span>
                <span class="review-val">{{draft.imageFiles.length}} selected</span>
              </div>
            </div>

            <div class="alert alert-warning" *ngIf="!isValidToPublish()">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:8px; flex-shrink:0;"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
              Please fix the missing required fields before publishing. (Name, Category, Stock, Base Price)
            </div>

            <div style="margin-top: 32px; display:flex; gap:16px;">
              <button class="btn btn-outline" style="flex:1;" (click)="saveDraft()" [disabled]="processing">
                Save as Draft
              </button>
              <button class="btn btn-primary" style="flex:2; font-weight:600;" (click)="publishProduct()" [disabled]="!isValidToPublish() || processing">
                {{processing ? 'Publishing...' : 'Publish Product to Catalog'}}
              </button>
            </div>
          </div>

          <!-- Bottom Navigation Bar -->
          <div class="stepper-actions">
            <button class="btn btn-outline" (click)="prevStep()" [disabled]="currentStep === 1 || processing">Back</button>
            <button class="btn btn-primary" (click)="nextStep()" *ngIf="currentStep < 9">Next Step</button>
          </div>
        </div>

        <!-- Right: Live Preview Summary -->
        <div class="preview-sidebar">
          <div class="cc-panel preview-panel">
            <h3 style="font-size:14px; margin-top:0; color:var(--text-muted); text-transform:uppercase; letter-spacing:1px; margin-bottom:16px;">Live Preview</h3>
            
            <div class="preview-img-box">
              <svg *ngIf="draft.imageFiles.length === 0" xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="var(--border-strong)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
              <div *ngIf="draft.imageFiles.length > 0" style="color:var(--text-muted); font-size:12px;">Image Selected</div>
            </div>
            
            <h4 class="preview-title">{{draft.name || 'Product Title'}}</h4>
            
            <div class="preview-price">
              <span class="final">&#8377;{{finalPrice | number:'1.0-0'}}</span>
              <span class="mrp" *ngIf="draft.discountPercentage > 0">&#8377;{{draft.basePrice}}</span>
              <span class="discount" *ngIf="draft.discountPercentage > 0">{{draft.discountPercentage}}% OFF</span>
            </div>
            
            <div class="preview-stock" [class.out]="draft.stockQuantity === 0">
              {{draft.stockQuantity > 0 ? 'In Stock' : 'Out of Stock'}}
            </div>
          </div>
        </div>
      </div>

      <!-- EXISTING PRODUCTS LIST (When form is closed) -->
      <div *ngIf="!showForm">
        <div class="loading-center" *ngIf="loading" style="min-height: 200px; display:flex; align-items:center; justify-content:center;">
          <div class="spinner"></div>
        </div>

        <div class="cc-panel" *ngIf="!loading">
          <div class="panel-header border-bottom">
            <h2>
              <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width:20px;height:20px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="m20.25 7.5-.625 10.632a2.25 2.25 0 0 1-2.247 2.118H6.622a2.25 2.25 0 0 1-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125Z" /></svg>
              All Products
              <span class="badge badge-purple" style="margin-left: 8px;">{{products.length}} Products</span>
            </h2>
          </div>
          
          <div class="cc-table-wrapper" *ngIf="products.length > 0">
            <table class="cc-table">
              <thead>
                <tr>
                  <th>Product Details</th>
                  <th>Price & Discount</th>
                  <th>Rating</th>
                  <th>Status</th>
                  <th class="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let p of products" [routerLink]="['/seller/products', p.slug || p.publicId || p.productPublicId]" style="cursor: pointer;">
                  <td>
                    <div style="display:flex; align-items:center; gap: 16px;">
                      <img [src]="p.mediaGallery?.[0]?.mediaUrl || 'assets/placeholder-product.png'" style="width:48px; height:48px; object-fit:cover; border-radius:8px; border:1px solid var(--border-default); background: var(--glass-sm);" onerror="this.src='https://placehold.co/100x100/1e293b/a5b4fc?text=Img'" />
                      <div>
                        <div style="font-weight:600; color:var(--text-primary); font-size:14px; margin-bottom:4px;">{{p.productName || p.name}}</div>
                        <div style="display:flex; gap:8px; align-items:center;">
                          <span class="badge badge-gray" style="font-size:10px;">{{p.category?.name || p.categoryName || 'Uncategorized'}}</span>
                          <span *ngIf="p.productCode" style="font-size:11px; color:var(--text-dim); font-family:var(--font-mono);">{{p.productCode}}</span>
                        </div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div style="font-weight:700; color:var(--text-primary); font-size:14px;">&#8377;{{(p.price || p.basePrice) | number:'1.0-0'}}</div>
                    <div *ngIf="p.discountPercentage > 0" style="font-size:12px; color:var(--success); font-weight:600; margin-top:4px; display:flex; align-items:center; gap:4px;">
                      <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width:12px;height:12px;"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z" /></svg>
                      {{p.discountPercentage}}% OFF
                    </div>
                  </td>
                  <td>
                    <div *ngIf="p.averageRating" style="display:flex; align-items:center; gap:4px;">
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="var(--warning)" stroke="var(--warning)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                      <span style="color:var(--text-primary); font-weight:600;">{{p.averageRating | number:'1.1-1'}}</span>
                      <span style="color:var(--text-muted); font-size:12px;">({{p.totalReviews || 0}})</span>
                    </div>
                    <span *ngIf="!p.averageRating" style="color:var(--text-dim); font-size:13px; font-style:italic;">No reviews</span>
                  </td>
                  <td>
                    <span class="badge" [class.badge-green]="p.isActive || p.isAvailable" [class.badge-red]="!p.isActive && !p.isAvailable">
                      {{p.isActive || p.isAvailable ? 'Active' : (p.isActive === false ? 'Hidden' : 'Draft')}}
                    </span>
                  </td>
                  <td>
                    <div style="display:flex; gap:8px; justify-content:flex-end;">
                      <button class="btn btn-icon-sm btn-ghost" (click)="toggle(p); $event.stopPropagation()" [title]="p.isActive || p.isAvailable ? 'Hide product' : 'Show product'">
                        <svg *ngIf="p.isActive || p.isAvailable" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
                        <svg *ngIf="!p.isActive && !p.isAvailable" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                      </button>
                      <button class="btn btn-icon-sm btn-ghost" style="color:var(--danger); background:rgba(244,63,94,0.1);" (click)="delete(p); $event.stopPropagation()" title="Delete product">
                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="cc-empty-state" *ngIf="products.length === 0" style="margin: 24px; border: none; background: transparent;">
            <div class="empty-icon-ring" style="background: rgba(255,255,255,0.05); border-color: rgba(255,255,255,0.1); color: var(--text-muted);">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007ZM8.625 10.5a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm7.5 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" /></svg>
            </div>
            <h3>No Products Found</h3>
            <p>You haven't added any products to your catalog yet.</p>
            <button class="btn btn-primary" style="margin-top: 16px;" (click)="startCreation()">Add Your First Product</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .product-creator-layout {
      display: flex;
      gap: 24px;
      margin-bottom: 32px;
      align-items: flex-start;
    }
    .stepper-sidebar {
      width: 240px;
      flex-shrink: 0;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .form-content-area {
      flex-grow: 1;
      min-width: 0; /* prevent overflow */
      padding: 32px;
      display: flex;
      flex-direction: column;
      min-height: 500px;
    }
    .preview-sidebar {
      width: 280px;
      flex-shrink: 0;
    }
    
    @media (max-width: 992px) {
      .product-creator-layout {
        flex-direction: column;
      }
      .stepper-sidebar {
        width: 100%;
        flex-direction: row;
        overflow-x: auto;
        padding-bottom: 8px;
      }
      .stepper-item {
        flex-shrink: 0;
      }
      .preview-sidebar {
        width: 100%;
      }
    }

    .stepper-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      border-radius: 12px;
      cursor: pointer;
      transition: all 0.2s;
      border: 1px solid transparent;
    }
    .stepper-item:hover {
      background: rgba(255,255,255,0.02);
    }
    .stepper-item.active {
      background: rgba(99,102,241,0.1);
      border-color: rgba(99,102,241,0.2);
    }
    .step-indicator {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: var(--bg-elevated);
      border: 2px solid var(--border-default);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 600;
      color: var(--text-muted);
      transition: all 0.2s;
    }
    .stepper-item.active .step-indicator {
      border-color: var(--primary);
      color: var(--primary);
    }
    .stepper-item.completed .step-indicator {
      background: var(--primary);
      border-color: var(--primary);
      color: #fff;
    }
    .step-label {
      display: flex;
      flex-direction: column;
    }
    .step-title {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-muted);
    }
    .stepper-item.active .step-title, .stepper-item.completed .step-title {
      color: var(--text-primary);
    }
    .step-desc {
      font-size: 11px;
      color: var(--primary);
      margin-top: 2px;
    }

    .step-header {
      font-size: 20px;
      margin-top: 0;
      margin-bottom: 24px;
      padding-bottom: 16px;
      border-bottom: 1px solid var(--border-subtle);
    }
    .stepper-actions {
      margin-top: auto;
      padding-top: 32px;
      display: flex;
      justify-content: space-between;
      border-top: 1px solid var(--border-subtle);
    }

    /* Media Upload */
    .media-upload-zone {
      border: 2px dashed rgba(99,102,241,0.3);
      border-radius: 12px;
      padding: 48px 32px;
      text-align: center;
      background: rgba(99,102,241,0.03);
      transition: all 0.2s;
    }
    .media-upload-zone:hover {
      background: rgba(99,102,241,0.08);
      border-color: rgba(99,102,241,0.5);
    }
    .upload-icon {
      width: 56px;
      height: 56px;
      background: rgba(99,102,241,0.1);
      color: var(--primary);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 16px auto;
    }
    .upload-text {
      color: var(--text-primary);
      font-weight: 600;
      font-size: 16px;
    }
    .upload-hint {
      color: var(--text-dim);
      font-size: 13px;
      margin-top: 8px;
    }

    .image-gallery {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
      gap: 16px;
      margin-top: 24px;
    }
    .img-preview-box {
      width: 100px;
      height: 100px;
      border-radius: 8px;
      background: var(--bg-deep);
      border: 1px solid var(--border-default);
      position: relative;
      overflow: hidden;
    }
    .img-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 10px;
      color: var(--text-dim);
      padding: 8px;
      text-align: center;
      word-break: break-all;
    }
    .primary-badge {
      position: absolute;
      top: 4px; left: 4px;
      background: var(--primary);
      color: #fff;
      font-size: 9px;
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: bold;
    }
    .btn-remove {
      position: absolute;
      top: 4px; right: 4px;
      background: rgba(0,0,0,0.6);
      color: #fff;
      border: none;
      border-radius: 50%;
      width: 20px; height: 20px;
      font-size: 10px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .btn-remove:hover {
      background: var(--danger);
    }

    /* Pricing Summary */
    .pricing-summary-box {
      background: rgba(0,0,0,0.1);
      border: 1px solid var(--border-subtle);
      border-radius: 12px;
      padding: 20px;
      margin-top: 24px;
    }
    .pricing-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 12px;
      font-size: 14px;
      color: var(--text-muted);
    }
    .pricing-row.final {
      font-size: 18px;
      font-weight: 700;
      color: var(--text-primary);
      margin-bottom: 0;
    }
    .pricing-divider {
      height: 1px;
      background: var(--border-subtle);
      margin: 16px 0;
    }

    /* Variant Cards */
    .variant-card {
      background: rgba(255,255,255,0.02);
      border: 1px solid var(--border-default);
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 16px;
    }
    .variant-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--border-subtle);
      padding-bottom: 12px;
    }
    .empty-variants {
      text-align: center;
      padding: 32px;
      color: var(--text-muted);
      border: 1px dashed var(--border-strong);
      border-radius: 12px;
      background: rgba(0,0,0,0.1);
    }

    /* Review Block */
    .review-block {
      background: var(--bg-deep);
      border: 1px solid var(--border-subtle);
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 24px;
    }
    .review-item {
      display: flex;
      justify-content: space-between;
      padding: 12px 0;
      border-bottom: 1px solid var(--border-subtle);
    }
    .review-item:last-child {
      border-bottom: none;
    }
    .review-label {
      color: var(--text-muted);
      font-size: 14px;
    }
    .review-val {
      font-weight: 600;
      color: var(--text-primary);
      font-size: 14px;
    }

    /* Live Preview Panel */
    .preview-panel {
      position: sticky;
      top: 24px;
      padding: 24px;
    }
    .preview-img-box {
      width: 100%;
      aspect-ratio: 1;
      background: var(--bg-deep);
      border-radius: 12px;
      border: 1px solid var(--border-default);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      margin-bottom: 16px;
    }
    .preview-title {
      font-size: 16px;
      margin: 0 0 12px 0;
      color: var(--text-primary);
      line-height: 1.4;
    }
    .preview-price {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;
    }
    .preview-price .final {
      font-size: 20px;
      font-weight: 700;
      color: var(--text-primary);
    }
    .preview-price .mrp {
      font-size: 14px;
      color: var(--text-muted);
      text-decoration: line-through;
    }
    .preview-price .discount {
      font-size: 12px;
      font-weight: 600;
      color: var(--success);
      background: rgba(16,185,129,0.1);
      padding: 2px 6px;
      border-radius: 4px;
    }
    .preview-stock {
      font-size: 13px;
      font-weight: 600;
      color: var(--success);
      display: inline-block;
      padding: 4px 8px;
      background: rgba(16,185,129,0.1);
      border-radius: 4px;
    }
    .preview-stock.out {
      color: var(--danger);
      background: rgba(244,63,94,0.1);
    }
  `]
})
export class SellerProductsComponent implements OnInit {
  products: any[] = [];
  categories: any[] = [];
  loading = true;
  
  // State
  showForm = false;
  processing = false;
  currentStep = 1;
  steps = [
    { id: 1, title: 'Basic Info' },
    { id: 2, title: 'Media' },
    { id: 3, title: 'Pricing' },
    { id: 4, title: 'Inventory' },
    { id: 5, title: 'Variants' },
    { id: 6, title: 'Shipping' },
    { id: 7, title: 'Returns' },
    { id: 8, title: 'SEO' },
    { id: 9, title: 'Publish' }
  ];

  draft: any = this.getEmptyDraft();
  discountAmount = 0;
  finalPrice = 0;

  constructor(private productService: ProductService, private toast: ToastService) {}

  ngOnInit(): void {
    this.fetchProducts();
    this.productService.getCategories().subscribe({ 
      next: c => { this.categories = this.flattenCategories(c); }, 
      error: () => {} 
    });
  }

  fetchProducts(): void {
    this.productService.getAll(0, 100).subscribe({
      next: (res) => { this.products = res.content ?? res ?? []; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  getEmptyDraft() {
    return {
      name: '', description: '', categoryPublicId: '', brand: '', sku: '',
      imageFiles: [] as File[],
      basePrice: null, discountPercentage: 0,
      stockQuantity: null, lowStockThreshold: 5,
      variants: [],
      weight: null, lengthCm: null, widthCm: null, heightCm: null,
      policyType: 'NON_RETURNABLE', returnWindowDays: 7,
      seoSlug: ''
    };
  }

  flattenCategories(categories: any[], prefix: string = '', flatList: any[] = []): any[] {
    for (const c of categories) {
      flatList.push({ ...c, displayName: prefix + c.name });
      if (c.subCategories && c.subCategories.length > 0) {
        this.flattenCategories(c.subCategories, prefix + c.name + ' > ', flatList);
      }
    }
    return flatList;
  }

  // --- UI Navigation ---
  startCreation() {
    this.draft = this.getEmptyDraft();
    this.currentStep = 1;
    this.showForm = true;
    this.calculatePrice();
  }

  cancelCreation() {
    if (confirm('Discard this draft? All unsaved changes will be lost.')) {
      this.showForm = false;
    }
  }

  goToStep(step: number) {
    // Only allow skipping forward if valid, but let them go backward freely
    if (step < this.currentStep) {
      this.currentStep = step;
    } else {
      // Very basic validation blocker
      if (this.currentStep === 1 && (!this.draft.name || !this.draft.categoryPublicId)) {
        this.toast.warning('Please fill required basic info first.');
        return;
      }
      this.currentStep = step;
    }
  }

  nextStep() {
    if (this.currentStep < 9) this.goToStep(this.currentStep + 1);
  }

  prevStep() {
    if (this.currentStep > 1) this.currentStep--;
  }

  // --- Interactions ---
  calculatePrice() {
    const base = this.draft.basePrice || 0;
    const dp = this.draft.discountPercentage || 0;
    this.discountAmount = base * (dp / 100);
    this.finalPrice = base - this.discountAmount;
  }

  onDraftImagesSelect(event: any) {
    const files = event.target.files;
    if (files) {
      for (let i = 0; i < files.length; i++) {
        this.draft.imageFiles.push(files[i]);
      }
    }
  }

  removeDraftImage(index: number) {
    this.draft.imageFiles.splice(index, 1);
  }

  addVariant() {
    this.draft.variants.push({
      sku: '', stock: null, priceModifier: 0, attributesStr: ''
    });
  }

  removeVariant(index: number) {
    this.draft.variants.splice(index, 1);
  }

  isValidToPublish(): boolean {
    if (!this.draft.name || !this.draft.categoryPublicId) return false;
    if (this.finalPrice < 0) return false;
    if (this.draft.stockQuantity === null || this.draft.stockQuantity < 0) return false;
    return true;
  }

  // --- API Integration (Progressive Save) ---
  
  async saveDraft() {
    // Same as publish, but could pass an internal flag to keep isAvailable = false
    this.toast.info('Draft functionality requires backend Draft State implementation.');
    // In actual implementation, we would hit POST /api/products, get ID, then stop.
  }

  async publishProduct() {
    if (!this.isValidToPublish()) return;
    this.processing = true;

    try {
      const variantsList = [];
      if (this.draft.variants && this.draft.variants.length > 0) {
        const firstVariant = this.draft.variants[0];
        const attrs: any = {};
        if (firstVariant.attributesStr) {
          firstVariant.attributesStr.split(',').forEach((pair: string) => {
            const [k, val] = pair.split(':');
            if (k && val) attrs[k.trim()] = val.trim();
          });
        }
        variantsList.push({
          sku: firstVariant.sku || ('VAR-' + Math.random().toString(36).substring(7)),
          stockQuantity: firstVariant.stock !== null ? firstVariant.stock : this.draft.stockQuantity,
          priceModifier: firstVariant.priceModifier || 0,
          attributes: attrs,
          weight: this.draft.weight || 0,
          lengthCm: this.draft.lengthCm || 0,
          widthCm: this.draft.widthCm || 0,
          heightCm: this.draft.heightCm || 0
        });
      } else {
        variantsList.push({
          sku: this.draft.sku || ('STD-' + Math.random().toString(36).substring(7)),
          stockQuantity: this.draft.stockQuantity,
          priceModifier: 0,
          attributes: { "Type": "Standard" },
          weight: this.draft.weight || 0,
          lengthCm: this.draft.lengthCm || 0,
          widthCm: this.draft.widthCm || 0,
          heightCm: this.draft.heightCm || 0
        });
      }

      const productPayload = {
        productName: this.draft.name,
        description: this.draft.description,
        price: this.draft.basePrice,
        discountPrice: this.finalPrice,
        stockQuantity: this.draft.stockQuantity,
        categoryPublicId: this.draft.categoryPublicId || null,
        brand: this.draft.brand,
        countryOfOrigin: 'India',
        condition: 'NEW',
        productType: 'PHYSICAL',
        isDraft: true,
        variants: variantsList
      };

      // We await promises for sequential orchestration
      const productRes = await this.productService.create(productPayload).toPromise();
      const pId = productRes.productPublicId || productRes.publicId;

      // 2. Set Return Policy
      const policyPayload = {
        productPublicId: pId,
        policyType: this.draft.policyType,
        returnWindowDays: this.draft.policyType === 'NON_RETURNABLE' ? 0 : this.draft.returnWindowDays,
        returnAllowed: ['RETURN_ONLY', 'RETURN_AND_EXCHANGE'].includes(this.draft.policyType),
        exchangeAllowed: ['EXCHANGE_ONLY', 'RETURN_AND_EXCHANGE'].includes(this.draft.policyType),
        replacementAllowed: this.draft.policyType === 'REPLACEMENT_ONLY',
        pickupAvailable: true
      };
      await this.productService.createReturnPolicy(policyPayload).toPromise().catch(e => console.warn('Policy setup err', e));

      // 3. Upload Images Sequentially
      for (const file of this.draft.imageFiles) {
        await this.productService.uploadImage(pId, file).toPromise().catch(e => console.warn('Img err', e));
      }

      // 4. Create remaining Variants (if any)
      if (this.draft.variants && this.draft.variants.length > 1) {
        for (let i = 1; i < this.draft.variants.length; i++) {
          const v = this.draft.variants[i];
          // Parse "Size:M, Color:Red" into JSON object
          const attrs: any = {};
          if (v.attributesStr) {
            v.attributesStr.split(',').forEach((pair: string) => {
              const [k, val] = pair.split(':');
              if (k && val) attrs[k.trim()] = val.trim();
            });
          }
          
          const variantPayload = {
            sku: v.sku || (productRes.productCode + '-' + Math.random().toString(36).substring(7)),
            attributes: attrs,
            stockQuantity: v.stock !== null ? v.stock : this.draft.stockQuantity,
            priceModifier: v.priceModifier || 0,
            weight: this.draft.weight || 0,
            lengthCm: this.draft.lengthCm || 0,
            widthCm: this.draft.widthCm || 0,
            heightCm: this.draft.heightCm || 0
          };
          await this.productService.createVariant(pId, variantPayload).toPromise().catch(e => console.warn('Variant err', e));
        }
      }

      // 5. Submit for Review
      // Now that all images and variants are attached to the draft, we submit it
      await this.productService.submitForReview(pId).toPromise();

      this.toast.success('Product submitted for review successfully!');
      this.fetchProducts(); // Refresh list
      this.showForm = false;
    } catch (e: any) {
      this.toast.error(e.error?.message || 'Failed to publish product');
    } finally {
      this.processing = false;
    }
  }

  // --- List Actions ---
  toggle(product: any): void {
    const id = product.productPublicId || product.publicId;
    this.productService.toggle(id).subscribe({
      next: () => { product.isActive = !product.isActive; this.toast.success('Visibility updated'); },
      error: () => this.toast.error('Failed to toggle')
    });
  }

  delete(product: any): void {
    if (!confirm(`Delete "${product.name || product.productName}"? This cannot be undone.`)) return;
    const id = product.productPublicId || product.publicId;
    this.productService.delete(id).subscribe({
      next: () => { this.products = this.products.filter(p => p !== product); this.toast.success('Product deleted'); },
      error: (e) => this.toast.error(e.error?.message || 'Failed to delete')
    });
  }
}

