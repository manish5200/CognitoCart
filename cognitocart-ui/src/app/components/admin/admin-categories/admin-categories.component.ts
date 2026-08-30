import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../services/product.service';
import { ToastService } from '../../../services/toast.service';
import { AdminShellComponent } from '../admin-shell/admin-shell.component';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminShellComponent],
  template: `
    <app-admin-shell>
      <div class="page-header" style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <h1 class="page-title" style="display:flex; align-items:center; gap:12px;">
            <div style="background:var(--grad-brand); border-radius:8px; padding:6px; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 14px rgba(99,102,241,0.3);">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/></svg>
            </div>
            Category Management
          </h1>
          <p class="page-subtitle">Organize and manage the product taxonomy tree.</p>
        </div>
        <button class="btn btn-primary" (click)="openModal()">
          + Add Root Category
        </button>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div *ngIf="!loading && topLevelCategories.length === 0" class="empty-state">
        No categories found. Start by adding a root category.
      </div>

      <div class="card" *ngIf="!loading && topLevelCategories.length > 0">
        <div class="card-body" style="padding: 0;">
          <div class="category-tree" style="padding: 16px;">
            <ng-container *ngTemplateOutlet="recursiveList; context:{ $implicit: topLevelCategories }"></ng-container>
          </div>
        </div>
      </div>

      <ng-template #recursiveList let-list>
        <ul class="cat-list">
          <li *ngFor="let cat of list" class="cat-item">
            <div class="cat-row">
              <div class="cat-info">
                <span class="cat-name">{{ cat.name }}</span>
                <span class="cat-slug">{{ cat.slug }}</span>
              </div>
              <div class="cat-actions">
                <button class="btn-icon" (click)="openModal(cat.publicId)" title="Add Subcategory">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></svg>
                </button>
                <button class="btn-icon" (click)="editCategory(cat)" title="Edit">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
                </button>
                <button class="btn-icon danger" (click)="deleteCategory(cat.publicId)" title="Delete">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
                </button>
              </div>
            </div>
            <div *ngIf="cat.subCategories && cat.subCategories.length > 0" class="subcat-container">
              <ng-container *ngTemplateOutlet="recursiveList; context:{ $implicit: cat.subCategories }"></ng-container>
            </div>
          </li>
        </ul>
      </ng-template>

      <!-- Category Modal -->
      <div class="modal-overlay" *ngIf="showModal" (click)="closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>{{ editingId ? 'Edit Category' : 'Add Category' }}</h3>
            <button class="close-btn" (click)="closeModal()">&times;</button>
          </div>
          <div class="modal-body">
            <div class="form-group" *ngIf="parentCategoryName">
              <label class="form-label">Parent Category</label>
              <input type="text" class="form-input" [value]="parentCategoryName" disabled style="background:#f9fafb; color:var(--text-muted);" />
            </div>
            <div class="form-group">
              <label class="form-label">Category Name</label>
              <input type="text" class="form-input" [(ngModel)]="currentCategory.name" placeholder="e.g., Men's Clothing" />
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" (click)="closeModal()">Cancel</button>
            <button class="btn btn-primary" (click)="saveCategory()" [disabled]="!currentCategory.name?.trim()">
              {{ editingId ? 'Save Changes' : 'Create Category' }}
            </button>
          </div>
        </div>
      </div>
    </app-admin-shell>
  `,
  styles: [`
    .cat-list { list-style: none; padding: 0; margin: 0; }
    .cat-item { border-left: 2px solid var(--border-subtle); margin-left: 12px; margin-top: 8px; }
    .cat-item:first-child { margin-top: 0; }
    .cat-row {
      display: flex; justify-content: space-between; align-items: center;
      padding: 10px 16px; background: #fff; border: 1px solid var(--border-subtle);
      border-radius: 6px; margin-bottom: 8px; margin-left: 12px; transition: 0.2s;
    }
    .cat-row:hover { border-color: rgba(99,102,241,0.3); box-shadow: 0 4px 12px rgba(0,0,0,0.03); }
    .cat-info { display: flex; align-items: center; gap: 12px; }
    .cat-name { font-weight: 600; color: var(--text-primary); font-size: 14px; }
    .cat-slug { font-size: 12px; color: var(--text-muted); background: #f3f4f6; padding: 2px 8px; border-radius: 4px; }
    .cat-actions { display: flex; gap: 4px; }
    .btn-icon {
      background: transparent; border: none; padding: 6px; cursor: pointer;
      color: var(--text-muted); border-radius: 4px; transition: 0.2s;
    }
    .btn-icon:hover { background: var(--bg-hover); color: var(--brand); }
    .btn-icon.danger:hover { color: var(--danger); background: rgba(239,68,68,0.1); }
    .subcat-container { padding-left: 12px; }
    
    .modal-overlay {
      position: fixed; top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 2000;
      backdrop-filter: blur(2px);
    }
    .modal {
      background: #fff; border-radius: 12px; width: 400px; max-width: 90vw;
      box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04);
      animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
    }
    .modal-header { padding: 16px 24px; border-bottom: 1px solid var(--border-subtle); display: flex; justify-content: space-between; align-items: center; }
    .modal-header h3 { margin: 0; font-size: 18px; color: var(--text-primary); }
    .close-btn { background: none; border: none; font-size: 24px; cursor: pointer; color: var(--text-muted); }
    .modal-body { padding: 24px; }
    .modal-footer { padding: 16px 24px; border-top: 1px solid var(--border-subtle); display: flex; justify-content: flex-end; gap: 12px; background: #f9fafb; border-bottom-left-radius: 12px; border-bottom-right-radius: 12px; }
    @keyframes slideUp {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class AdminCategoriesComponent implements OnInit {
  loading = false;
  categories: any[] = [];
  topLevelCategories: any[] = [];
  
  showModal = false;
  editingId: string | null = null;
  parentCategoryName: string | null = null;
  
  currentCategory: { name: string, parentCategory?: { publicId: string } } = { name: '' };

  constructor(
    private productService: ProductService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading = true;
    this.productService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
        // Assume API returns a nested structure where parent is null for top level categories.
        this.topLevelCategories = this.categories.filter(c => !c.parentCategory);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error('Failed to load categories');
      }
    });
  }

  openModal(parentId?: string): void {
    this.editingId = null;
    this.currentCategory = { name: '' };
    this.parentCategoryName = null;
    
    if (parentId) {
      this.currentCategory.parentCategory = { publicId: parentId };
      const parent = this.findCategoryByPublicId(this.categories, parentId);
      if (parent) {
        this.parentCategoryName = parent.name;
      }
    }
    
    this.showModal = true;
  }
  
  editCategory(cat: any): void {
    this.editingId = cat.publicId;
    this.currentCategory = { name: cat.name };
    this.parentCategoryName = null;
    
    // In our backend, parentCategory might not be serialized if it's bidirectional
    // but if it is, we can extract it.
    if (cat.parentCategory && cat.parentCategory.publicId) {
      this.currentCategory.parentCategory = { publicId: cat.parentCategory.publicId };
      const parent = this.findCategoryByPublicId(this.categories, cat.parentCategory.publicId);
      if (parent) {
        this.parentCategoryName = parent.name;
      }
    }
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.currentCategory = { name: '' };
    this.editingId = null;
    this.parentCategoryName = null;
  }

  saveCategory(): void {
    if (!this.currentCategory.name?.trim()) return;
    
    if (this.editingId) {
      this.productService.updateCategory(this.editingId, this.currentCategory).subscribe({
        next: () => {
          this.toast.success('Category updated successfully');
          this.closeModal();
          this.loadCategories();
        },
        error: () => this.toast.error('Failed to update category')
      });
    } else {
      this.productService.addCategory(this.currentCategory).subscribe({
        next: () => {
          this.toast.success('Category created successfully');
          this.closeModal();
          this.loadCategories();
        },
        error: () => this.toast.error('Failed to create category')
      });
    }
  }

  deleteCategory(publicId: string): void {
    if (confirm('Are you sure you want to delete this category? This may affect products linked to it.')) {
      this.productService.deleteCategory(publicId).subscribe({
        next: () => {
          this.toast.success('Category deleted');
          this.loadCategories();
        },
        error: () => this.toast.error('Failed to delete category')
      });
    }
  }

  private findCategoryByPublicId(list: any[], id: string): any {
    for (const cat of list) {
      if (cat.publicId === id) return cat;
      if (cat.subCategories && cat.subCategories.length > 0) {
        const found = this.findCategoryByPublicId(cat.subCategories, id);
        if (found) return found;
      }
    }
    return null;
  }
}
