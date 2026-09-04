import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../services/product.service';
import { ToastService } from '../../../services/toast.service';
import { AdminShellComponent } from '../admin-shell/admin-shell.component';

/** Mirrors the backend CategoryDTO &#8212; nested tree structure from API */
interface CategoryDTO {
  publicId: string;
  name: string;
  slug: string;
  parentPublicId: string | null;
  subCategories: CategoryDTO[];
}

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminShellComponent],
  template: `
    <app-admin-shell>

      <!-- ── Header ──────────────────────────────────────────────── -->
      <div class="page-header">
        <div class="header-left">
          <div class="header-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/>
              <rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/>
            </svg>
          </div>
          <div>
            <h1 class="page-title">Category Management</h1>
            <p class="page-subtitle">Organize and manage your product taxonomy tree</p>
          </div>
        </div>
        <div class="header-right">
          <div class="stats-chip">
            <span class="stats-dot"></span>
            {{ totalCount }} Categories
          </div>
          <button class="add-btn" (click)="openModal()">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></svg>
            Add Root Category
          </button>
        </div>
      </div>

      <!-- ── Loading ──────────────────────────────────────────────── -->
      <div class="loading-center" *ngIf="loading">
        <div class="spinner-ring"></div>
      </div>

      <!-- ── Empty State ──────────────────────────────────────────── -->
      <div *ngIf="!loading && topLevelCategories.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
            <rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/>
            <rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/>
          </svg>
        </div>
        <h3 class="empty-title">No categories yet</h3>
        <p class="empty-sub">Start by creating your first root category</p>
        <button class="add-btn" (click)="openModal()">+ Add Root Category</button>
      </div>

      <!-- ── Category Tree ────────────────────────────────────────── -->
      <!-- API already returns a nested tree &#8212; no client-side filtering needed -->
      <div class="tree-wrap" *ngIf="!loading && topLevelCategories.length > 0">
        <ng-container *ngTemplateOutlet="treeNode; context:{ $implicit: topLevelCategories, depth: 0 }"></ng-container>
      </div>

      <!-- ── Recursive Tree Template ──────────────────────────────── -->
      <ng-template #treeNode let-list let-depth="depth">
        <div class="node-group" [class.nested]="depth > 0">
          <div *ngFor="let cat of list; let i = index" class="node-entry">

            <div class="node-card"
                 [class.root-node]="depth === 0"
                 [class.sub-node]="depth === 1"
                 [class.deep-node]="depth >= 2"
                 [style.--accent]="getColor(i, depth)">

              <div class="node-left">
                <!-- Expand/Collapse toggle -->
                <button class="toggle-btn"
                        *ngIf="cat.subCategories?.length > 0"
                        (click)="toggleExpand(cat.publicId)"
                        [title]="expandedIds.has(cat.publicId) ? 'Collapse' : 'Expand'">
                  <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"
                       [style.transform]="expandedIds.has(cat.publicId) ? 'rotate(90deg)' : 'rotate(0deg)'"
                       style="transition: transform 0.22s ease;">
                    <path d="m9 18 6-6-6-6"/>
                  </svg>
                </button>
                <div class="toggle-spacer" *ngIf="!cat.subCategories?.length"></div>

                <!-- Emoji Icon -->
                <div class="cat-icon-wrap" [style.background]="'color-mix(in srgb,' + getColor(i,depth) + ' 14%, transparent)'">
                  <span style="font-size:17px; line-height:1;">{{ getEmoji(cat.name) }}</span>
                </div>

                <!-- Name & Meta -->
                <div class="node-info">
                  <span class="node-name">{{ cat.name }}</span>
                  <div class="node-meta">
                    <span class="slug-pill">
                      <svg xmlns="http://www.w3.org/2000/svg" width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
                      {{ cat.slug }}
                    </span>
                    <span class="child-count" *ngIf="cat.subCategories?.length > 0">
                      {{ cat.subCategories.length }} sub
                    </span>
                    <span class="level-badge root" *ngIf="depth === 0">Root</span>
                    <span class="level-badge sub"  *ngIf="depth === 1">Sub</span>
                    <span class="level-badge deep" *ngIf="depth >= 2">Nested</span>
                  </div>
                </div>
              </div>

              <!-- Action Buttons -->
              <div class="node-actions">
                <button class="act-btn add" (click)="openModal(cat)" title="Add Subcategory">
                  <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></svg>
                  <span>Add Sub</span>
                </button>
                <button class="act-btn edit" (click)="openEditModal(cat)" title="Edit">
                  <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
                  <span>Edit</span>
                </button>
                <button class="act-btn del"
                        (click)="confirmDelete(cat)"
                        title="Delete"
                        [disabled]="deletingId === cat.publicId">
                  <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
                </button>
              </div>
            </div>

            <!-- Children (collapsed by default for sub-nodes; roots are expanded) -->
            <div class="children-wrap" *ngIf="cat.subCategories?.length > 0 && expandedIds.has(cat.publicId)">
              <div class="tree-connector"></div>
              <div class="children-body">
                <ng-container *ngTemplateOutlet="treeNode; context:{ $implicit: cat.subCategories, depth: depth + 1 }"></ng-container>
              </div>
            </div>

          </div>
        </div>
      </ng-template>

      <!-- ── Delete Confirm Modal ─────────────────────────────────── -->
      <div class="overlay" *ngIf="showDeleteConfirm" (click)="cancelDelete()">
        <div class="confirm-box" (click)="$event.stopPropagation()">
          <div class="confirm-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#f87171" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
          </div>
          <h3 class="confirm-title">Delete "{{ categoryToDelete?.name }}"?</h3>
          <p class="confirm-body" *ngIf="deleteHasSubs">
            ⚠️ This will also delete <strong>{{ deleteSubCount }}</strong> subcategor{{ deleteSubCount === 1 ? 'y' : 'ies' }} recursively.
          </p>
          <p class="confirm-body" *ngIf="!deleteHasSubs">
            This action cannot be undone.
          </p>
          <div class="confirm-actions">
            <button class="cancel-btn" (click)="cancelDelete()">Cancel</button>
            <button class="delete-btn" (click)="executeDelete()">Yes, Delete</button>
          </div>
        </div>
      </div>

      <!-- ── Add / Edit Modal ─────────────────────────────────────── -->
      <div class="overlay" *ngIf="showModal" (click)="closeModal()">
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
                <h3 class="modal-title">{{ editingId ? 'Edit Category' : (parentName ? 'Add Subcategory' : 'New Root Category') }}</h3>
                <p class="modal-sub">{{ editingId ? 'Update the category name below' : 'Fill in the name to create' }}</p>
              </div>
            </div>
            <button class="modal-close" (click)="closeModal()">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18M6 6l12 12"/></svg>
            </button>
          </div>

          <div class="modal-body">
            <!-- Parent info pill -->
            <div class="parent-pill" *ngIf="parentName">
              <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
              Under: <strong>{{ parentName }}</strong>
            </div>

            <!-- Name field -->
            <div class="field-group">
              <label class="field-label">
                Category Name <span class="required">*</span>
              </label>
              <div class="field-wrap">
                <svg class="field-icon" xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/>
                  <rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/>
                </svg>
                <input
                  class="field-input"
                  type="text"
                  [(ngModel)]="formName"
                  placeholder="e.g., Men's Clothing, Smartphones..."
                  autofocus
                />
              </div>
              <!-- Live slug preview -->
              <div class="slug-preview" *ngIf="formName.trim()">
                Slug: <code>/{{ slugPreview }}</code>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="modal-cancel" (click)="closeModal()">Cancel</button>
            <button class="modal-save" (click)="save()" [disabled]="!formName.trim() || saving">
              <svg *ngIf="!saving" xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
              <span class="mini-spinner" *ngIf="saving"></span>
              {{ saving ? 'Saving...' : (editingId ? 'Save Changes' : 'Create Category') }}
            </button>
          </div>
        </div>
      </div>

    </app-admin-shell>
  `,
  styles: [`
    /* ── Header ──────────────────────────────────────────────────── */
    .page-header {
      display: flex; justify-content: space-between; align-items: center;
      margin-bottom: 32px; flex-wrap: wrap; gap: 16px;
    }
    .header-left { display: flex; align-items: center; gap: 16px; }
    .header-icon {
      width: 52px; height: 52px; border-radius: 14px; flex-shrink: 0;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 8px 24px rgba(99,102,241,0.35);
    }
    .page-title  { font-size: 26px; font-weight: 800; color: var(--text-primary); margin: 0 0 4px; }
    .page-subtitle { color: var(--text-muted); font-size: 14px; margin: 0; }
    .header-right { display: flex; align-items: center; gap: 12px; }

    .stats-chip {
      display: flex; align-items: center; gap: 8px; font-size: 13px;
      font-weight: 600; color: #6366f1;
      background: rgba(99,102,241,0.08); border: 1px solid rgba(99,102,241,0.2);
      border-radius: 20px; padding: 6px 14px;
    }
    .stats-dot {
      width: 8px; height: 8px; border-radius: 50%; background: #6366f1;
      box-shadow: 0 0 8px rgba(99,102,241,0.6);
      animation: pulse 2s ease-in-out infinite;
    }
    @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.4} }

    .add-btn {
      display: flex; align-items: center; gap: 8px;
      background: linear-gradient(135deg, #6366f1, #8b5cf6);
      color: #fff; border: none; border-radius: 10px;
      padding: 10px 20px; font-size: 14px; font-weight: 600;
      cursor: pointer; transition: all 0.2s;
      box-shadow: 0 4px 14px rgba(99,102,241,0.35);
    }
    .add-btn:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(99,102,241,0.45); }

    /* ── Loading ─────────────────────────────────────────────────── */
    .loading-center { display:flex; justify-content:center; align-items:center; padding:80px; }
    .spinner-ring {
      width: 40px; height: 40px; border-radius: 50%;
      border: 3px solid rgba(99,102,241,0.2);
      border-top-color: #6366f1;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    /* ── Empty State ─────────────────────────────────────────────── */
    .empty-state { text-align:center; padding:80px 20px; }
    .empty-icon { color: rgba(148,163,184,0.3); margin-bottom:16px; }
    .empty-title { font-size:20px; font-weight:700; color:var(--text-secondary); margin:0 0 8px; }
    .empty-sub { color:var(--text-muted); font-size:14px; margin:0 0 20px; }

    /* ── Tree ────────────────────────────────────────────────────── */
    .tree-wrap { display:flex; flex-direction:column; gap:10px; }
    .node-group { display:flex; flex-direction:column; gap:8px; }

    .node-card {
      display: flex; justify-content: space-between; align-items: center;
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(255,255,255,0.07);
      border-left: 3px solid var(--accent, #6366f1);
      border-radius: 12px; padding: 13px 16px;
      transition: all 0.2s; cursor: default;
    }
    .node-card:hover {
      background: rgba(255,255,255,0.06);
      border-color: rgba(255,255,255,0.14);
      border-left-color: var(--accent, #6366f1);
      transform: translateX(3px);
      box-shadow: 0 4px 20px rgba(0,0,0,0.18);
    }
    .root-node { padding: 15px 20px; background: rgba(99,102,241,0.05); }
    .sub-node  { background: rgba(255,255,255,0.025); }
    .deep-node { background: rgba(255,255,255,0.015); border-style: dashed; }

    .node-left { display:flex; align-items:center; gap:12px; flex:1; min-width:0; }

    .toggle-btn {
      width: 28px; height: 28px; flex-shrink:0;
      display: flex; align-items:center; justify-content:center;
      background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1);
      border-radius: 7px; cursor: pointer; color: var(--text-muted);
      transition: all 0.18s;
    }
    .toggle-btn:hover { background: rgba(99,102,241,0.15); color:#818cf8; border-color:rgba(99,102,241,0.3); }
    .toggle-spacer { width:28px; flex-shrink:0; }

    .cat-icon-wrap {
      width:36px; height:36px; border-radius:9px; flex-shrink:0;
      display:flex; align-items:center; justify-content:center;
    }

    .node-info  { display:flex; flex-direction:column; gap:4px; min-width:0; }
    .node-name  { font-weight:700; color:#f1f5f9; font-size:15px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
    .node-meta  { display:flex; align-items:center; gap:7px; flex-wrap:wrap; }

    .slug-pill {
      display:flex; align-items:center; gap:4px;
      font-size:11px; color:var(--text-muted); font-family:monospace;
      background:rgba(255,255,255,0.05); border:1px solid rgba(255,255,255,0.07);
      padding:2px 8px; border-radius:4px;
    }
    .child-count { font-size:11px; color:#6366f1; font-weight:700; }
    .level-badge {
      font-size:10px; font-weight:700; text-transform:uppercase;
      letter-spacing:.05em; padding:2px 7px; border-radius:4px;
    }
    .level-badge.root { background:rgba(99,102,241,0.15); color:#818cf8; }
    .level-badge.sub  { background:rgba(16,185,129,0.12); color:#34d399; }
    .level-badge.deep { background:rgba(245,158,11,0.12); color:#fbbf24; }

    /* ── Action Buttons ──────────────────────────────────────────── */
    .node-actions { display:flex; align-items:center; gap:6px; flex-shrink:0; }
    .act-btn {
      display:flex; align-items:center; gap:5px;
      border-radius:7px; padding:6px 11px; font-size:12px; font-weight:600;
      cursor:pointer; transition:all 0.18s; white-space:nowrap; border:1px solid transparent;
    }
    .act-btn.add  { background:rgba(16,185,129,0.1); color:#34d399; border-color:rgba(16,185,129,0.2); }
    .act-btn.add:hover  { background:rgba(16,185,129,0.2); border-color:rgba(16,185,129,0.4); transform:translateY(-1px); }
    .act-btn.edit { background:rgba(99,102,241,0.1); color:#818cf8; border-color:rgba(99,102,241,0.2); }
    .act-btn.edit:hover { background:rgba(99,102,241,0.2); border-color:rgba(99,102,241,0.4); transform:translateY(-1px); }
    .act-btn.del  { background:rgba(239,68,68,0.1); color:#f87171; border-color:rgba(239,68,68,0.2); padding:6px 9px; }
    .act-btn.del:hover  { background:rgba(239,68,68,0.2); border-color:rgba(239,68,68,0.4); transform:translateY(-1px); }
    .act-btn:disabled { opacity:0.4; cursor:not-allowed; transform:none !important; }

    /* ── Tree Lines ──────────────────────────────────────────────── */
    .children-wrap { display:flex; margin-top:6px; padding-left:22px; }
    .tree-connector {
      width: 2px; flex-shrink:0; margin-right:16px; border-radius:2px;
      background: linear-gradient(180deg, rgba(99,102,241,0.5), transparent);
      min-height: 20px;
    }
    .children-body { flex:1; display:flex; flex-direction:column; gap:6px; }

    /* ── Overlay ─────────────────────────────────────────────────── */
    .overlay {
      position:fixed; inset:0; z-index:2000;
      background:rgba(0,0,0,0.72); backdrop-filter:blur(6px);
      display:flex; align-items:center; justify-content:center; padding:20px;
    }

    /* ── Delete Confirm ──────────────────────────────────────────── */
    .confirm-box {
      background:#0f1117; border:1px solid rgba(239,68,68,0.25);
      border-radius:18px; width:400px; max-width:100%; padding:32px;
      text-align:center; animation:popIn 0.28s cubic-bezier(.16,1,.3,1);
      box-shadow: 0 25px 60px rgba(0,0,0,0.6), 0 0 0 1px rgba(239,68,68,0.1);
    }
    .confirm-icon {
      width:60px; height:60px; border-radius:50%;
      background:rgba(239,68,68,0.1); border:1px solid rgba(239,68,68,0.25);
      display:flex; align-items:center; justify-content:center; margin:0 auto 20px;
    }
    .confirm-title { font-size:20px; font-weight:800; color:#f1f5f9; margin:0 0 12px; }
    .confirm-body  { font-size:14px; color:var(--text-muted); margin:0 0 28px; line-height:1.6; }
    .confirm-body strong { color:#fbbf24; }
    .confirm-actions { display:flex; gap:12px; justify-content:center; }
    .cancel-btn {
      flex:1; padding:11px; border-radius:10px; font-size:14px; font-weight:600;
      background:rgba(255,255,255,0.05); border:1px solid rgba(255,255,255,0.1);
      color:var(--text-muted); cursor:pointer; transition:all 0.2s;
    }
    .cancel-btn:hover { background:rgba(255,255,255,0.1); color:#f1f5f9; }
    .delete-btn {
      flex:1; padding:11px; border-radius:10px; font-size:14px; font-weight:700;
      background:linear-gradient(135deg,#ef4444,#dc2626); border:none;
      color:#fff; cursor:pointer; transition:all 0.2s;
      box-shadow:0 4px 14px rgba(239,68,68,0.35);
    }
    .delete-btn:hover { transform:translateY(-1px); box-shadow:0 6px 20px rgba(239,68,68,0.5); }

    /* ── Add/Edit Modal ──────────────────────────────────────────── */
    .modal-box {
      background:#0f1117; border:1px solid rgba(255,255,255,0.1);
      border-radius:20px; width:480px; max-width:100%; overflow:hidden;
      box-shadow:0 25px 60px rgba(0,0,0,0.6), 0 0 0 1px rgba(99,102,241,0.1);
      animation:popIn 0.28s cubic-bezier(.16,1,.3,1);
    }
    @keyframes popIn {
      from { opacity:0; transform:scale(0.94) translateY(12px); }
      to   { opacity:1; transform:scale(1)    translateY(0); }
    }
    .modal-header {
      display:flex; justify-content:space-between; align-items:flex-start;
      padding:24px 24px 20px;
      border-bottom:1px solid rgba(255,255,255,0.07);
      background:linear-gradient(135deg,rgba(99,102,241,0.1),transparent);
    }
    .modal-title-row { display:flex; align-items:flex-start; gap:14px; }
    .modal-icon {
      width:44px; height:44px; border-radius:12px; flex-shrink:0;
      background:linear-gradient(135deg,#6366f1,#8b5cf6);
      display:flex; align-items:center; justify-content:center;
      box-shadow:0 6px 20px rgba(99,102,241,0.3);
    }
    .modal-title { font-size:18px; font-weight:800; color:#f1f5f9; margin:0 0 4px; }
    .modal-sub   { font-size:13px; color:var(--text-muted); margin:0; }
    .modal-close {
      width:32px; height:32px; border-radius:8px; flex-shrink:0;
      display:flex; align-items:center; justify-content:center;
      background:rgba(255,255,255,0.05); border:1px solid rgba(255,255,255,0.1);
      color:var(--text-muted); cursor:pointer; transition:all 0.2s;
    }
    .modal-close:hover { background:rgba(239,68,68,0.15); color:#f87171; border-color:rgba(239,68,68,0.3); }

    .modal-body { padding:24px; display:flex; flex-direction:column; gap:20px; }

    .parent-pill {
      display:flex; align-items:center; gap:7px;
      background:rgba(99,102,241,0.1); border:1px solid rgba(99,102,241,0.2);
      border-radius:8px; padding:10px 14px; font-size:13px; color:#818cf8;
    }
    .parent-pill strong { color:#a5b4fc; }

    .field-group { display:flex; flex-direction:column; gap:8px; }
    .field-label { font-size:13px; font-weight:700; color:#cbd5e1; letter-spacing:.02em; }
    .required    { color:#f87171; margin-left:2px; }

    .field-wrap  { position:relative; display:flex; align-items:center; }
    .field-icon  { position:absolute; left:14px; color:var(--text-muted); pointer-events:none; }
    .field-input {
      width:100%; padding:13px 16px 13px 42px; box-sizing:border-box;
      background:rgba(255,255,255,0.04); border:1.5px solid rgba(255,255,255,0.1);
      border-radius:10px; font-size:15px; font-weight:500; color:#f1f5f9;
      outline:none; transition:all 0.2s;
    }
    .field-input::placeholder { color:rgba(148,163,184,0.45); font-weight:400; }
    .field-input:focus {
      border-color:#6366f1; background:rgba(99,102,241,0.06);
      box-shadow:0 0 0 3px rgba(99,102,241,0.15);
    }
    .slug-preview { font-size:12px; color:var(--text-muted); }
    .slug-preview code {
      background:rgba(255,255,255,0.07); padding:1px 6px;
      border-radius:4px; color:#818cf8; font-family:monospace;
    }

    .modal-footer {
      display:flex; justify-content:flex-end; gap:10px; align-items:center;
      padding:16px 24px; border-top:1px solid rgba(255,255,255,0.07);
      background:rgba(0,0,0,0.2);
    }
    .modal-cancel {
      background:rgba(255,255,255,0.05); border:1px solid rgba(255,255,255,0.1);
      border-radius:9px; padding:10px 20px; font-size:14px; font-weight:600;
      color:var(--text-muted); cursor:pointer; transition:all 0.2s;
    }
    .modal-cancel:hover { background:rgba(255,255,255,0.1); color:#f1f5f9; }
    .modal-save {
      display:flex; align-items:center; gap:8px;
      background:linear-gradient(135deg,#6366f1,#8b5cf6); border:none;
      border-radius:9px; padding:10px 22px; font-size:14px; font-weight:700;
      color:#fff; cursor:pointer; transition:all 0.2s;
      box-shadow:0 4px 14px rgba(99,102,241,0.35);
    }
    .modal-save:hover:not(:disabled) { transform:translateY(-1px); box-shadow:0 6px 20px rgba(99,102,241,0.5); }
    .modal-save:disabled { opacity:0.4; cursor:not-allowed; transform:none; }
    .mini-spinner {
      width:14px; height:14px; border-radius:50%;
      border:2px solid rgba(255,255,255,0.3); border-top-color:#fff;
      animation:spin 0.7s linear infinite; flex-shrink:0;
    }
  `]
})
export class AdminCategoriesComponent implements OnInit {

