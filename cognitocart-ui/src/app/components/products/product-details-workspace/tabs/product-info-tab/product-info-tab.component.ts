import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-product-info-tab',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="info-tab">
      <h2 style="margin-bottom:24px; color:#fff;">Basic Information</h2>
      
      <div class="card">
        <div class="card-body">
          <div class="form-grid">
            <div class="form-group" style="grid-column: span 2;">
              <label class="form-label">Product Name</label>
              <input type="text" class="form-input" [(ngModel)]="product.productName" placeholder="e.g. Nike Air Max">
            </div>
            
            <div class="form-group">
              <label class="form-label">Category</label>
              <select class="form-select" [(ngModel)]="product.categoryId">
                <option [value]="product.categoryId">{{product.categoryName || 'Select Category'}}</option>
                <!-- Populate dynamically later -->
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">Base Price (\u20B9)</label>
              <input type="number" class="form-input" [(ngModel)]="product.price" placeholder="0.00">
            </div>

            <div class="form-group" style="grid-column: span 2;">
              <label class="form-label">Description</label>
              <textarea class="form-textarea" [(ngModel)]="product.description" rows="5" placeholder="Detailed product description..."></textarea>
            </div>
            
            <div class="form-group" style="grid-column: span 2;">
              <label class="form-label">Search Tags</label>
              <input type="text" class="form-input" [(ngModel)]="tagsString" placeholder="e.g. shoes, running, sneakers (comma separated)">
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .info-tab { animation: fadeIn 0.3s ease; }
    .card { background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.05); border-radius: 12px; }
    .card-body { padding: 32px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
    @media (max-width: 768px) { .form-grid { grid-template-columns: 1fr; } .form-group[style] { grid-column: span 1 !important; } }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class ProductInfoTabComponent {
  @Input() product: any = {};

  get tagsString(): string {
    return this.product.tags ? this.product.tags.join(', ') : '';
  }

  set tagsString(val: string) {
    this.product.tags = val ? val.split(',').map(t => t.trim()).filter(t => t) : [];
  }
}
