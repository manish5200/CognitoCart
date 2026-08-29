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
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:24px;height:24px;color:var(--purple-400);"><path stroke-linecap="round" stroke-linejoin="round" d="m20.25 7.5-.625 10.632a2.25 2.25 0 0 1-2.247 2.118H6.622a2.25 2.25 0 0 1-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125Z" /></svg>
              </div>
              My Catalog
            </h1>
            <p>Manage your products, inventory, and return policies.</p>
          </div>
        </div>
        <div class="header-right">
          <button class="btn btn-primary cc-btn" (click)="showForm = !showForm">
            <svg *ngIf="!showForm" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:8px;"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
            <svg *ngIf="showForm" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:8px;"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            {{showForm ? 'Cancel Creation' : 'Add New Product'}}
          </button>
        </div>
      </div>

      <!-- Add Product Form -->
      <div class="cc-panel" *ngIf="showForm" style="margin-bottom:32px;">
        <div class="panel-header border-bottom">
          <h2>
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
            Product Details
          </h2>
        </div>
        <div class="panel-body" style="padding-top: 24px;">
          <div class="form-row form-row-2">
            <div class="form-group">
              <label class="form-label">Product Name <span class="text-danger">*</span></label>
              <input type="text" [(ngModel)]="newProduct.name" class="form-input" placeholder="e.g. Wireless Earphones Pro" />
            </div>
            <div class="form-group">
              <label class="form-label">Category <span class="text-danger">*</span></label>
              <select [(ngModel)]="newProduct.categoryPublicId" class="form-select">
                <option value="">Select a category...</option>
                <option *ngFor="let c of categories" [value]="c.publicId">{{c.name}}</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Description</label>
            <textarea [(ngModel)]="newProduct.description" class="form-textarea" placeholder="Provide a detailed product description..." rows="3"></textarea>
          </div>
          <div class="form-row form-row-3">
            <div class="form-group">
              <label class="form-label">Base Price (₹) <span class="text-danger">*</span></label>
              <input type="number" [(ngModel)]="newProduct.basePrice" class="form-input" placeholder="0.00" />
            </div>
            <div class="form-group">
              <label class="form-label">Discount %</label>
              <input type="number" [(ngModel)]="newProduct.discountPercentage" class="form-input" placeholder="0" min="0" max="90" />
            </div>
            <div class="form-group">
              <label class="form-label">Brand</label>
              <input type="text" [(ngModel)]="newProduct.brand" class="form-input" placeholder="e.g. Apple, Sony..." />
            </div>
          </div>
          <div class="form-row form-row-2" style="margin-top: 16px;">
            <div class="form-group">
              <label class="form-label">Return Policy <span class="text-danger">*</span></label>
              <select [(ngModel)]="newProduct.policyType" class="form-select">
                <option value="NON_RETURNABLE">Non-Returnable</option>
                <option value="RETURN_ONLY">Return Only</option>
                <option value="EXCHANGE_ONLY">Exchange Only</option>
                <option value="RETURN_AND_EXCHANGE">Return & Exchange</option>
                <option value="REPLACEMENT_ONLY">Replacement Only</option>
              </select>
            </div>
            <div class="form-group" *ngIf="newProduct.policyType !== 'NON_RETURNABLE'">
              <label class="form-label">Return Window (Days) <span class="text-danger">*</span></label>
              <input type="number" [(ngModel)]="newProduct.returnWindowDays" class="form-input" placeholder="e.g. 7" min="0" max="30" />
            </div>
          </div>
          <div style="margin-top: 24px; display:flex; justify-content:flex-end;">
            <button class="btn btn-primary" (click)="createProduct()" [disabled]="creating" style="padding: 10px 24px; font-weight: 600;">
              <svg *ngIf="!creating" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:8px;"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path><polyline points="17 21 17 13 7 13 7 21"></polyline><polyline points="7 3 7 8 15 8"></polyline></svg>
              {{creating ? 'Creating...' : 'Save Product'}}
            </button>
          </div>
        </div>
      </div>

      <!-- Products List -->
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
                    <img [src]="p.imageUrls?.[0] || 'assets/placeholder-product.png'" style="width:48px; height:48px; object-fit:cover; border-radius:8px; border:1px solid var(--border-default); background: var(--glass-sm);" onerror="this.src='https://placehold.co/100x100/1e293b/a5b4fc?text=Img'" />
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
                  <div style="font-weight:700; color:var(--text-primary); font-size:14px;">₹{{(p.price || p.basePrice) | number:'1.0-0'}}</div>
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
                  <span class="badge" [class.badge-green]="p.isActive" [class.badge-red]="!p.isActive">
                    {{p.isActive ? 'Active' : 'Hidden'}}
                  </span>
                </td>
                <td>
                  <div style="display:flex; gap:8px; justify-content:flex-end;">
                    <button class="btn btn-icon-sm btn-ghost" (click)="toggle(p); $event.stopPropagation()" [title]="p.isActive ? 'Hide product' : 'Show product'">
                      <svg *ngIf="p.isActive" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>
                      <svg *ngIf="!p.isActive" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                    </button>
                    <button class="btn btn-icon-sm btn-ghost" style="color:var(--primary); background:rgba(99,102,241,0.1);" (click)="uploadImg(p); $event.stopPropagation()" title="Upload Image">
                      <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
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
          <button class="btn btn-primary" style="margin-top: 16px;" (click)="showForm = true">Add Your First Product</button>
        </div>
      </div>

      <!-- Image Upload Modal -->
      <div class="modal-backdrop" *ngIf="uploadingFor" (click)="uploadingFor = null">
        <div class="modal" (click)="$event.stopPropagation()" style="background: var(--bg-elevated); border: 1px solid var(--border-default); border-radius: var(--r-xl); box-shadow: var(--shadow-xl); overflow:hidden;">
          <div class="modal-header" style="background: rgba(0,0,0,0.2); padding: 20px 24px; border-bottom: 1px solid var(--border-subtle);">
            <span class="modal-title" style="font-size:16px; font-weight:600; color:var(--text-primary); display:flex; align-items:center; gap:8px;">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
              Upload Image
            </span>
            <button class="btn-icon" (click)="uploadingFor = null">✕</button>
          </div>
          <div class="modal-body" style="padding: 24px;">
            <div style="font-size:14px; color:var(--text-muted); margin-bottom:16px;">
              Uploading image for <strong style="color:var(--text-primary);">{{uploadingFor.productName || uploadingFor.name}}</strong>
            </div>
            
            <div style="border: 2px dashed rgba(99,102,241,0.3); border-radius: 12px; padding: 32px; text-align:center; background: rgba(99,102,241,0.05); margin-bottom:24px; transition: all 0.2s;" onmouseover="this.style.background='rgba(99,102,241,0.1)'" onmouseout="this.style.background='rgba(99,102,241,0.05)'">
              <input type="file" id="img-upload" (change)="onImgSelect($event)" accept="image/*" style="display:none;" />
              <label for="img-upload" style="cursor:pointer; display:flex; flex-direction:column; align-items:center; gap:12px;">
                <div style="width:48px; height:48px; background:rgba(99,102,241,0.1); border-radius:50%; display:flex; align-items:center; justify-content:center; color:var(--primary);">
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
                </div>
                <div>
                  <span style="color:var(--primary); font-weight:600;">Click to browse</span>
                  <span style="color:var(--text-muted); font-size:14px;"> or drag and drop</span>
                </div>
                <div style="font-size:12px; color:var(--text-dim);">PNG, JPG, WEBP up to 5MB</div>
              </label>
              <div *ngIf="imgFile" style="margin-top:16px; padding:12px; background:rgba(0,0,0,0.2); border-radius:8px; font-size:13px; color:var(--success); display:flex; align-items:center; justify-content:center; gap:8px;">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                {{imgFile.name}}
              </div>
            </div>
            
            <button class="btn btn-primary" (click)="submitImage()" [disabled]="!imgFile || uploading" style="width:100%; padding:12px; font-weight:600;">
              {{uploading ? 'Uploading...' : 'Upload Image'}}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class SellerProductsComponent implements OnInit {
  products: any[] = [];
  categories: any[] = [];
  loading = true;
  showForm = false;
  creating = false;
  uploadingFor: any = null;
  imgFile: File | null = null;
  uploading = false;
  newProduct: any = { name: '', description: '', basePrice: 0, discountPercentage: 0, brand: '', categoryPublicId: '', policyType: 'NON_RETURNABLE', returnWindowDays: 7 };

  constructor(private productService: ProductService, private toast: ToastService) {}

  ngOnInit(): void {
    this.productService.getAll(0, 100).subscribe({
      next: (res) => { this.products = res.content ?? res ?? []; this.loading = false; },
      error: () => { this.loading = false; }
    });
    this.productService.getCategories().subscribe({ next: c => this.categories = c, error: () => {} });
  }

  createProduct(): void {
    if (!this.newProduct.name || !this.newProduct.basePrice || !this.newProduct.categoryPublicId) { 
      this.toast.warning('Name, price, and category are required'); 
      return; 
    }
    
    // Find the category to get its internal ID
    const category = this.categories.find(c => c.publicId === this.newProduct.categoryPublicId);
    
    const payload = {
      productName: this.newProduct.name,
      description: this.newProduct.description,
      price: this.newProduct.basePrice,
      discountPrice: this.newProduct.basePrice - (this.newProduct.basePrice * (this.newProduct.discountPercentage / 100)),
      stockQuantity: 10, // Default for now
      categoryId: category ? category.id : null,
      brand: this.newProduct.brand
    };

    this.creating = true;
    this.productService.create(payload).subscribe({
      next: (p) => {
        // Now create the return policy
        const policyPayload = {
          productPublicId: p.productPublicId || p.publicId,
          policyType: this.newProduct.policyType,
          returnWindowDays: this.newProduct.policyType === 'NON_RETURNABLE' ? 0 : this.newProduct.returnWindowDays,
          returnAllowed: ['RETURN_ONLY', 'RETURN_AND_EXCHANGE'].includes(this.newProduct.policyType),
          exchangeAllowed: ['EXCHANGE_ONLY', 'RETURN_AND_EXCHANGE'].includes(this.newProduct.policyType),
          replacementAllowed: this.newProduct.policyType === 'REPLACEMENT_ONLY',
          pickupAvailable: true // Default for now
        };

        this.productService.createReturnPolicy(policyPayload).subscribe({
          next: () => {
            this.toast.success('Product & Return Policy created!');
            this.products.unshift(p);
            this.creating = false;
            this.showForm = false;
            this.newProduct = { name: '', description: '', basePrice: 0, discountPercentage: 0, brand: '', categoryPublicId: '', policyType: 'NON_RETURNABLE', returnWindowDays: 7 };
          },
          error: () => {
            this.toast.warning('Product created, but failed to set return policy.');
            this.products.unshift(p);
            this.creating = false;
            this.showForm = false;
          }
        });
      },
      error: (e) => { this.creating = false; this.toast.error(e.error?.message || 'Failed to create product'); }
    });
  }

  toggle(product: any): void {
    const id = product.productPublicId || product.publicId;
    this.productService.toggle(id).subscribe({
      next: () => { product.isActive = !product.isActive; this.toast.success('Visibility updated'); },
      error: () => this.toast.error('Failed to toggle')
    });
  }

  delete(product: any): void {
    if (!confirm(`Delete "${product.name}"? This cannot be undone.`)) return;
    const id = product.productPublicId || product.publicId;
    this.productService.delete(id).subscribe({
      next: () => { this.products = this.products.filter(p => p !== product); this.toast.success('Product deleted'); },
      error: (e) => this.toast.error(e.error?.message || 'Failed to delete')
    });
  }

  uploadImg(product: any): void { this.uploadingFor = product; this.imgFile = null; }

  onImgSelect(e: any): void { this.imgFile = e.target.files[0] || null; }

  submitImage(): void {
    if (!this.imgFile || !this.uploadingFor) return;
    this.uploading = true;
    const id = this.uploadingFor.productPublicId || this.uploadingFor.publicId;
    this.productService.uploadImage(id, this.imgFile).subscribe({
      next: (res) => {
        this.toast.success('Image uploaded!');
        this.uploading = false;
        if (res.imageUrl) {
          if (!this.uploadingFor.imageUrls) this.uploadingFor.imageUrls = [];
          this.uploadingFor.imageUrls.push(res.imageUrl);
        }
        this.uploadingFor = null;
      },
      error: () => { this.uploading = false; this.toast.error('Image upload failed'); }
    });
  }
}
