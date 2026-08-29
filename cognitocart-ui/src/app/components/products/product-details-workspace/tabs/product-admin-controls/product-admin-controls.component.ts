import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-product-admin-controls',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="admin-tab">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:24px;">
        <h2 style="margin:0; color:#fff;">Admin Moderation Controls</h2>
        <div class="badge admin-badge">ADMIN PRIVILEGES ACTIVE</div>
      </div>

      <div class="alert warning">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
        <span>Changes made here override seller settings and are logged for audit purposes.</span>
      </div>

      <div class="settings-group">
        <h3 class="group-title">Product Visibility</h3>
        <div class="setting-row">
          <div class="setting-info">
            <h4>Suspend Product</h4>
            <p>Hide this product from the marketplace. It will remain in the seller's dashboard but cannot be purchased.</p>
          </div>
          <button class="btn btn-outline danger" *ngIf="product?.status !== 'SUSPENDED'">Suspend Product</button>
          <button class="btn btn-primary" *ngIf="product?.status === 'SUSPENDED'">Restore Product</button>
        </div>
      </div>

      <div class="settings-group">
        <h3 class="group-title">Trust & Safety</h3>
        <div class="setting-row">
          <div class="setting-info">
            <h4>Flag for Review</h4>
            <p>Flag this product for terms of service violations (e.g. counterfeit, offensive content).</p>
          </div>
          <button class="btn btn-outline warning">Flag Product</button>
        </div>
        <div class="setting-row">
          <div class="setting-info">
            <h4>Force Return Policy Compliance</h4>
            <p>Override the seller's return policy if it violates marketplace minimum standards.</p>
          </div>
          <button class="btn btn-outline primary">Override Policy</button>
        </div>
      </div>

      <div class="settings-group">
        <h3 class="group-title">Platform Promotion</h3>
        <div class="setting-row">
          <div class="setting-info">
            <h4>Feature in Flash Sales</h4>
            <p>Add this product to the homepage flash sales carousel.</p>
          </div>
          <button class="btn btn-outline primary">Add to Flash Sale</button>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .admin-tab { animation: fadeIn 0.3s ease; }
    .badge { padding: 6px 12px; border-radius: 20px; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; }
    .admin-badge { background: rgba(239,68,68,0.15); color: #ef4444; border: 1px solid rgba(239,68,68,0.3); }
    
    .alert { display: flex; align-items: flex-start; gap: 12px; padding: 16px; border-radius: 12px; margin-bottom: 32px; font-size: 14px; line-height: 1.5; }
    .alert.warning { background: rgba(245,158,11,0.1); color: #f59e0b; border: 1px solid rgba(245,158,11,0.2); }
    
    .settings-group { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.05); border-radius: 12px; margin-bottom: 24px; overflow: hidden; }
    .group-title { margin: 0; padding: 20px 24px; font-size: 14px; text-transform: uppercase; letter-spacing: 1px; color: var(--text-muted); border-bottom: 1px solid rgba(255,255,255,0.05); background: rgba(0,0,0,0.2); }
    
    .setting-row { display: flex; justify-content: space-between; align-items: center; padding: 24px; border-bottom: 1px solid rgba(255,255,255,0.05); }
    .setting-row:last-child { border-bottom: none; }
    
    .setting-info h4 { margin: 0 0 4px 0; color: #fff; font-size: 16px; }
    .setting-info p { margin: 0; color: var(--text-dim); font-size: 13px; max-width: 500px; line-height: 1.5; }
    
    .btn { display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; border: none; }
    .btn-outline { background: transparent; border: 1px solid; }
    .btn-outline.danger { color: #ef4444; border-color: rgba(239,68,68,0.3); }
    .btn-outline.danger:hover { background: rgba(239,68,68,0.1); border-color: #ef4444; }
    .btn-outline.warning { color: #f59e0b; border-color: rgba(245,158,11,0.3); }
    .btn-outline.warning:hover { background: rgba(245,158,11,0.1); border-color: #f59e0b; }
    .btn-outline.primary { color: var(--primary); border-color: rgba(99,102,241,0.3); }
    .btn-outline.primary:hover { background: rgba(99,102,241,0.1); border-color: var(--primary); }
    .btn-primary { background: var(--primary); color: #fff; }

    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class ProductAdminControlsComponent {
  @Input() product: any;
}
