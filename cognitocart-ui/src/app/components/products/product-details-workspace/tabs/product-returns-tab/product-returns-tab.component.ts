import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductReturnStatusComponent } from '../../../../shared/product-return-status/product-return-status.component';
import { ProductService } from '../../../../../services/product.service';
import { ToastService } from '../../../../../services/toast.service';

@Component({
  selector: 'app-product-returns-tab',
  standalone: true,
  imports: [CommonModule, FormsModule, ProductReturnStatusComponent],
  template: `
    <div class="returns-tab">
      <h2 style="margin-bottom:24px; color:#fff;">Return Policy Configuration</h2>
      
      <div class="policy-card">
        <div class="policy-header">
          <div class="policy-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
          </div>
          <div style="flex: 1;">
            <div style="display:flex; justify-content:space-between; align-items:center;">
              <h3 style="margin:0; color:#fff;">Product Return Policy</h3>
              <app-product-return-status [isReturnable]="product?.isReturnable"></app-product-return-status>
            </div>
            <p style="margin:4px 0 0 0; color:var(--text-muted); font-size:13px;">Configure the return and exchange rules for this specific product.</p>
          </div>
        </div>

        <div class="form-group" style="margin-top:24px;">
          <label class="form-label">Policy Type</label>
          <div class="radio-grid">
            <label class="radio-card" [class.selected]="policyType === 'NON_RETURNABLE'">
              <input type="radio" name="policyType" [(ngModel)]="policyType" value="NON_RETURNABLE">
              <span class="rc-title" style="color:#ef4444;">Non-Returnable</span>
              <span class="rc-desc">Item cannot be returned or exchanged.</span>
            </label>
            <label class="radio-card" [class.selected]="policyType === 'RETURN_ONLY'">
              <input type="radio" name="policyType" [(ngModel)]="policyType" value="RETURN_ONLY">
              <span class="rc-title" style="color:#10b981;">Return Only</span>
              <span class="rc-desc">Customer gets a full refund on return.</span>
            </label>
            <label class="radio-card" [class.selected]="policyType === 'EXCHANGE_ONLY'">
              <input type="radio" name="policyType" [(ngModel)]="policyType" value="EXCHANGE_ONLY">
              <span class="rc-title" style="color:#3b82f6;">Exchange Only</span>
              <span class="rc-desc">Customer can exchange for a different variant.</span>
            </label>
            <label class="radio-card" [class.selected]="policyType === 'RETURN_AND_EXCHANGE'">
              <input type="radio" name="policyType" [(ngModel)]="policyType" value="RETURN_AND_EXCHANGE">
              <span class="rc-title" style="color:#a855f7;">Return & Exchange</span>
              <span class="rc-desc">Full flexibility for the customer.</span>
            </label>
          </div>
        </div>

        <div class="form-group" *ngIf="policyType !== 'NON_RETURNABLE'" style="margin-top:24px;">
          <label class="form-label">Return Window (Days)</label>
          <input type="number" class="form-input" [(ngModel)]="returnWindowDays" min="1" max="90" style="max-width:200px;">
          <div style="font-size:12px; color:var(--text-muted); margin-top:8px;">Standard window is usually 7, 15, or 30 days.</div>
        </div>

      </div>

      <div class="save-actions" style="margin-top:24px; display:flex; justify-content:flex-end;">
        <button class="btn btn-primary" (click)="savePolicy()">Save Policy</button>
      </div>
    </div>
  `,
  styles: [`
    .returns-tab { animation: fadeIn 0.3s ease; }
    .policy-card { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.05); border-radius: 12px; padding: 32px; }
    .policy-header { display: flex; align-items: flex-start; gap: 16px; }
    .policy-icon { width: 48px; height: 48px; background: rgba(99,102,241,0.1); border-radius: 12px; display: flex; align-items: center; justify-content: center; color: var(--primary); }
    
    .radio-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-top: 8px; }
    .radio-card { display: flex; flex-direction: column; padding: 16px; border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; cursor: pointer; transition: all 0.2s; position: relative; }
    .radio-card:hover { background: rgba(255,255,255,0.03); }
    .radio-card input[type="radio"] { position: absolute; opacity: 0; }
    .radio-card.selected { border-color: var(--primary); background: rgba(99,102,241,0.05); }
    .rc-title { font-weight: 700; font-size: 15px; margin-bottom: 4px; }
    .rc-desc { font-size: 12px; color: var(--text-muted); }

    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class ProductReturnsTabComponent implements OnInit {
  @Input() product: any;
  
  policyType: string = 'NON_RETURNABLE';
  returnWindowDays: number = 7;
  saving = false;

  constructor(private productService: ProductService, private toast: ToastService) {}

  ngOnInit() {
    this.loadPolicy();
  }

  loadPolicy() {
    if (this.product && this.product.productPublicId) {
      this.productService.getReturnPolicy(this.product.productPublicId).subscribe({
        next: (policy) => {
          this.policyType = policy.policyType || 'NON_RETURNABLE';
          this.returnWindowDays = policy.returnWindowDays || 7;
          // Store the policy ID if we want to update it later, but only if it's a product-level policy
          if (policy.scope === 'PRODUCT') {
            this.product.policyPublicId = policy.policyPublicId; 
          } else {
            this.product.policyPublicId = null;
          }
        },
        error: () => {
          // Fallback to default
          this.policyType = 'NON_RETURNABLE';
          this.returnWindowDays = 7;
        }
      });
    }
  }

  savePolicy() {
    if (!this.product) return;
    this.saving = true;
    
    // We update the product's return info via the product update endpoint
    // Assuming we have an update endpoint or we construct a payload
    const isReturnable = this.policyType !== 'NON_RETURNABLE';
    
    const payload = {
      productPublicId: this.product.productPublicId,
      policyType: this.policyType,
      returnWindowDays: isReturnable ? this.returnWindowDays : 0,
      returnAllowed: this.policyType === 'RETURN_ONLY' || this.policyType === 'RETURN_AND_EXCHANGE',
      exchangeAllowed: this.policyType === 'EXCHANGE_ONLY' || this.policyType === 'RETURN_AND_EXCHANGE',
      replacementAllowed: false,
      pickupAvailable: isReturnable
    };
    
    // Update local state immediately for optimistic UI
    this.product.isReturnable = isReturnable;

    // Use actual backend endpoint
    if (this.product.policyPublicId) {
      this.productService.updateReturnPolicy(this.product.policyPublicId, payload).subscribe({
        next: () => {
          this.saving = false;
          this.toast.success('Return policy updated successfully.');
        },
        error: (e) => {
          this.saving = false;
          this.toast.error(e.error?.message || 'Failed to update policy');
        }
      });
    } else {
      this.productService.createReturnPolicy(payload).subscribe({
        next: (res) => {
          this.saving = false;
          this.product.policyPublicId = res.policyPublicId;
          this.toast.success('Return policy created successfully.');
        },
        error: (e) => {
          this.saving = false;
          this.toast.error(e.error?.message || 'Failed to create policy');
        }
      });
    }
  }
}
