import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../../../services/product.service';

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
              <select class="form-select" [(ngModel)]="product.categoryId" (change)="onCategoryChange($event)">
                <option [value]="product.categoryId">{{product.categoryName || 'Select Category'}}</option>
                <option *ngFor="let cat of flatCategories" [value]="cat.publicId">{{ cat.name }}</option>
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
export class ProductInfoTabComponent implements OnInit {
  @Input() product: any = {};
  categories: any[] = [];
  flatCategories: any[] = [];

  constructor(private productService: ProductService) {}

  ngOnInit() {
    this.productService.getCategories().subscribe({
      next: (res) => {
        this.categories = res;
        this.flattenCategories(this.categories);
      }
    });
  }

  flattenCategories(categories: any[], prefix = '') {
    for (const cat of categories) {
      this.flatCategories.push({
        publicId: cat.publicId,
        name: prefix + cat.name
      });
      if (cat.subCategories && cat.subCategories.length > 0) {
        this.flattenCategories(cat.subCategories, prefix + cat.name + ' > ');
      }
    }
  }

  onCategoryChange(event: any) {
    const selected = this.flatCategories.find(c => c.publicId === this.product.categoryId);
    if (selected) {
      this.product.categoryName = selected.name.split(' > ').pop();
    }
  }

  get tagsString(): string {
    return this.product.tags ? this.product.tags.join(', ') : '';
  }

  set tagsString(val: string) {
    this.product.tags = val ? val.split(',').map(t => t.trim()).filter(t => t) : [];
  }
}
