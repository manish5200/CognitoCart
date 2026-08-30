import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { UserIdentityComponent } from '../../shared/user-identity/user-identity.component';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, UserIdentityComponent],
  styles: [`
    .admin-layout {
      display: flex;
      min-height: 100vh;
      background: radial-gradient(circle at 15% 50%, rgba(99, 102, 241, 0.08), transparent 50%),
                  radial-gradient(circle at 85% 30%, rgba(56, 189, 248, 0.08), transparent 50%),
                  var(--bg-base);
    }
    .admin-sidebar {
      width: 72px;
      flex-shrink: 0;
      position: fixed;
      top: var(--navbar-h);
      bottom: 0;
      left: 0;
      overflow-y: auto;
      overflow-x: hidden;
      background: rgba(4,6,15,0.7);
      backdrop-filter: blur(40px) saturate(150%);
      -webkit-backdrop-filter: blur(40px) saturate(150%);
      border-right: 1px solid rgba(255,255,255,0.08);
      z-index: 100;
      box-shadow: 1px 0 24px rgba(0,0,0,0.4);
      transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .admin-sidebar:hover {
      width: 260px;
    }
    .sidebar-inner {
      width: 260px;
      padding: 24px 0;
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-height: 100%;
    }
    .admin-main {
      margin-left: 72px;
      flex: 1;
      padding: 32px 40px;
      margin-top: var(--navbar-h);
      min-height: calc(100vh - var(--navbar-h));
      overflow-x: hidden;
    }
    .sidebar-section-title {
      font-size: 10px;
      font-weight: 800;
      letter-spacing: 0.15em;
      text-transform: uppercase;
      color: var(--text-muted);
      margin: 20px 0 8px 24px;
      transition: opacity 0.3s ease;
    }
    .admin-sidebar:not(:hover) .sidebar-section-title {
      opacity: 0;
    }
    .sidebar-link {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 24px;
      margin-right: 16px;
      border-radius: 0 12px 12px 0;
      font-size: 14px;
      font-weight: 600;
      color: var(--text-secondary);
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
    }
    .sidebar-text, .admin-sidebar .user-details {
      transition: opacity 0.3s ease;
    }
    .admin-sidebar:not(:hover) .sidebar-text,
    .admin-sidebar:not(:hover) .user-details {
      opacity: 0;
    }
    .sidebar-link::before {
      content: '';
      position: absolute;
      left: 0; top: 0; bottom: 0; width: 3px;
      background: var(--grad-brand);
      border-radius: 0 4px 4px 0;
      transform: scaleY(0);
      transition: transform 0.3s ease;
    }
    .sidebar-link:hover {
      color: var(--text-primary);
      background: rgba(255,255,255,0.04);
    }
    .sidebar-link.active {
      color: #fff;
      background: linear-gradient(90deg, rgba(99,102,241,0.15) 0%, transparent 100%);
      box-shadow: inset 1px 0 0 rgba(99,102,241,0.5);
    }
    .sidebar-link.active::before {
      transform: scaleY(1);
    }
    .sidebar-link-icon {
      font-size: 18px;
      filter: drop-shadow(0 2px 4px rgba(0,0,0,0.5));
    }
    .sidebar-link-badge {
      margin-left: auto;
      background: var(--danger);
      color: #fff;
      font-size: 11px;
      font-weight: 800;
      padding: 2px 8px;
      border-radius: var(--r-full);
      box-shadow: 0 0 12px rgba(239,68,68,0.5);
    }
    @media(max-width:900px){
      .admin-sidebar { 
        width: 260px;
        transform: translateX(-100%);
        transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      }
      .admin-sidebar.open {
        transform: translateX(0);
      }
      .admin-sidebar .sidebar-section-title {
        opacity: 1;
      }
      .admin-sidebar .sidebar-text {
        opacity: 1;
      }
      .admin-main { margin-left: 0; padding: 20px; }
      .admin-mobile-header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; }
    }
    @media(min-width:901px){
      .admin-mobile-header { display: none; }
    }
    .admin-menu-btn {
      background: transparent; border: none; color: var(--text-primary); cursor: pointer; padding: 4px; border-radius: var(--r);
    }
    .admin-menu-btn:hover { background: rgba(255,255,255,0.05); }
    .admin-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,0.6); backdrop-filter: blur(2px); z-index: 90;
      animation: fadeIn 0.3s ease-out forwards;
    }
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
  `],
  template: `
    <div class="admin-layout" [class.sidebar-open]="sidebarOpen">
      <div class="admin-overlay" *ngIf="sidebarOpen" (click)="toggleSidebar()"></div>
      <aside class="admin-sidebar" [class.open]="sidebarOpen">
        <div class="sidebar-inner">
          <div style="padding:0 24px 20px; border-bottom:1px solid rgba(255,255,255,0.08); margin-bottom:12px; display:flex; align-items:center; justify-content:space-between;">
            <app-user-identity
              *ngIf="auth.currentUser$ | async as user"
              [name]="user.name"
              [showDetails]="true"
              subtitle="ADMIN PANEL"
              size="md">
            </app-user-identity>
          </div>

          <span class="sidebar-section-title">Overview</span>
          <a routerLink="/admin" [routerLinkActiveOptions]="{exact:true}" routerLinkActive="active" class="sidebar-link">
            <svg class="sidebar-link-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;"><rect width="7" height="9" x="3" y="3" rx="1"/><rect width="7" height="5" x="14" y="3" rx="1"/><rect width="7" height="9" x="14" y="12" rx="1"/><rect width="7" height="5" x="3" y="16" rx="1"/></svg> <span class="sidebar-text">Dashboard</span>
          </a>
          <a routerLink="/admin/intelligence" routerLinkActive="active" class="sidebar-link">
            <svg class="sidebar-link-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;"><path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/></svg> <span class="sidebar-text">BI Intelligence</span>
          </a>

          <span class="sidebar-section-title">Operations</span>
          <a routerLink="/admin/orders" routerLinkActive="active" class="sidebar-link">
            <svg class="sidebar-link-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;"><path d="m21.64 3.64-1.28-1.28A2 2 0 0 0 18.94 2h-13.9a2 2 0 0 0-1.42.59L2.36 3.86a2 2 0 0 0-.36.78v15.36A2 2 0 0 0 4 22h16a2 2 0 0 0 2-2V4.64a2 2 0 0 0-.36-.78"/><path d="m22 6-20 0"/><path d="M12 12v.01"/></svg> <span class="sidebar-text">All Orders</span>
          </a>
          <a routerLink="/admin/returns" routerLinkActive="active" class="sidebar-link">
            <svg class="sidebar-link-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;"><path d="M3 7v6h6"/><path d="M21 17v-6h-6"/><path d="M21 11.2A9 9 0 0 0 4.1 7L3 8"/><path d="M3 12.8A9 9 0 0 0 19.9 17l1-1"/></svg> <span class="sidebar-text">Returns Queue</span>
            <span *ngIf="pendingReturnsCount > 0" class="sidebar-link-badge sidebar-text">{{pendingReturnsCount}}</span>
          </a>
          <a routerLink="/admin/kyc" routerLinkActive="active" class="sidebar-link">
            <svg class="sidebar-link-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><polyline points="16 11 18 13 22 9"/></svg> <span class="sidebar-text">Seller KYC</span>
            <span *ngIf="pendingKycCount > 0" class="sidebar-link-badge sidebar-text">{{pendingKycCount}}</span>
          </a>
          <a routerLink="/admin/categories" routerLinkActive="active" class="sidebar-link">
            <svg class="sidebar-link-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;"><rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/></svg> <span class="sidebar-text">Categories</span>
          </a>
          <a routerLink="/admin/reviews" routerLinkActive="active" class="sidebar-link">
            <svg class="sidebar-link-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;"><path d="M14 9a2 2 0 0 1-2 2H6l-4 4V4c0-1.1.9-2 2-2h8a2 2 0 0 1 2 2v5Z"/><path d="M18 9h2a2 2 0 0 1 2 2v11l-4-4h-6a2 2 0 0 1-2-2v-1"/></svg> <span class="sidebar-text">Moderation</span>
          </a>

          <span class="sidebar-section-title">Marketing</span>
          <a routerLink="/admin/coupons" routerLinkActive="active" class="sidebar-link">
            <svg class="sidebar-link-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/><line x1="7" y1="7" x2="7.01" y2="7"/></svg> <span class="sidebar-text">Coupon Engine</span>
          </a>
          <a routerLink="/admin/sales" routerLinkActive="active" class="sidebar-link">
            <svg class="sidebar-link-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;"><path d="M11 2L2 14h9l-1 8 9-12h-9z"/></svg> <span class="sidebar-text">Flash Sales</span>
          </a>

          <span class="sidebar-section-title">System</span>
          <a routerLink="/admin/webhooks" routerLinkActive="active" class="sidebar-link">
            <svg class="sidebar-link-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg> <span class="sidebar-text">Webhook DLQ</span>
          </a>

          <div style="margin-top:auto; padding:16px 24px 0; border-top:1px solid rgba(255,255,255,0.06);">
            <a routerLink="/" class="sidebar-link" style="margin-right:0;">
              <svg class="sidebar-link-icon" xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0;"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg> <span class="sidebar-text">Back to Store</span>
            </a>
          </div>
        </div>
      </aside>

      <main class="admin-main">
        <div class="admin-mobile-header">
          <button class="admin-menu-btn" (click)="toggleSidebar()">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:24px;height:24px;"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" /></svg>
          </button>
          <div style="font-weight:700; color:var(--text-primary); font-size:16px;">Admin Panel</div>
        </div>
        <ng-content></ng-content>
      </main>
    </div>
  `
})
export class AdminShellComponent {
  @Input() pendingReturnsCount = 0;
  @Input() pendingKycCount = 0;
  sidebarOpen = false;

  constructor(public auth: AuthService) {}

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }
}