  loading   = false;
  saving    = false;

  /** API returns pre-built nested tree &#8212; no client-side filtering needed */
  topLevelCategories: CategoryDTO[] = [];

  /** Flattened cache for O(1) lookups by publicId (e.g., find parent name) */
  private flatCategories: CategoryDTO[] = [];

  totalCount    = 0;
  expandedIds   = new Set<string>();
  deletingId: string | null = null;

  // ── Modal state ───────────────────────────────────────────────────
  showModal   = false;
  editingId: string | null = null;
  parentPublicId: string | null = null;
  parentName: string | null = null;
  formName    = '';

  // ── Delete confirm state ──────────────────────────────────────────
  showDeleteConfirm  = false;
  categoryToDelete: CategoryDTO | null = null;

  private readonly COLORS = [
    '#6366f1','#8b5cf6','#06b6d4','#10b981',
    '#f59e0b','#ef4444','#ec4899','#3b82f6'
  ];
  private readonly EMOJIS: Record<string, string> = {
    electronics:'&#9889;', fashion:'👗', mobiles:'📱', laptops:'💻',
    'home-kitchen':'🏠', kitchen:'🍳', sports:'⚽', books:'📚',
    toys:'🧸', beauty:'💄', health:'💊', automotive:'🚗',
    jewelry:'💎', food:'🍕', clothing:'👕', shoes:'👟',
    accessories:'🎒', furniture:'🛋️', gaming:'&#127918;', music:'🎵'
  };

