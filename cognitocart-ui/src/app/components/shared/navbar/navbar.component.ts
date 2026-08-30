import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../services/auth.service';
import { CartService } from '../../../services/cart.service';
import { NotificationService } from '../../../services/notification.service';
import { ToastService } from '../../../services/toast.service';
import { UserIdentityComponent } from '../../shared/user-identity/user-identity.component';

@Component({
// ... (imports remain the same above, I am replacing from CartService down to menuOpen)

  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, UserIdentityComponent],
  template: `
    <!-- Verification Banner -->
    <div *ngIf="(auth.currentUser$ | async) as user">
      <div *ngIf="user.emailVerified === false" class="verification-banner">
        <span class="verification-text">
          <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
          Your email address is not verified. You cannot checkout until it is verified.
        </span>
        <button class="btn btn-primary btn-sm verify-btn" (click)="triggerVerification(user.email)" [disabled]="isSendingOtp">
          {{ isSendingOtp ? 'Sending...' : 'Verify Now' }}
        </button>
      </div>
    </div>
    
    <nav class="navbar" [class.has-banner]="(auth.currentUser$ | async)?.emailVerified === false">
      <!-- Logo -->
      <a routerLink="/" class="nav-logo">
        <span class="logo-cognito">Cognito</span><span class="logo-cart">Cart</span><span class="logo-dot">.</span>
      </a>

      <!-- AI Search (only on non-dashboard pages) -->
      <div class="nav-search" *ngIf="!isDashboard">
        <input
          type="text"
          class="nav-search-input"
          [(ngModel)]="query"
          (keyup.enter)="search()"
          placeholder="✨ Ask AI — try 'earphones for gym'..."
        />
        <button class="nav-search-btn" (click)="search()" aria-label="Search">✨</button>
      </div>

      <!-- Right Actions -->
      <div class="nav-links">
        <ng-container *ngIf="!(auth.currentUser$ | async)">
          <a routerLink="/products" class="nav-link" [class.active]="currentPath === '/products'">
            <span class="nav-link-icon" style="display:flex;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="20" height="20"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007ZM8.625 10.5a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm7.5 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" /></svg></span> Shop
          </a>
          <a routerLink="/login" class="nav-link">
            <span class="nav-link-icon" style="display:flex;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="20" height="20"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z" /></svg></span> Sign In
          </a>
          <a routerLink="/register" class="btn btn-primary btn-sm" style="margin-left:4px; font-weight:600; letter-spacing: 0.5px;">
            Get Started
          </a>
        </ng-container>

        <ng-container *ngIf="auth.currentUser$ | async as user">

          <!-- Public nav links (hidden for admin) -->
          <ng-container *ngIf="user.role !== 'ROLE_ADMIN'">
            <div class="nav-user-dropdown" (click)="$event.stopPropagation()" style="margin-right: 4px;">
              <a class="nav-link" (click)="categoriesOpen = !categoriesOpen; menuOpen = false; notifOpen = false; cartOpen = false">
                <span class="nav-link-icon" style="display:flex;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="20" height="20"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-2.25Z" /></svg></span> Categories
              </a>
              <div class="nav-user-menu" style="width: 200px; right: auto; left: 0;" *ngIf="categoriesOpen">
                <div class="nav-menu-item" routerLink="/products" [queryParams]="{category:'Electronics'}" (click)="closeMenu()">Electronics</div>
                <div class="nav-menu-item" routerLink="/products" [queryParams]="{category:'Fashion'}" (click)="closeMenu()">Fashion</div>
                <div class="nav-menu-item" routerLink="/products" [queryParams]="{category:'Home'}" (click)="closeMenu()">Home</div>
                <div class="nav-menu-item" routerLink="/products" [queryParams]="{category:'Beauty'}" (click)="closeMenu()">Beauty</div>
                <div class="nav-menu-divider"></div>
                <div class="nav-menu-item" routerLink="/products" (click)="closeMenu()" style="font-weight: 600; color: var(--primary);">View All Categories</div>
              </div>
            </div>

            <a routerLink="/products" class="nav-link" [class.active]="currentPath.startsWith('/product')">
              <span class="nav-link-icon" style="display:flex;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="20" height="20"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5V6a3.75 3.75 0 1 0-7.5 0v4.5m11.356-1.993 1.263 12c.07.665-.45 1.243-1.119 1.243H4.25a1.125 1.125 0 0 1-1.12-1.243l1.264-12A1.125 1.125 0 0 1 5.513 7.5h12.974c.576 0 1.059.435 1.119 1.007ZM8.625 10.5a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm7.5 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Z" /></svg></span> Shop
            </a>

            <!-- Wishlist (customer only) -->
            <a *ngIf="user.role === 'ROLE_CUSTOMER'" routerLink="/wishlist" class="nav-link" [class.active]="currentPath === '/wishlist'">
              <span class="nav-link-icon" style="display:flex; color: #f43f5e;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="20" height="20"><path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" /></svg></span>
            </a>

            <!-- Cart (non-admin) -->
            <div class="nav-cart-wrapper" (click)="$event.stopPropagation()">
              <div class="nav-link" (click)="cartOpen = !cartOpen; notifOpen = false; menuOpen = false" style="position:relative; cursor:pointer;" [class.active]="currentPath === '/cart'">
                <span class="nav-link-icon" style="display:flex;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="20" height="20"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z" /></svg></span>
                <span *ngIf="cartCount > 0" class="nav-cart-badge">{{ cartCount }}</span>
              </div>
              
              <!-- Mini Cart Slide-out -->
              <div class="nav-user-menu mini-cart-menu" *ngIf="cartOpen">
                <div style="padding:16px; border-bottom:1px solid var(--border-subtle); display:flex; justify-content:space-between; align-items:center;">
                  <span style="font-weight:800; font-size:16px; color:var(--text-primary);">Your Cart ({{cartCount}})</span>
                  <span style="cursor:pointer; color:var(--text-muted); font-size:18px;" (click)="cartOpen = false">×</span>
                </div>
                
                <div style="max-height:350px; overflow-y:auto; padding:8px 0;">
                  <div *ngIf="!cartItems?.length" style="padding:32px 16px; text-align:center;">
                    <div style="font-size:32px; margin-bottom:8px;">🛒</div>
                    <div style="color:var(--text-primary); font-weight:600;">Your cart is empty</div>
                    <div style="font-size:13px; color:var(--text-muted); margin-top:4px;">Add some items to get started.</div>
                  </div>
                  
                  <div *ngFor="let item of cartItems" class="mini-cart-item">
                    <img [src]="item.productImageUrl || item.imageUrl || 'https://via.placeholder.com/60'" class="mc-img" />
                    <div class="mc-info">
                      <div class="mc-name">{{item.productName}}</div>
                      <div class="mc-var">{{item.variantInfo || item.color || item.size || 'Standard'}}</div>
                      <div class="mc-qty">Qty: {{item.quantity}}</div>
                    </div>
                    <div class="mc-price">₹{{(item.price * item.quantity) | number:'1.0-0'}}</div>
                    <button class="mc-remove" (click)="removeCartItem(item)">🗑️</button>
                  </div>
                </div>
                
                <div *ngIf="cartItems?.length" style="padding:16px; border-top:1px solid var(--border-subtle); background:var(--bg-glass);">
                  <div style="display:flex; justify-content:space-between; margin-bottom:16px; font-weight:700; font-size:16px;">
                    <span>Subtotal</span>
                    <span style="color:var(--primary);">₹{{cartTotal | number:'1.0-0'}}</span>
                  </div>
                  <button class="btn btn-primary btn-block" style="width:100%; margin-bottom:8px;" routerLink="/checkout" (click)="cartOpen = false">Checkout</button>
                  <button class="btn btn-secondary btn-block" style="width:100%;" routerLink="/cart" (click)="cartOpen = false">View Cart</button>
                </div>
              </div>
            </div>
          </ng-container>

          <!-- Admin quick-access links -->
          <ng-container *ngIf="user.role === 'ROLE_ADMIN'">
            <a routerLink="/admin" class="nav-link" [class.active]="currentPath === '/admin'">
              <span class="nav-link-icon" style="display:flex;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="20" height="20"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 6a7.5 7.5 0 1 0 7.5 7.5h-7.5V6Z" /><path stroke-linecap="round" stroke-linejoin="round" d="M13.5 10.5H21A7.5 7.5 0 0 0 13.5 3v7.5Z" /></svg></span> Dashboard
            </a>
            <a routerLink="/admin/kyc" class="nav-link" [class.active]="currentPath === '/admin/kyc'">
              <span class="nav-link-icon" style="display:flex;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="20" height="20"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75 11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 0 1-1.043 3.296 3.745 3.745 0 0 1-3.296 1.043A3.745 3.745 0 0 1 12 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 0 1-3.296-1.043 3.745 3.745 0 0 1-1.043-3.296A3.745 3.745 0 0 1 3 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 0 1 1.043-3.296 3.746 3.746 0 0 1 3.296-1.043A3.746 3.746 0 0 1 12 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 0 1 3.296 1.043 3.746 3.746 0 0 1 1.043 3.296A3.745 3.745 0 0 1 21 12Z" /></svg></span> KYC
            </a>
            <a routerLink="/admin/returns" class="nav-link" [class.active]="currentPath === '/admin/returns'">
              <span class="nav-link-icon" style="display:flex;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="20" height="20"><path stroke-linecap="round" stroke-linejoin="round" d="M9 15 3 9m0 0 6-6M3 9h12a6 6 0 0 1 0 12h-3" /></svg></span> Returns
            </a>
          </ng-container>

          <!-- Notifications Dropdown -->
          <div class="nav-user-dropdown" (click)="$event.stopPropagation()" style="margin-right: 12px;">
            <div class="nav-link" (click)="notifOpen = !notifOpen; menuOpen = false; cartOpen = false" style="position:relative; cursor:pointer;">
              <span class="nav-link-icon" style="display:flex;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="20" height="20"><path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" /></svg></span>
              <span *ngIf="unreadCount > 0" class="nav-cart-badge" style="right: -4px;">{{ unreadCount }}</span>
            </div>
            
            <div class="nav-user-menu" style="width: 320px; right: -50px; z-index: 1000;" *ngIf="notifOpen">
              <div style="padding:12px; border-bottom:1px solid var(--border-subtle); display:flex; justify-content:space-between; align-items:center;">
                <span style="font-weight:700; color:var(--text-primary);">Notifications</span>
                <span *ngIf="unreadCount > 0" style="font-size:12px; color:var(--brand); cursor:pointer;" (click)="markAllRead()">Mark all read</span>
              </div>
              <div style="max-height:350px; overflow-y:auto; padding:8px 0;">
                <div *ngIf="notifications.length === 0" style="padding:16px; text-align:center; color:var(--text-muted); font-size:13px;">
                  No notifications yet.
                </div>
                <div *ngFor="let n of notifications" 
                     style="padding:12px 16px; border-bottom:1px solid var(--border-subtle); cursor:pointer; transition:0.2s;"
                     [style.background]="n.read ? 'transparent' : 'rgba(108,92,231,0.05)'"
                     (click)="markRead(n)">
                  <div style="display:flex; justify-content:space-between; margin-bottom:4px;">
                    <span style="font-size:13px; font-weight:600; color:var(--text-primary);">{{n.title}}</span>
                    <span *ngIf="!n.read" style="width:8px; height:8px; border-radius:50%; background:var(--brand); flex-shrink: 0; margin-top:4px;"></span>
                  </div>
                  <div style="font-size:12px; color:var(--text-muted); line-height:1.4;">{{n.message}}</div>
                  <div style="font-size:10px; color:var(--text-muted); margin-top:6px;">{{n.createdAt | date:'short'}}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- User Avatar + Dropdown -->
          <div class="nav-user-dropdown" (click)="$event.stopPropagation()">
            <div class="nav-avatar" (click)="menuOpen = !menuOpen; notifOpen = false; cartOpen = false" style="background: transparent; box-shadow: none; padding: 0;">
              <app-user-identity
                [name]="user.name"
                [showDetails]="false"
                size="sm">
              </app-user-identity>
            </div>

            <div class="nav-user-menu" *ngIf="menuOpen">
              <!-- User info header -->
              <div style="padding:12px; border-bottom:1px solid var(--border-subtle); margin-bottom:6px;">
                <div style="font-size:14px; font-weight:700; color:var(--text-primary);">{{ user.name }}</div>
                <div style="font-size:12px; color:var(--text-muted);">{{ user.email }}</div>
                <span class="badge" [class]="roleBadge(user.role)" style="margin-top:6px;">
                  {{ roleLabel(user.role) }}
                </span>
              </div>

              <!-- Customer links -->
              <ng-container *ngIf="user.role === 'ROLE_CUSTOMER'">
                <div class="nav-menu-item" routerLink="/dashboard" (click)="closeMenu()">
                  <span class="nav-menu-item-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 6a7.5 7.5 0 1 0 7.5 7.5h-7.5V6Z" /><path stroke-linecap="round" stroke-linejoin="round" d="M13.5 10.5H21A7.5 7.5 0 0 0 13.5 3v7.5Z" /></svg></span> My Dashboard
                </div>
                <div class="nav-menu-item" routerLink="/orders" (click)="closeMenu()">
                  <span class="nav-menu-item-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M21 7.5l-9-5.25L3 7.5m18 0l-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" /></svg></span> My Orders
                </div>
                <div class="nav-menu-item" routerLink="/wishlist" (click)="closeMenu()">
                  <span class="nav-menu-item-icon" style="color:#f43f5e;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12Z" /></svg></span> Wishlist
                </div>
                <div class="nav-menu-item" routerLink="/profile" (click)="closeMenu()">
                  <span class="nav-menu-item-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M17.982 18.725A7.488 7.488 0 0 0 12 15.75a7.488 7.488 0 0 0-5.982 2.975m11.963 0a9 9 0 1 0-11.963 0m11.963 0A8.966 8.966 0 0 1 12 21a8.966 8.966 0 0 1-5.982-2.275M15 9.75a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" /></svg></span> Profile & Addresses
                </div>
              </ng-container>

              <!-- Seller links -->
              <ng-container *ngIf="user.role === 'ROLE_SELLER'">
                <div class="nav-menu-item" routerLink="/seller" (click)="closeMenu()">
                  <span class="nav-menu-item-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 6a7.5 7.5 0 1 0 7.5 7.5h-7.5V6Z" /><path stroke-linecap="round" stroke-linejoin="round" d="M13.5 10.5H21A7.5 7.5 0 0 0 13.5 3v7.5Z" /></svg></span> Dashboard
                </div>
                <div class="nav-menu-item" routerLink="/seller/products" (click)="closeMenu()">
                  <span class="nav-menu-item-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M21 7.5l-9-5.25L3 7.5m18 0l-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" /></svg></span> My Products
                </div>
                <div class="nav-menu-item" routerLink="/seller/orders" (click)="closeMenu()">
                  <span class="nav-menu-item-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z" /></svg></span> Orders
                </div>
                <div class="nav-menu-item" routerLink="/seller/analytics" (click)="closeMenu()">
                  <span class="nav-menu-item-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z" /></svg></span> Analytics
                </div>
                <div class="nav-menu-item" routerLink="/seller/sales" (click)="closeMenu()">
                  <span class="nav-menu-item-icon" style="color:#f59e0b;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="m3.75 13.5 10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75Z" /></svg></span> Flash Sales
                </div>
              </ng-container>

              <!-- Admin links -->
              <ng-container *ngIf="user.role === 'ROLE_ADMIN'">
                <div class="nav-menu-item" routerLink="/admin" (click)="closeMenu()">
                  <span class="nav-menu-item-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 6a7.5 7.5 0 1 0 7.5 7.5h-7.5V6Z" /><path stroke-linecap="round" stroke-linejoin="round" d="M13.5 10.5H21A7.5 7.5 0 0 0 13.5 3v7.5Z" /></svg></span> Command Center
                </div>
                <div class="nav-menu-item" routerLink="/admin/intelligence" (click)="closeMenu()">
                  <span class="nav-menu-item-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M8.25 3v1.5M4.5 8.25H3m18 0h-1.5M4.5 12H3m18 0h-1.5m-15 3.75H3m18 0h-1.5M8.25 19.5V21M12 3v1.5m0 15V21m3.75-18v1.5m0 15V21m-9-1.5h10.5a2.25 2.25 0 0 0 2.25-2.25V6.75a2.25 2.25 0 0 0-2.25-2.25H6.75A2.25 2.25 0 0 0 4.5 6.75v10.5a2.25 2.25 0 0 0 2.25 2.25Zm.75-12h9v9h-9v-9Z" /></svg></span> BI Intelligence
                </div>
                <div class="nav-menu-item" routerLink="/admin/coupons" (click)="closeMenu()">
                  <span class="nav-menu-item-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M16.5 6v.75m0 3v.75m0 3v.75m0 3V18m-9-5.25h5.25M7.5 15h3M3.375 5.25c-.621 0-1.125.504-1.125 1.125v3.026a2.999 2.999 0 0 1 0 5.198v3.026c0 .621.504 1.125 1.125 1.125h17.25c.621 0 1.125-.504 1.125-1.125v-3.026a2.999 2.999 0 0 1 0-5.198V6.375c0-.621-.504-1.125-1.125-1.125H3.375Z" /></svg></span> Coupons
                </div>
                <div class="nav-menu-item" routerLink="/admin/sales" (click)="closeMenu()">
                  <span class="nav-menu-item-icon" style="color:#f59e0b;"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="m3.75 13.5 10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75Z" /></svg></span> Flash Sales
                </div>
                <div class="nav-menu-item" routerLink="/admin/webhooks" (click)="closeMenu()">
                  <span class="nav-menu-item-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3Z" /></svg></span> Webhooks DLQ
                </div>
              </ng-container>

              <div class="nav-menu-divider"></div>
              <div class="nav-menu-item danger" (click)="logout()">
                <span class="nav-menu-item-icon"><svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" width="18" height="18"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0 0 13.5 3h-6a2.25 2.25 0 0 0-2.25 2.25v13.5A2.25 2.25 0 0 0 7.5 21h6a2.25 2.25 0 0 0 2.25-2.25V15M12 9l-3 3m0 0 3 3m-3-3h12.75" /></svg></span> Sign Out
              </div>
            </div>
          </div>
        </ng-container>
      </div>
    </nav>
  `,
  styles: [`
    .verification-banner {
      background: rgba(239, 68, 68, 0.1);
      border-bottom: 1px solid rgba(239, 68, 68, 0.2);
      padding: 10px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      color: #ef4444;
      font-size: 13px;
      font-weight: 500;
    }
    .verification-text {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .verify-btn {
      margin: 0;
      padding: 6px 12px;
      font-size: 12px;
      height: auto;
      background: #ef4444;
      border-color: #ef4444;
      box-shadow: none;
    }
    .verify-btn:hover { background: #dc2626; border-color: #dc2626; }
    
    .nav-cart-wrapper { position: relative; }
    .mini-cart-menu { width: 360px; right: -80px; z-index: 1000; padding: 0 !important; }
    .mini-cart-item { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-bottom: 1px solid rgba(255,255,255,0.03); transition: 0.2s; }
    .mini-cart-item:hover { background: rgba(255,255,255,0.02); }
    .mc-img { width: 48px; height: 48px; object-fit: cover; border-radius: 6px; background: rgba(255,255,255,0.05); }
    .mc-info { flex: 1; min-width: 0; }
    .mc-name { font-size: 13px; font-weight: 600; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 2px; }
    .mc-var { font-size: 11px; color: var(--text-muted); margin-bottom: 4px; }
    .mc-qty { font-size: 11px; color: var(--text-dim); font-weight: 500; }
    .mc-price { font-weight: 700; color: var(--primary); font-size: 14px; }
    .mc-remove { background: transparent; border: none; cursor: pointer; color: var(--danger); opacity: 0.7; transition: 0.2s; font-size: 14px; }
    .mc-remove:hover { opacity: 1; transform: scale(1.1); }
    @media (max-width: 600px) {
      .mini-cart-menu { position: fixed; top: 70px; right: 10px; left: 10px; width: auto; max-width: none; }
    }
  `]
})
export class NavbarComponent implements OnInit {
  query = '';
  menuOpen = false;
  notifOpen = false;
  cartOpen = false;
  categoriesOpen = false;
  currentPath = '/';
  cartCount = 0;
  cartTotal = 0;
  cartItems: any[] = [];
  unreadCount = 0;
  notifications: any[] = [];
  isSendingOtp = false;

