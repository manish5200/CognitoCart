import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../../../services/product.service';
import { ToastService } from '../../../../../services/toast.service';

@Component({
  selector: 'app-product-variants-tab',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="variants-tab">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:24px;">
        <h2 style="margin:0; color:#fff;">Variants & Inventory</h2>
        <button class="btn btn-primary" (click)="addVariant()">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          Add Variant
        </button>
      </div>

      <div class="variants-table-wrapper" *ngIf="product?.variants?.length">
        <table class="variants-table">
          <thead>
            <tr>
              <th>SKU</th>
              <th>Variant Label</th>
              <th>Price Adj. (\u20B9)</th>
              <th>Final Price (\u20B9)</th>
              <th>Stock</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let variant of product.variants; let i = index">
              <td><input type="text" class="cell-input" [(ngModel)]="variant.sku" placeholder="SKU..."></td>
              <td><input type="text" class="cell-input" [(ngModel)]="variant.displayLabel" placeholder="e.g. Red / XL"></td>
              <td><input type="number" class="cell-input" [(ngModel)]="variant.priceAdjustment" placeholder="0"></td>
              <td style="font-weight:600; color:#fff;">\u20B9{{ (product.basePrice + (variant.priceAdjustment || 0)) | number:'1.0-0' }}</td>
              <td>
                <div style="display:flex; align-items:center; gap:8px;">
                  <input type="number" class="cell-input" style="width:70px;" [(ngModel)]="variant.stockQuantity" placeholder="0">
                  <div class="stock-indicator" [class.low]="variant.stockQuantity < 10" [class.out]="variant.stockQuantity === 0"></div>
                </div>
              </td>
              <td>
                <span class="badge" [class.active]="variant.status === 'ACTIVE'" [class.inactive]="variant.status !== 'ACTIVE'">
                  {{ variant.status }}
                </span>
              </td>
              <td>
                <button class="btn btn-primary" style="padding: 6px 12px; margin-right: 8px;" (click)="saveVariant(variant)" [disabled]="variant.saving">
                  {{ variant.saving ? '...' : 'Save' }}
                </button>
                <button class="icon-btn" title="Toggle Status" (click)="toggleStatus(variant)" style="margin-right: 8px;"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg></button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div *ngIf="!product?.variants?.length" class="empty-state">
        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>
        <h3>No Variants Yet</h3>
        <p>Add variants like colors, sizes, or materials for this product.</p>
      </div>
    </div>
  `,
  styles: [`
    .variants-tab { animation: fadeIn 0.3s ease; }
    .btn { display: inline-flex; align-items: center; gap: 8px; padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; border: none; }
    .btn-primary { background: var(--primary); color: #fff; }
    .btn-primary:hover { background: var(--primary-hover); }
    
    .variants-table-wrapper {
      background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.05);
      border-radius: 12px; overflow-x: auto;
    }
    .variants-table { width: 100%; border-collapse: collapse; text-align: left; }
    .variants-table th { padding: 16px; font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-dim); border-bottom: 1px solid rgba(255,255,255,0.05); }
    .variants-table td { padding: 12px 16px; border-bottom: 1px solid rgba(255,255,255,0.05); vertical-align: middle; }
    .variants-table tr:last-child td { border-bottom: none; }
    
    .cell-input {
      width: 100%; background: rgba(0,0,0,0.2); border: 1px solid rgba(255,255,255,0.1);
      border-radius: 6px; padding: 8px 12px; color: #fff; font-size: 13px; transition: border-color 0.2s;
    }
    .cell-input:focus { outline: none; border-color: var(--primary); background: rgba(0,0,0,0.4); }
    
    .stock-indicator { width: 8px; height: 8px; border-radius: 50%; background: #10b981; }
    .stock-indicator.low { background: #f59e0b; box-shadow: 0 0 8px rgba(245,158,11,0.4); }
    .stock-indicator.out { background: #ef4444; box-shadow: 0 0 8px rgba(239,68,68,0.4); }
    
    .badge { padding: 4px 8px; border-radius: 20px; font-size: 11px; font-weight: 700; text-transform: uppercase; }
    .badge.active { background: rgba(16,185,129,0.1); color: #10b981; }
    .badge.inactive { background: rgba(255,255,255,0.05); color: var(--text-muted); }
    
    .icon-btn {
      width: 28px; height: 28px; border-radius: 6px; background: rgba(255,255,255,0.05);
      color: var(--text-muted); border: none; cursor: pointer;
      display: flex; align-items: center; justify-content: center; transition: all 0.2s;
    }
    .icon-btn:hover { background: rgba(255,255,255,0.1); color: #fff; }
    .icon-btn.danger:hover { background: rgba(239,68,68,0.1); color: #ef4444; }

    .empty-state { padding: 48px; text-align: center; border: 1px dashed rgba(255,255,255,0.1); border-radius: 12px; margin-top: 24px; color: var(--text-muted); }
    .empty-state svg { color: var(--text-dim); margin-bottom: 16px; }
    .empty-state h3 { color: #fff; margin: 0 0 8px 0; font-size: 18px; }
    .empty-state p { margin: 0; font-size: 14px; }

    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class ProductVariantsTabComponent implements OnInit {
  @Input() product: any = {};

  constructor(private productService: ProductService, private toast: ToastService) {}

  ngOnInit() {
    this.loadVariants();
  }

  loadVariants() {
    if (this.product && this.product.productPublicId) {
      this.productService.getVariants(this.product.productPublicId).subscribe({
        next: (variants) => {
          this.product.variants = variants;
        }
      });
    }
  }

  addVariant() {
    if (!this.product.variants) this.product.variants = [];
    this.product.variants.push({
      sku: '',
      displayLabel: '',
      priceAdjustment: 0,
      stockQuantity: 0,
      status: 'ACTIVE',
      isNew: true
    });
  }

  saveVariant(variant: any) {
    if (!this.product || !this.product.productPublicId) return;
    
    variant.saving = true;
    const payload = {
      sku: variant.sku,
      attributes: { 'Label': variant.displayLabel }, // Send as attributes map
      priceAdjustment: variant.priceAdjustment,
      stockQuantity: variant.stockQuantity // Fix property name
    };

    if (variant.isNew) {
      this.productService.createVariant(this.product.productPublicId, payload).subscribe({
        next: (res) => {
          variant.saving = false;
          variant.isNew = false;
          variant.publicId = res.variantPublicId;
          this.toast.success('Variant created successfully');
          this.loadVariants(); // Reload to get fresh data
        },
        error: (e) => {
          variant.saving = false;
          this.toast.error(e.error?.message || 'Failed to create variant');
        }
      });
    } else {
      // For updates, we usually just update the basic info, stock is adjusted separately, but we'll try to update
      this.productService.updateVariant(this.product.productPublicId, variant.publicId || variant.variantPublicId, payload).subscribe({
        next: () => {
          variant.saving = false;
          this.toast.success('Variant updated successfully');
        },
        error: (e) => {
          variant.saving = false;
          this.toast.error(e.error?.message || 'Failed to update variant');
        }
      });
    }
  }

  toggleStatus(variant: any) {
    if (!this.product || !this.product.productPublicId || variant.isNew) return;
    
    this.productService.toggleVariantStatus(this.product.productPublicId, variant.publicId || variant.variantPublicId).subscribe({
      next: () => {
        variant.status = variant.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
        this.toast.success('Variant status toggled');
      },
      error: (e) => {
        this.toast.error(e.error?.message || 'Failed to toggle status');
      }
    });
  }
}
