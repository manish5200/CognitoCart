import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { NotificationService } from '../../services/notification.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <header class="header">
      <div class="header-container">
        
        <a routerLink="/" class="brand-logo">Cognito<span>Cart</span></a>
        
        <div class="search-bar">
          <input type="text" [(ngModel)]="searchQuery" (keyup.enter)="onSearch()" class="search-input" placeholder="Ask Cognitive AI (e.g. earphones for noisy cafe)...">
          <button (click)="onSearch()" class="search-btn" style="display:flex; align-items:center; justify-content:center;">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:18px;height:18px;"><path stroke-linecap="round" stroke-linejoin="round" d="M9.813 15.904 9 18.75l-.813-2.846a4.5 4.5 0 0 0-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 0 0 3.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 0 0 3.09 3.09l2.846.813-2.846.813a4.5 4.5 0 0 0-3.09 3.09ZM18.259 8.715 18 9.75l-.259-1.035a3.375 3.375 0 0 0-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 0 0 2.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 0 0 2.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 0 0-2.456 2.456ZM16.894 20.567 16.5 21.75l-.394-1.183a2.25 2.25 0 0 0-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 0 0 1.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 0 0 1.423 1.423l1.183.394-1.183.394a2.25 2.25 0 0 0-1.423 1.423Z" /></svg>
          </button>
        </div>
        
        <div class="nav-actions">
          <!-- Logged Out -->
          <a *ngIf="!(auth.currentUser$ | async)" routerLink="/login" class="nav-link">
            <span>Hello, sign in</span>
            <span style="font-weight: 700; color: #fff;">Account & Lists</span>
          </a>

          <!-- Logged In -->
          <ng-container *ngIf="auth.currentUser$ | async as user">
            
            <div class="nav-link" style="cursor: pointer;" [routerLink]="user.role === 'ROLE_ADMIN' ? '/admin' : '/profile'">
              <span>Hello, {{ user.name }}</span>
              <span style="font-weight: 700; color: #fff;">{{ user.role === 'ROLE_ADMIN' ? 'Administrator' : 'Account & Lists' }}</span>
            </div>
            
            <!-- Admin Only -->
            <a *ngIf="user.role === 'ROLE_ADMIN'" routerLink="/admin" style="margin-left: 20px; font-weight: 500; color: var(--warning); text-decoration: none; display: flex; align-items: center; gap: 5px;">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:20px;height:20px;"><path stroke-linecap="round" stroke-linejoin="round" d="M10.343 3.94c.09-.542.56-.94 1.11-.94h1.093c.55 0 1.02.398 1.11.94l.149.894c.07.424.384.764.78.93.398.164.855.142 1.205-.108l.737-.527a1.125 1.125 0 0 1 1.45.12l.773.774c.39.389.44 1.002.12 1.45l-.527.737c-.25.35-.272.806-.107 1.204.165.397.505.71.93.78l.893.15c.543.09.94.56.94 1.109v1.094c0 .55-.397 1.02-.94 1.11l-.893.149c-.425.07-.765.383-.93.78-.165.398-.143.854.107 1.204l.527.738c.32.447.269 1.06-.12 1.45l-.774.773a1.125 1.125 0 0 1-1.449.12l-.738-.527c-.35-.25-.806-.272-1.203-.107-.397.165-.71.505-.78.929l-.15.894c-.09.542-.56.94-1.11.94h-1.094c-.55 0-1.019-.398-1.11-.94l-.148-.894c-.071-.424-.384-.764-.781-.93-.398-.164-.854-.142-1.204.108l-.738.527c-.447.32-1.06.269-1.45-.12l-.773-.774a1.125 1.125 0 0 1-.12-1.45l.527-.737c.25-.35.273-.806.108-1.204-.165-.397-.505-.71-.93-.78l-.894-.15c-.542-.09-.94-.56-.94-1.109v-1.094c0-.55.398-1.02.94-1.11l.894-.149c.424-.07.765-.383.93-.78.165-.398.143-.854-.107-1.204l-.527-.738a1.125 1.125 0 0 1 .12-1.45l.773-.773a1.125 1.125 0 0 1 1.45-.12l.737.527c.35.25.807.272 1.204.107.397-.165.71-.505.78-.929l.15-.894Z" /><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" /></svg>
              Command Center
            </a>
            
            <!-- Customer Only -->
            <a *ngIf="user.role !== 'ROLE_ADMIN'" routerLink="/cart" class="cart-container" style="display:flex; align-items:center;">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:24px;height:24px;"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 3h1.386c.51 0 .955.343 1.087.835l.383 1.437M7.5 14.25a3 3 0 0 0-3 3h15.75m-12.75-3h11.218c1.121-2.3 2.1-4.684 2.924-7.138a60.114 60.114 0 0 0-16.536-1.84M7.5 14.25 5.106 5.272M6 20.25a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm12.75 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z" /></svg>
              <span style="font-weight: 700; margin-left: 5px;">Cart</span>
              <span class="cart-badge" *ngIf="(cartService.cart$ | async) as cart">{{ cart.items.length }}</span>
              <span class="cart-badge" *ngIf="!(cartService.cart$ | async)">0</span>
            </a>
            <!-- Notifications Bell -->
            <div style="position: relative; margin-left: 15px; cursor: pointer;" (click)="toggleNotifications()">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:24px;height:24px;color:var(--text-primary);"><path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0M3.124 7.5A8.969 8.969 0 0 1 5.292 3m13.416 0a8.969 8.969 0 0 1 2.168 4.5" /></svg>
              <span class="cart-badge" *ngIf="unreadCount > 0" style="background:var(--danger);">{{unreadCount}}</span>
              
              <!-- Dropdown -->
              <div *ngIf="showNotifications" class="card" style="position: absolute; top: 100%; right: 0; width: 320px; z-index: 1000; margin-top: 10px; background:var(--bg-card); border:1px solid var(--border); box-shadow:0 10px 30px rgba(0,0,0,0.5);" (click)="$event.stopPropagation()">
                <div style="padding: 16px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center;">
                  <h4 style="margin: 0;">Notifications</h4>
                  <button *ngIf="unreadCount > 0" class="btn btn-ghost btn-sm" (click)="markAllAsRead()" style="font-size: 12px; padding: 4px 8px;">Mark all read</button>
                </div>
                <div style="max-height: 350px; overflow-y: auto;">
                  <div *ngIf="notifications.length === 0" style="padding: 24px; text-align: center; color: var(--text-muted);">No new notifications</div>
                  <div *ngFor="let n of notifications" style="padding: 12px 16px; border-bottom: 1px solid var(--border); display: flex; gap: 12px; align-items: flex-start; cursor: pointer;" [style.background]="n.read ? 'transparent' : 'rgba(99, 102, 241, 0.05)'" (click)="!n.read && markAsRead(n.publicId, $event)">
                    <div style="flex-shrink: 0; display:flex; align-items:center; justify-content:center; width:32px; height:32px; border-radius:50%; background:var(--bg-lighter);">
                      <svg *ngIf="n.type === 'ORDER_STATUS'" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:18px;height:18px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="m21 7.5-9-5.25L3 7.5m18 0-9 5.25m9-5.25v9l-9 5.25M3 7.5l9 5.25M3 7.5v9l9 5.25m0-9v9" /></svg>
                      <svg *ngIf="n.type === 'PROMO'" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:18px;height:18px;color:var(--warning);"><path stroke-linecap="round" stroke-linejoin="round" d="M15.362 5.214A8.252 8.252 0 0 1 12 21 8.25 8.25 0 0 1 6.038 7.047 8.287 8.287 0 0 0 9 9.601a8.983 8.983 0 0 1 3.361-6.867 8.21 8.21 0 0 0 3 2.48Z" /><path stroke-linecap="round" stroke-linejoin="round" d="M12 18a3.75 3.75 0 0 0 .495-7.468 5.99 5.99 0 0 0-1.925 3.547 5.975 5.975 0 0 1-2.133-1.001A3.75 3.75 0 0 0 12 18Z" /></svg>
                      <svg *ngIf="n.type !== 'ORDER_STATUS' && n.type !== 'PROMO'" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:18px;height:18px;color:var(--text-muted);"><path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0" /></svg>
                    </div>
                    <div style="flex: 1;">
                      <div style="font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 2px;">{{n.title}}</div>
                      <div style="font-size: 12px; color: var(--text-muted);">{{n.message}}</div>
                      <div style="font-size: 11px; color: var(--text-dim); margin-top: 4px;">{{n.createdAt | date:'short'}}</div>
                    </div>
                    <div *ngIf="!n.read" style="width: 8px; height: 8px; border-radius: 50%; background: var(--primary); flex-shrink: 0; margin-top: 4px;"></div>
                  </div>
                </div>
              </div>
            </div>
            
            <button (click)="logout()" class="btn-secondary" style="padding: 6px 12px; font-size: 12px; margin-left: 20px;">
              Logout
            </button>
            
          </ng-container>
        </div>
      </div>
    </header>
  `
})
export class NavbarComponent implements OnInit, OnDestroy {
  searchQuery = '';
  showNotifications = false;
  notifications: any[] = [];
  unreadCount = 0;
  private notifSub?: Subscription;
  private authSub?: Subscription;

  constructor(
    public auth: AuthService, 
    public cartService: CartService, 
    public notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit() {
    this.authSub = this.auth.currentUser$.subscribe(u => {
      if (u) {
        this.loadNotifications();
        if (!this.notifSub) {
          this.notifSub = this.notificationService.notificationsUpdated$.subscribe(() => this.loadNotifications());
        }
      } else {
        this.notifications = [];
        this.unreadCount = 0;
        this.showNotifications = false;
      }
    });
  }

  ngOnDestroy() {
    if (this.notifSub) this.notifSub.unsubscribe();
    if (this.authSub) this.authSub.unsubscribe();
  }

  loadNotifications() {
    this.notificationService.getNotifications(0, 10).subscribe({
      next: (res) => {
        this.notifications = res.content || res || [];
        this.unreadCount = this.notifications.filter(n => !n.read).length;
      }
    });
  }

  toggleNotifications() {
    this.showNotifications = !this.showNotifications;
    if (this.showNotifications) {
      this.loadNotifications();
    }
  }

  markAsRead(id: string, e: Event) {
    e.stopPropagation();
    this.notificationService.markAsRead(id).subscribe();
  }
  
  markAllAsRead() {
    this.notificationService.markAllAsRead().subscribe();
  }

  onSearch() {
    if (this.searchQuery.trim()) {
      this.router.navigate(['/search'], { queryParams: { q: this.searchQuery.trim() } });
    }
  }

  logout() {
    this.auth.logout();
    this.cartService.clearCart().subscribe();
    this.router.navigate(['/']);
  }
}