  get isDashboard(): boolean {
    return this.currentPath.startsWith('/admin') || this.currentPath.startsWith('/seller');
  }

  constructor(
    public auth: AuthService, 
    public cartService: CartService, 
    private router: Router,
    private notifService: NotificationService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe((e: any) => {
      this.currentPath = e.urlAfterRedirects;
      this.closeMenu();
    });
    this.currentPath = this.router.url;

    this.cartService.cart$.subscribe(cart => {
      this.cartCount = cart?.items?.length ?? 0;
      this.cartTotal = cart?.totalAmount ?? 0;
      this.cartItems = cart?.items ?? [];
    });

    this.auth.currentUser$.subscribe(user => {
      if (user) {
        this.loadNotifications();
      }
    });

    this.notifService.notificationsUpdated$.subscribe(() => {
      this.loadNotifications();
    });
  }

  @HostListener('document:click')
  onDocClick(): void { this.closeMenu(); }

  search(): void {
    if (this.query.trim()) {
      this.router.navigate(['/products'], { queryParams: { q: this.query.trim() } });
    }
  }

  closeMenu(): void { this.menuOpen = false; this.notifOpen = false; this.cartOpen = false; this.categoriesOpen = false; }

  logout(): void {
    this.menuOpen = false;
    this.notifOpen = false;
    this.auth.logoutAndRedirect();
  }

