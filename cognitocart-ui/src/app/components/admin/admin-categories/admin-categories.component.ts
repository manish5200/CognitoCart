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

      <!-- Header -->
      <div class="page-header">
        <div class="header-left">
          <div class="header-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/></svg>
          </div>
          <div>
            <h1 class="page-title">Category Management</h1>
            <p class="page-subtitle">Organize and manage your product taxonomy tree</p>
          </div>
        </div>
        <div class="header-right">
          <div class="stats-chip">
            <span class="stats-dot"></span>
            {{ totalCount }} Total Categories
          </div>
          <button class="add-btn" (click)="openModal()">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></svg>
            Add Root Category
          </button>
        </div>
      </div>

      <!-- Loading -->
      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <!-- Empty -->
      <div *ngIf="!loading && topLevelCategories.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/></svg>
        </div>
        <div class="empty-title">No categories yet</div>
        <div class="empty-sub">Start by creating your first root category</div>
        <button class="add-btn" (click)="openModal()" style="margin-top:20px;">+ Add Root Category</button>
      </div>

      <!-- Category Tree -->
      <div class="tree-container" *ngIf="!loading && topLevelCategories.length > 0">
        <ng-container *ngTemplateOutlet="recursiveList; context:{ $implicit: topLevelCategories, depth: 0 }"></ng-container>
      </div>

      <!-- Recursive Template -->
      <ng-template #recursiveList let-list let-depth="depth">
        <div class="cat-group" [class.nested]="depth > 0">
          <div *ngFor="let cat of list; let i = index" class="cat-entry" [attr.data-depth]="depth">

            <div class="cat-card" [class.root-card]="depth === 0" [class.sub-card]="depth === 1" [class.deep-card]="depth >= 2"
                 [style.--accent]="getCategoryColor(i, depth)">
              <div class="cat-card-left">
                <!-- Expand toggle -->
                <button class="expand-btn" *ngIf="cat.subCategories?.length > 0" (click)="toggleExpand(cat)">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"
                       [style.transform]="expandedIds.has(cat.publicId) ? 'rotate(90deg)' : 'rotate(0deg)'"
                       style="transition:transform 0.2s;">
                    <path d="m9 18 6-6-6-6"/>
                  </svg>
                </button>
                <div class="expand-spacer" *ngIf="!cat.subCategories?.length"></div>

                <!-- Category Icon -->
                <div class="cat-icon" [style.background]="'color-mix(in srgb, ' + getCategoryColor(i, depth) + ' 15%, transparent)'">
                  <span [style.color]="getCategoryColor(i, depth)" style="font-size:16px;">{{ getCategoryEmoji(cat.name) }}</span>
                </div>

                <!-- Info -->
                <div class="cat-info">
                  <span class="cat-name">{{ cat.name }}</span>
                  <div class="cat-meta">
                    <span class="cat-slug">
                      <svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
                      {{ cat.slug }}
                    </span>
                    <span class="sub-count" *ngIf="cat.subCategories?.length > 0">
                      {{ cat.subCategories.length }} subcategories
                    </span>
                    <span class="depth-badge" *ngIf="depth === 0">Root</span>
                    <span class="depth-badge sub" *ngIf="depth === 1">Sub</span>
                    <span class="depth-badge deep" *ngIf="depth >= 2">Nested</span>
                  </div>
                </div>
              </div>

              <!-- Actions -->
              <div class="cat-actions">
                <button class="action-btn add" (click)="openModal(cat.publicId)" title="Add Subcategory">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></svg>
                  <span>Add Sub</span>
                </button>
                <button class="action-btn edit" (click)="editCategory(cat)" title="Edit">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
                  <span>Edit</span>
                </button>
                <button class="action-btn delete" (click)="deleteCategory(cat.publicId)" title="Delete">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
                </button>
              </div>
            </div>

            <!-- Children -->
            <div class="children-wrapper" *ngIf="cat.subCategories?.length > 0 && expandedIds.has(cat.publicId)">
              <div class="tree-line"></div>
              <div class="children-content">
                <ng-container *ngTemplateOutlet="recursiveList; context:{ $implicit: cat.subCategories, depth: depth + 1 }"></ng-container>
              </div>
            </div>

          </div>
        </div>
      </ng-template>

      <!-- Modal -->
      <div class="modal-overlay" *ngIf="showModal" (click)="closeModal()">
        <div class="modal-box" (click)="$event.stopPropagation()">

          <div class="modal-header">
            <div class="modal-title-row">
              <div class="modal-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/>
                  <rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/>
                </svg>
              </div>
              <div>
                <h3 class="modal-title">{{ editingId ? 'Edit Category' : (parentCategoryName ? 'Add Subcategory' : 'New Root Category') }}</h3>
                <p class="modal-sub">{{ editingId ? 'Update the category details below' : 'Fill in the details to create a category' }}</p>
              </div>
            </div>
            <button class="modal-close" (click)="closeModal()">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18M6 6l12 12"/></svg>
            </button>
          </div>

          <div class="modal-body">
            <div class="parent-pill" *ngIf="parentCategoryName">
              <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
              Under: <strong>{{ parentCategoryName }}</strong>
            </div>

            <div class="field-group">
              <label class="field-label">Category Name <span class="required">*</span></label>
              <div class="field-input-wrap">
                <svg class="field-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/></svg>
                <input
                  class="field-input"
                  type="text"
                  [(ngModel)]="currentCategory.name"
                  placeholder="e.g., Men's Clothing, Electronics..."
                  autofocus
                />
              </div>
              <div class="field-hint" *ngIf="currentCategory.name">
                Slug will be: <code>{{ slugPreview }}</code>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="modal-cancel-btn" (click)="closeModal()">Cancel</button>
            <button class="modal-save-btn" (click)="saveCategory()" [disabled]="!currentCategory.name.trim()">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
              {{ editingId ? 'Save Changes' : 'Create Category' }}
            </button>
          </div>
        </div>
      </div>

    </app-admin-shell>
  `,
  styles: [`
    /* ─── Header ─────────────────────────────────────────────── */
    .page-header {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 32px; flex-wrap: wrap; gap: 16px;
    }
    .header-left { display: flex; align-items: center; gap: 16px; }
    .header-icon {
      width: 52px; height: 52px; border-radius: 14px;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 8px 24px rgba(99,102,241,0.35);
      flex-shrink: 0;
    }
    .page-title { font-size: 26px; font-weight: 800; color: var(--text-primary); margin: 0 0 4px; }
    .page-subtitle { color: var(--text-muted); font-size: 14px; margin: 0; }
    .header-right { display: flex; align-items: center; gap: 12px; }
    .stats-chip {
      display: flex; align-items: center; gap: 8px;
      background: rgba(99,102,241,0.08); border: 1px solid rgba(99,102,241,0.2);
      border-radius: 20px; padding: 6px 14px;
      font-size: 13px; font-weight: 600; color: #6366f1;
    }
    .stats-dot {
      width: 8px; height: 8px; border-radius: 50%;
      background: #6366f1; box-shadow: 0 0 8px rgba(99,102,241,0.6);
      animation: pulse 2s ease-in-out infinite;
    }
    @keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

    .add-btn {
      display: flex; align-items: center; gap: 8px;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      color: #fff; border: none; border-radius: 10px;
      padding: 10px 20px; font-size: 14px; font-weight: 600;
      cursor: pointer; transition: all 0.2s;
      box-shadow: 0 4px 14px rgba(99,102,241,0.35);
    }
    .add-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(99,102,241,0.45); }
    .add-btn:active { transform: translateY(0); }

    /* ─── Tree ─────────────────────────────────────────────────── */
    .tree-container { display: flex; flex-direction: column; gap: 10px; }
    .cat-group { display: flex; flex-direction: column; gap: 8px; }
    .cat-entry { display: flex; flex-direction: column; }

    .cat-card {
      display: flex; justify-content: space-between; align-items: center;
      background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.08);
      border-radius: 12px; padding: 14px 16px;
      transition: all 0.2s; cursor: default;
      border-left: 3px solid var(--accent, #6366f1);
      backdrop-filter: blur(10px);
    }
    .cat-card:hover {
      background: rgba(255,255,255,0.06);
      border-color: rgba(255,255,255,0.15);
      border-left-color: var(--accent, #6366f1);
      transform: translateX(2px);
      box-shadow: 0 4px 20px rgba(0,0,0,0.2);
    }
    .root-card { padding: 16px 20px; background: rgba(99,102,241,0.05); }
    .sub-card { background: rgba(255,255,255,0.02); }
    .deep-card { background: rgba(255,255,255,0.01); border-style: dashed; }

    .cat-card-left { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }

    .expand-btn {
      background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1);
      border-radius: 6px; width: 26px; height: 26px; display: flex; align-items: center;
      justify-content: center; cursor: pointer; color: var(--text-muted); flex-shrink: 0;
      transition: all 0.2s;
    }
    .expand-btn:hover { background: rgba(99,102,241,0.15); color: #6366f1; border-color: rgba(99,102,241,0.3); }
    .expand-spacer { width: 26px; flex-shrink: 0; }

    .cat-icon {
      width: 36px; height: 36px; border-radius: 8px;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }

    .cat-info { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
    .cat-name { font-weight: 700; color: #f1f5f9; font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .cat-meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .cat-slug {
      display: flex; align-items: center; gap: 4px;
      font-size: 11px; color: var(--text-muted);
      background: rgba(255,255,255,0.05); padding: 2px 8px; border-radius: 4px;
      font-family: monospace; border: 1px solid rgba(255,255,255,0.08);
    }
    .sub-count { font-size: 11px; color: #6366f1; font-weight: 600; }
    .depth-badge {
      font-size: 10px; font-weight: 700; letter-spacing: 0.05em; text-transform: uppercase;
      padding: 2px 7px; border-radius: 4px;
      background: rgba(99,102,241,0.15); color: #818cf8;
    }
    .depth-badge.sub { background: rgba(16,185,129,0.12); color: #34d399; }
    .depth-badge.deep { background: rgba(245,158,11,0.12); color: #fbbf24; }

    /* ─── Actions ─────────────────────────────────────────────── */
    .cat-actions { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
    .action-btn {
      display: flex; align-items: center; gap: 5px;
      border: 1px solid transparent; border-radius: 7px;
      padding: 6px 11px; font-size: 12px; font-weight: 600;
      cursor: pointer; transition: all 0.18s; white-space: nowrap;
    }
    .action-btn.add {
      background: rgba(16,185,129,0.1); color: #34d399; border-color: rgba(16,185,129,0.2);
    }
    .action-btn.add:hover { background: rgba(16,185,129,0.2); border-color: rgba(16,185,129,0.4); transform: translateY(-1px); }
    .action-btn.edit {
      background: rgba(99,102,241,0.1); color: #818cf8; border-color: rgba(99,102,241,0.2);
    }
    .action-btn.edit:hover { background: rgba(99,102,241,0.2); border-color: rgba(99,102,241,0.4); transform: translateY(-1px); }
    .action-btn.delete {
      background: rgba(239,68,68,0.1); color: #f87171; border-color: rgba(239,68,68,0.2);
      padding: 6px 9px;
    }
    .action-btn.delete:hover { background: rgba(239,68,68,0.2); border-color: rgba(239,68,68,0.4); transform: translateY(-1px); }

    /* ─── Children / Tree Lines ───────────────────────────────── */
    .children-wrapper { display: flex; margin-top: 6px; padding-left: 20px; }
    .tree-line {
      width: 2px; background: linear-gradient(180deg, rgba(99,102,241,0.4), transparent);
      border-radius: 2px; margin-right: 16px; flex-shrink: 0; min-height: 20px;
    }
    .children-content { flex: 1; display: flex; flex-direction: column; gap: 6px; }

    /* ─── Empty State ─────────────────────────────────────────── */
    .empty-state { text-align: center; padding: 80px 20px; }
    .empty-icon { color: var(--text-dim); margin-bottom: 16px; opacity: 0.4; }
    .empty-title { font-size: 20px; font-weight: 700; color: var(--text-secondary); margin-bottom: 8px; }
    .empty-sub { color: var(--text-muted); font-size: 14px; }

    /* ─── Modal ────────────────────────────────────────────────── */
    .modal-overlay {
      position: fixed; inset: 0; z-index: 2000;
      background: rgba(0,0,0,0.7); backdrop-filter: blur(6px);
      display: flex; align-items: center; justify-content: center; padding: 20px;
    }
    .modal-box {
      background: #0f1117; border: 1px solid rgba(255,255,255,0.1);
      border-radius: 20px; width: 480px; max-width: 100%;
      box-shadow: 0 25px 60px rgba(0,0,0,0.6), 0 0 0 1px rgba(99,102,241,0.1);
      animation: modalIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      overflow: hidden;
    }
    @keyframes modalIn {
      from { opacity: 0; transform: scale(0.95) translateY(10px); }
      to { opacity: 1; transform: scale(1) translateY(0); }
    }
    .modal-header {
      display: flex; justify-content: space-between; align-items: flex-start;
      padding: 24px 24px 20px;
      border-bottom: 1px solid rgba(255,255,255,0.07);
      background: linear-gradient(135deg, rgba(99,102,241,0.1), transparent);
    }
    .modal-title-row { display: flex; align-items: flex-start; gap: 14px; }
    .modal-icon {
      width: 44px; height: 44px; border-radius: 12px; flex-shrink: 0;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 6px 20px rgba(99,102,241,0.3);
    }
    .modal-title { font-size: 18px; font-weight: 800; color: #f1f5f9; margin: 0 0 4px; }
    .modal-sub { font-size: 13px; color: var(--text-muted); margin: 0; }
    .modal-close {
      background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1);
      border-radius: 8px; width: 32px; height: 32px; display: flex; align-items: center;
      justify-content: center; cursor: pointer; color: var(--text-muted); flex-shrink: 0;
      transition: all 0.2s;
    }
    .modal-close:hover { background: rgba(239,68,68,0.15); color: #f87171; border-color: rgba(239,68,68,0.3); }

    .modal-body { padding: 24px; display: flex; flex-direction: column; gap: 20px; }
    .parent-pill {
      display: flex; align-items: center; gap: 6px;
      background: rgba(99,102,241,0.1); border: 1px solid rgba(99,102,241,0.2);
      border-radius: 8px; padding: 10px 14px; font-size: 13px; color: #818cf8;
    }
    .parent-pill strong { color: #a5b4fc; }

    .field-group { display: flex; flex-direction: column; gap: 8px; }
    .field-label { font-size: 13px; font-weight: 700; color: #cbd5e1; letter-spacing: 0.02em; }
    .required { color: #f87171; margin-left: 2px; }
    .field-input-wrap { position: relative; display: flex; align-items: center; }
    .field-icon {
      position: absolute; left: 14px; color: var(--text-muted); pointer-events: none; flex-shrink: 0;
    }
    .field-input {
      width: 100%; padding: 13px 16px 13px 42px;
      background: rgba(255,255,255,0.04); border: 1.5px solid rgba(255,255,255,0.1);
      border-radius: 10px; font-size: 15px; font-weight: 500;
      color: #f1f5f9; outline: none; transition: all 0.2s;
      box-sizing: border-box;
    }
    .field-input::placeholder { color: rgba(148,163,184,0.5); font-weight: 400; }
    .field-input:focus {
      border-color: #6366f1; background: rgba(99,102,241,0.06);
      box-shadow: 0 0 0 3px rgba(99,102,241,0.15);
    }
    .field-hint { font-size: 12px; color: var(--text-muted); }
    .field-hint code {
      background: rgba(255,255,255,0.07); padding: 1px 6px; border-radius: 4px;
      color: #818cf8; font-family: monospace;
    }

    .modal-footer {
      display: flex; justify-content: flex-end; gap: 10px; align-items: center;
      padding: 16px 24px; border-top: 1px solid rgba(255,255,255,0.07);
      background: rgba(0,0,0,0.2);
    }
    .modal-cancel-btn {
      background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1);
      border-radius: 9px; padding: 10px 20px; font-size: 14px; font-weight: 600;
      color: var(--text-muted); cursor: pointer; transition: all 0.2s;
    }
    .modal-cancel-btn:hover { background: rgba(255,255,255,0.1); color: #f1f5f9; }
    .modal-save-btn {
      display: flex; align-items: center; gap: 8px;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      border: none; border-radius: 9px; padding: 10px 22px;
      font-size: 14px; font-weight: 700; color: #fff;
      cursor: pointer; transition: all 0.2s;
      box-shadow: 0 4px 14px rgba(99,102,241,0.35);
    }
    .modal-save-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(99,102,241,0.5); }
    .modal-save-btn:disabled { opacity: 0.4; cursor: not-allowed; transform: none; }
  `]
})
export class AdminCategoriesComponent implements OnInit {
  loading = false;
  categories: any[] = [];
  topLevelCategories: any[] = [];
  expandedIds = new Set<string>();
  totalCount = 0;

  showModal = false;
  editingId: string | null = null;
  parentCategoryName: string | null = null;
  currentCategory: { name: string, parentCategory?: { publicId: string } } = { name: '' };

  get slugPreview(): string {
    return this.currentCategory.name.toLowerCase().replace(/\s+/g, '-');
  }

  private categoryColors = ['#6366f1', '#8b5cf6', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#ec4899', '#3b82f6'];
  private categoryEmojis: Record<string, string> = {
    electronics: '⚡', fashion: '👗', mobiles: '📱', laptops: '💻',
    'home-kitchen': '🏠', kitchen: '🍳', sports: '⚽', books: '📚',
    toys: '🧸', beauty: '💄', health: '💊', automotive: '🚗',
    jewelry: '💎', food: '🍕', clothing: '👕', shoes: '👟'
  };

  constructor(private productService: ProductService, private toast: ToastService) {}

  ngOnInit(): void { this.loadCategories(); }

  loadCategories(): void {
    this.loading = true;
    this.productService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
        this.topLevelCategories = this.categories.filter(c => !c.parentCategory);
        this.totalCount = this.countAll(this.categories);
        // Auto-expand root categories
        this.topLevelCategories.forEach(c => this.expandedIds.add(c.publicId));
        this.loading = false;
      },
      error: () => { this.loading = false; this.toast.error('Failed to load categories'); }
    });
  }

  private countAll(list: any[]): number {
    return list.reduce((acc, c) => acc + 1 + (c.subCategories ? this.countAll(c.subCategories) : 0), 0);
  }

  toggleExpand(cat: any): void {
    if (this.expandedIds.has(cat.publicId)) {
      this.expandedIds.delete(cat.publicId);
    } else {
      this.expandedIds.add(cat.publicId);
    }
  }

  getCategoryColor(index: number, depth: number): string {
    return this.categoryColors[(index + depth) % this.categoryColors.length];
  }

  getCategoryEmoji(name: string): string {
    const key = name.toLowerCase().replace(/\s/g, '-');
    for (const k of Object.keys(this.categoryEmojis)) {
      if (key.includes(k)) return this.categoryEmojis[k];
    }
    return '📁';
  }

  openModal(parentId?: string): void {
    this.editingId = null;
    this.currentCategory = { name: '' };
    this.parentCategoryName = null;
    if (parentId) {
      this.currentCategory.parentCategory = { publicId: parentId };
      const parent = this.findCategoryByPublicId(this.categories, parentId);
      if (parent) this.parentCategoryName = parent.name;
    }
    this.showModal = true;
  }

  editCategory(cat: any): void {
    this.editingId = cat.publicId;
    this.currentCategory = { name: cat.name };
    this.parentCategoryName = null;
    if (cat.parentCategory?.publicId) {
      this.currentCategory.parentCategory = { publicId: cat.parentCategory.publicId };
      const parent = this.findCategoryByPublicId(this.categories, cat.parentCategory.publicId);
      if (parent) this.parentCategoryName = parent.name;
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
    const call = this.editingId
      ? this.productService.updateCategory(this.editingId, this.currentCategory)
      : this.productService.addCategory(this.currentCategory);

    call.subscribe({
      next: () => {
        this.toast.success(this.editingId ? 'Category updated!' : 'Category created!');
        this.closeModal();
        this.loadCategories();
      },
      error: () => this.toast.error(this.editingId ? 'Failed to update' : 'Failed to create')
    });
  }

  deleteCategory(publicId: string): void {
    if (confirm('Delete this category? Products linked to it may be affected.')) {
      this.productService.deleteCategory(publicId).subscribe({
        next: () => { this.toast.success('Category deleted'); this.loadCategories(); },
        error: () => this.toast.error('Failed to delete category')
      });
    }
  }

  private findCategoryByPublicId(list: any[], id: string): any {
    for (const cat of list) {
      if (cat.publicId === id) return cat;
      if (cat.subCategories?.length) {
        const found = this.findCategoryByPublicId(cat.subCategories, id);
        if (found) return found;
      }
    }
    return null;
  }
}