  constructor(
    private productService: ProductService,
    private toast: ToastService
  ) {}

  ngOnInit(): void { this.loadCategories(); }

  // ── Slug preview (computed in TS &#8212; Angular templates don't allow regex) ──
  get slugPreview(): string {
    return this.formName.toLowerCase()
      .replace(/'/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '');
  }

  // ── Load ─────────────────────────────────────────────────────────
  loadCategories(): void {
    this.loading = true;
    this.productService.getCategories().subscribe({
      next: (tree: CategoryDTO[]) => {
        // API now returns nested tree directly &#8212; assign as-is
        this.topLevelCategories = tree;
        this.flatCategories = this.flatten(tree);
        this.totalCount = this.flatCategories.length;
        // Auto-expand root categories on first load
        tree.forEach(c => this.expandedIds.add(c.publicId));
        this.loading = false;
      },
      error: () => {
        this.toast.error('Failed to load categories');
        this.loading = false;
      }
    });
  }

  /** Flatten nested tree into a single array for fast lookups */
  private flatten(list: CategoryDTO[]): CategoryDTO[] {
    return list.reduce((acc, cat) => {
      acc.push(cat);
      if (cat.subCategories?.length) acc.push(...this.flatten(cat.subCategories));
      return acc;
    }, [] as CategoryDTO[]);
  }

  // ── Expand / Collapse ─────────────────────────────────────────────
  toggleExpand(publicId: string): void {
    if (this.expandedIds.has(publicId)) this.expandedIds.delete(publicId);
    else this.expandedIds.add(publicId);
  }

  // ── Color / Emoji helpers ─────────────────────────────────────────
  getColor(index: number, depth: number): string {
    return this.COLORS[(index + depth) % this.COLORS.length];
  }

  getEmoji(name: string): string {
    const key = name.toLowerCase().replace(/\s+/g, '-');
    for (const k of Object.keys(this.EMOJIS)) {
      if (key.includes(k)) return this.EMOJIS[k];
    }
    return '📁';
  }

  // ── Add modal (for subcategory: pass parent cat) ──────────────────
  openModal(parent?: CategoryDTO): void {
    this.editingId      = null;
    this.parentPublicId = parent?.publicId ?? null;
    this.parentName     = parent?.name ?? null;
    this.formName       = '';
    this.showModal      = true;
  }

  // ── Edit modal ────────────────────────────────────────────────────
  openEditModal(cat: CategoryDTO): void {
    this.editingId      = cat.publicId;
    this.parentPublicId = cat.parentPublicId;
    this.parentName     = cat.parentPublicId
      ? (this.flatCategories.find(c => c.publicId === cat.parentPublicId)?.name ?? null)
      : null;
    this.formName       = cat.name;
    this.showModal      = true;
  }

  closeModal(): void {
    this.showModal      = false;
    this.editingId      = null;
    this.parentPublicId = null;
    this.parentName     = null;
    this.formName       = '';
  }

  // ── Save (create or update) ───────────────────────────────────────
  save(): void {
    if (!this.formName.trim() || this.saving) return;
    this.saving = true;

    // Request body shape matches backend Category entity deserialization
    const body: { name: string; parentCategory?: { publicId: string } } = {
      name: this.formName.trim(),
      ...(this.parentPublicId ? { parentCategory: { publicId: this.parentPublicId } } : {})
    };

    const call = this.editingId
      ? this.productService.updateCategory(this.editingId, body)
      : this.productService.addCategory(body);

    call.subscribe({
      next: () => {
        this.toast.success(this.editingId ? 'Category updated!' : 'Category created!');
        this.closeModal();
        this.loadCategories();
        this.saving = false;
      },
      error: (err) => {
        const msg = err?.error?.message || (this.editingId ? 'Failed to update' : 'Failed to create');
        this.toast.error(msg);
        this.saving = false;
      }
    });
  }

  // ── Delete confirm helpers (getters avoid nullable comparisons in templates) ──
  get deleteHasSubs(): boolean {
    return (this.categoryToDelete?.subCategories?.length ?? 0) > 0;
  }
  get deleteSubCount(): number {
    return this.categoryToDelete?.subCategories?.length ?? 0;
  }

  // ── Delete with confirm ───────────────────────────────────────────
  confirmDelete(cat: CategoryDTO): void {
    this.categoryToDelete   = cat;
    this.showDeleteConfirm  = true;
  }

  cancelDelete(): void {
    this.categoryToDelete   = null;
    this.showDeleteConfirm  = false;
  }

  executeDelete(): void {
    if (!this.categoryToDelete) return;
    const id = this.categoryToDelete.publicId;
    this.deletingId        = id;
    this.showDeleteConfirm = false;

    this.productService.deleteCategory(id).subscribe({
      next: () => {
        this.toast.success('Category deleted (subtree removed)');
        this.deletingId = null;
        this.categoryToDelete = null;
        this.loadCategories();
      },
      error: () => {
        this.toast.error('Failed to delete category');
        this.deletingId = null;
      }
    });
  }
}
