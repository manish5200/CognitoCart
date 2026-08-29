import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page">
      <div class="page-header">
        <h1 class="page-title">My Account</h1>
      </div>

      <div class="container" style="display:grid; grid-template-columns: 280px 1fr; gap: 32px; align-items: start;">
        
        <!-- Sidebar Navigation -->
        <div class="card" style="padding: 16px;">
          <div style="display:flex; align-items:center; gap:16px; margin-bottom:24px; padding-bottom:16px; border-bottom:1px solid var(--border-subtle);">
            <div style="width:56px; height:56px; border-radius:50%; background:var(--primary); color:#fff; display:flex; align-items:center; justify-content:center; font-size:24px; font-weight:800;">
              {{ user?.name?.[0]?.toUpperCase() || '?' }}
            </div>
            <div>
              <div style="font-weight:700; font-size:16px;">{{ user?.name || 'User' }}</div>
              <div style="font-size:12px; color:var(--text-muted);">{{ user?.email }}</div>
            </div>
          </div>
          
          <ul class="profile-nav">
            <li [class.active]="activeTab === 'profile'" (click)="activeTab = 'profile'">👤 Personal Info</li>
            <li [class.active]="activeTab === 'security'" (click)="activeTab = 'security'">🔒 Security</li>
            <li (click)="navigateToOrders()">📦 My Orders</li>
            <li (click)="logout()" style="color:var(--danger); margin-top:24px; border-top:1px solid var(--border-subtle); padding-top:16px;">🚪 Sign Out</li>
          </ul>
        </div>

        <!-- Main Content -->
        <div class="card" style="min-height: 400px;">
          <div class="card-body">
            
            <!-- Personal Info Tab -->
            <div *ngIf="activeTab === 'profile'">
              <h2 style="margin-bottom:24px; font-size:1.5rem;">Personal Information</h2>
              <div style="display:grid; grid-template-columns: 1fr 1fr; gap:20px; max-width:600px;">
                <div class="form-group">
                  <label class="form-label">Full Name</label>
                  <input type="text" class="form-input" [value]="user?.name" disabled>
                </div>
                <div class="form-group">
                  <label class="form-label">Email Address</label>
                  <input type="email" class="form-input" [value]="user?.email" disabled>
                </div>
                <div class="form-group">
                  <label class="form-label">Account Role</label>
                  <input type="text" class="form-input" [value]="user?.role" disabled>
                </div>
              </div>
              <button class="btn btn-primary" style="margin-top:16px;" (click)="toast.info('Edit profile coming soon')">Edit Profile</button>
            </div>
            
            <!-- Security Tab -->
            <div *ngIf="activeTab === 'security'">
              <h2 style="margin-bottom:24px; font-size:1.5rem;">Security Settings</h2>
              <div style="max-width:400px;">
                <div class="form-group">
                  <label class="form-label">Current Password</label>
                  <input type="password" class="form-input" placeholder="Enter current password">
                </div>
                <div class="form-group">
                  <label class="form-label">New Password</label>
                  <input type="password" class="form-input" placeholder="Enter new password">
                </div>
                <div class="form-group">
                  <label class="form-label">Confirm New Password</label>
                  <input type="password" class="form-input" placeholder="Confirm new password">
                </div>
                <button class="btn btn-primary" (click)="toast.success('Password updated successfully')">Update Password</button>
              </div>
            </div>

          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .profile-nav { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
    .profile-nav li { padding: 12px 16px; border-radius: var(--radius); cursor: pointer; transition: 0.2s; color: var(--text-secondary); font-weight: 500; display: flex; align-items: center; gap: 12px; }
    .profile-nav li:hover { background: rgba(255,255,255,0.05); color: #fff; }
    .profile-nav li.active { background: var(--primary-glow); color: var(--primary); border: 1px solid rgba(99,102,241,0.3); }
    @media (max-width: 768px) {
      .container { grid-template-columns: 1fr !important; }
    }
  `]
})
export class ProfileComponent implements OnInit {
  user: any;
  activeTab: 'profile' | 'security' = 'profile';

  constructor(public auth: AuthService, public toast: ToastService) {}

  ngOnInit(): void {
    this.auth.currentUser$.subscribe(u => this.user = u);
  }

  navigateToOrders(): void {
    window.location.href = '/orders';
  }

  logout(): void {
    this.auth.logout();
    window.location.href = '/login';
  }
}
