import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SellerService } from '../../../services/seller.service';
import { ProductService } from '../../../services/product.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-seller-policies',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="command-center">
      <div class="cc-header">
        <div class="header-left">
          <div class="greeting-block">
            <h1 style="display:flex; align-items:center; gap:12px;">
              <div class="kpi-icon kpi-icon-purple">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:24px;height:24px;color:var(--purple-400);"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25ZM6.75 12h.008v.008H6.75V12Zm0 3h.008v.008H6.75V15Zm0 3h.008v.008H6.75V18Z" /></svg>
              </div>
              Return Policies
            </h1>
            <p>Manage return rules for your products and categories.</p>
          </div>
        </div>
        <div class="header-right">
          <button class="btn btn-primary cc-btn" (click)="showForm = !showForm">
            <svg *ngIf="!showForm" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:8px;"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
            <svg *ngIf="showForm" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:8px;"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            {{showForm ? 'Cancel Creation' : 'Create Policy'}}
          </button>
        </div>
      </div>

      <!-- Create Policy Form -->
      <div class="cc-panel" *ngIf="showForm" style="margin-bottom:32px;">
        <div class="panel-header border-bottom">
          <h2>
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
            Policy Details
          </h2>
        </div>
        <div class="panel-body" style="padding-top: 24px;">
          <div class="form-row form-row-2">
            <div class="form-group">
              <label class="form-label">Scope <span class="text-danger">*</span></label>
              <select [(ngModel)]="newPolicy.scopeType" (change)="onScopeChange()" class="form-select">
                <option value="PRODUCT">Specific Product</option>
                <option value="CATEGORY">Entire Category</option>
              </select>
            </div>
            
            <div class="form-group" *ngIf="newPolicy.scopeType === 'PRODUCT'">
              <label class="form-label">Product <span class="text-danger">*</span></label>
              <select [(ngModel)]="newPolicy.productId" class="form-select">
                <option value="">Select Product...</option>
                <option *ngFor="let p of products" [value]="p.productPublicId || p.publicId || p.id || ''">{{p.productName || p.name}}</option>
              </select>
            </div>
            
            <div class="form-group" *ngIf="newPolicy.scopeType === 'CATEGORY'">
              <label class="form-label">Category <span class="text-danger">*</span></label>
              <select [(ngModel)]="newPolicy.categoryId" class="form-select">
                <option value="">Select Category...</option>
                <option *ngFor="let c of categories" [value]="c.publicId || c.categoryPublicId || c.id || ''">{{c.name}}</option>
              </select>
            </div>
          </div>

          <div class="form-row form-row-2" style="margin-top:16px;">
            <div class="form-group">
              <label class="form-label">Policy Type <span class="text-danger">*</span></label>
              <select [(ngModel)]="newPolicy.policyType" class="form-select">
                <option value="RETURN_AND_EXCHANGE">Return and Exchange</option>
                <option value="RETURN_ONLY">Return Only</option>
                <option value="EXCHANGE_ONLY">Exchange Only</option>
                <option value="REPLACEMENT_ONLY">Replacement Only</option>
                <option value="NON_RETURNABLE">Non Returnable</option>
              </select>
            </div>
            <div class="form-group" *ngIf="newPolicy.policyType !== 'NON_RETURNABLE'">
              <label class="form-label">Return Window (Days) <span class="text-danger">*</span></label>
              <input type="number" [(ngModel)]="newPolicy.returnWindowDays" class="form-input" placeholder="e.g. 7" min="1" max="30" />
            </div>
          </div>

          <div class="form-group" style="margin-top:24px;" *ngIf="newPolicy.policyType !== 'NON_RETURNABLE'">
            <label class="form-label">Additional Options</label>
            <div style="display:flex; gap:24px; flex-wrap:wrap; margin-top:8px;">
              <label style="display:flex; align-items:center; gap:8px; cursor:pointer;">
                <input type="checkbox" [(ngModel)]="newPolicy.returnAllowed" style="width:16px;height:16px;" /> <span style="color:var(--text-primary);">Returns Allowed</span>
              </label>
              <label style="display:flex; align-items:center; gap:8px; cursor:pointer;">
                <input type="checkbox" [(ngModel)]="newPolicy.exchangeAllowed" style="width:16px;height:16px;" /> <span style="color:var(--text-primary);">Exchanges Allowed</span>
              </label>
              <label style="display:flex; align-items:center; gap:8px; cursor:pointer;">
                <input type="checkbox" [(ngModel)]="newPolicy.replacementAllowed" style="width:16px;height:16px;" /> <span style="color:var(--text-primary);">Replacements Allowed</span>
              </label>
              <label style="display:flex; align-items:center; gap:8px; cursor:pointer;">
                <input type="checkbox" [(ngModel)]="newPolicy.pickupAvailable" style="width:16px;height:16px;" /> <span style="color:var(--text-primary);">Pickup Available</span>
              </label>
            </div>
          </div>

          <div style="margin-top: 24px; display:flex; justify-content:flex-end;">
            <button class="btn btn-primary cc-btn" (click)="createPolicy()" [disabled]="creating">
              {{creating ? 'Creating...' : 'Save Policy'}}
            </button>
          </div>
        </div>
      </div>

      <!-- Policies List -->
      <div class="loading-center" *ngIf="loading" style="min-height: 200px; display:flex; align-items:center; justify-content:center;">
        <div class="spinner"></div>
      </div>

      <div class="cc-panel" *ngIf="!loading">
        <div class="panel-header border-bottom">
          <h2>
            <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width:20px;height:20px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 0 0 2.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 0 0-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 0 0 .75-.75 2.25 2.25 0 0 0-.1-.664m-5.8 0A2.251 2.251 0 0 1 13.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25ZM6.75 12h.008v.008H6.75V12Zm0 3h.008v.008H6.75V15Zm0 3h.008v.008H6.75V18Z" /></svg>
            Active Policies
            <span class="badge badge-purple" style="margin-left: 8px;">{{policies.length}} Policies</span>
          </h2>
        </div>
        
        <div class="cc-table-wrapper" *ngIf="policies.length > 0">
          <table class="cc-table">
            <thead>
              <tr>
                <th>Scope / Target</th>
                <th>Policy Type</th>
                <th>Window</th>
                <th>Capabilities</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let p of policies">
                <td>
                  <div style="font-weight:600; color:var(--text-primary);">{{p.scope === 'PRODUCT' ? p.productName : p.categoryName}}</div>
                  <div style="display:flex; align-items:center; gap:8px; margin-top:4px;">
                    <span class="badge" [class.badge-blue]="p.scope === 'PRODUCT'" [class.badge-purple]="p.scope === 'CATEGORY'">
                      {{p.scope}}
                    </span>
                  </div>
                </td>
                <td>
                  <span class="badge" [class.badge-red]="p.policyType === 'NON_RETURNABLE'" [class.badge-green]="p.policyType !== 'NON_RETURNABLE'">
                    {{p.policyType.replace('_', ' ')}}
                  </span>
                </td>
                <td style="color:var(--text-primary); font-weight:600;">
                  {{p.policyType === 'NON_RETURNABLE' ? '-' : p.returnWindowDays + ' Days'}}
                </td>
                <td>
                  <div style="display:flex; gap:8px; flex-wrap:wrap; font-size:12px;">
                    <span *ngIf="p.returnAllowed" style="color:var(--text-muted);">✓ Return</span>
                    <span *ngIf="p.exchangeAllowed" style="color:var(--text-muted);">✓ Exchange</span>
                    <span *ngIf="p.replacementAllowed" style="color:var(--text-muted);">✓ Replace</span>
                    <span *ngIf="p.pickupAvailable" style="color:var(--text-muted);">✓ Pickup</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="cc-empty-state" *ngIf="policies.length === 0" style="margin: 24px; border: none; background: transparent;">
          <div class="empty-icon-ring" style="background: rgba(255,255,255,0.05); border-color: rgba(255,255,255,0.1); color: var(--text-muted);">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" /></svg>
          </div>
          <h3>No Policies Found</h3>
          <p>You haven't defined any custom return policies yet. Default policies may apply.</p>
          <button class="btn btn-primary" style="margin-top: 16px;" (click)="showForm = true">Create Policy</button>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class SellerPoliciesComponent implements OnInit {
  policies: any[] = [];
  products: any[] = [];
  categories: any[] = [];
  
  loading = true;
  showForm = false;
  creating = false;
  
  newPolicy: any = {
    scopeType: 'PRODUCT',
    productId: '',
    categoryId: '',
    policyType: 'RETURN_AND_EXCHANGE',
    returnWindowDays: 7,
    returnAllowed: true,
    exchangeAllowed: true,
    replacementAllowed: true,
    pickupAvailable: true
  };

  constructor(
    private sellerService: SellerService,
    private productService: ProductService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.loadPolicies();
    this.loadDropdownData();
  }

  loadPolicies(): void {
    this.loading = true;
    this.sellerService.getPolicies().subscribe({
      next: (res) => { this.policies = res || []; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  loadDropdownData(): void {
    this.productService.getAll(0, 1000).subscribe({
      next: (res) => { this.products = res.content ?? res ?? []; },
      error: () => {}
    });
    this.productService.getCategories().subscribe({
      next: (res) => { this.categories = res; },
      error: () => {}
    });
  }
  
  onScopeChange(): void {
    if (this.newPolicy.scopeType === 'PRODUCT') {
      this.newPolicy.categoryId = '';
    } else {
      this.newPolicy.productId = '';
    }
  }

  createPolicy(): void {
    if (this.newPolicy.scopeType === 'PRODUCT' && !this.newPolicy.productId) {
      this.toast.warning('Please select a product');
      return;
    }
    if (this.newPolicy.scopeType === 'CATEGORY' && !this.newPolicy.categoryId) {
      this.toast.warning('Please select a category');
      return;
    }

    const payload: any = {
      policyType: this.newPolicy.policyType,
      returnWindowDays: this.newPolicy.policyType === 'NON_RETURNABLE' ? 0 : this.newPolicy.returnWindowDays,
      returnAllowed: this.newPolicy.policyType === 'NON_RETURNABLE' ? false : this.newPolicy.returnAllowed,
      exchangeAllowed: this.newPolicy.policyType === 'NON_RETURNABLE' ? false : this.newPolicy.exchangeAllowed,
      replacementAllowed: this.newPolicy.policyType === 'NON_RETURNABLE' ? false : this.newPolicy.replacementAllowed,
      pickupAvailable: this.newPolicy.policyType === 'NON_RETURNABLE' ? false : this.newPolicy.pickupAvailable
    };

    if (this.newPolicy.scopeType === 'PRODUCT') {
      payload.productPublicId = this.newPolicy.productId;
    } else {
      payload.categoryPublicId = this.newPolicy.categoryId;
    }

    this.creating = true;
    this.sellerService.createPolicy(payload).subscribe({
      next: (p) => {
        this.toast.success('Policy created successfully!');
        this.policies.unshift(p);
        this.creating = false;
        this.showForm = false;
        
        // Reset form
        this.newPolicy = {
          scopeType: 'PRODUCT',
          productId: '',
          categoryId: '',
          policyType: 'RETURN_AND_EXCHANGE',
          returnWindowDays: 7,
          returnAllowed: true,
          exchangeAllowed: true,
          replacementAllowed: true,
          pickupAvailable: true
        };
      },
      error: (e) => {
        this.creating = false;
        this.toast.error(e.error?.message || 'Failed to create policy');
      }
    });
  }
}