  loadNotifications(): void {
    this.notifService.getNotifications().subscribe({
      next: (res) => {
        this.unreadCount = res.unreadCount || 0;
        this.notifications = res.notifications?.content || [];
      },
      error: () => {}
    });
  }

  markRead(n: any): void {
    if (n.read) return;
    this.notifService.markAsRead(n.notificationPublicId).subscribe();
  }

  markAllRead(): void {
    this.notifService.markAllAsRead().subscribe();
  }

  roleLabel(role: string): string {
    return { ROLE_ADMIN: 'Administrator', ROLE_SELLER: 'Seller', ROLE_CUSTOMER: 'Customer' }[role] || role;
  }

  roleBadge(role: string): string {
    return { ROLE_ADMIN: 'badge-red', ROLE_SELLER: 'badge-purple', ROLE_CUSTOMER: 'badge-blue' }[role] || 'badge-gray';
  }

  removeCartItem(item: any): void {
    this.cartService.removeItem(item.variantPublicId || item.publicId).subscribe();
  }

  triggerVerification(email: string): void {
    this.isSendingOtp = true;
    this.auth.resendOtp(email).subscribe({
      next: () => {
        this.isSendingOtp = false;
        this.toast.success('OTP sent! Please check your email.');
        this.router.navigate(['/verify-email'], { queryParams: { email } });
      },
      error: () => {
        this.isSendingOtp = false;
        this.toast.error('Failed to send OTP. Please try again later.');
      }
    });
  }
}
