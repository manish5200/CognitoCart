import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="auth-page">
      <div class="auth-split">
        <!-- Left Side: AI Branding -->
        <div class="auth-brand-side fade-up">
          <div class="brand-overlay"></div>
          <div class="brand-content">
            <h1 class="brand-logo">Cognito<span class="text-glow">Cart</span></h1>
            <h2 class="brand-tagline">Start your intelligent journey.</h2>
            <p class="brand-desc">
              Create an account to unlock personalized AI recommendations, dynamic pricing alerts, and a curated shopping experience tailored just for you.
            </p>
            <div class="ai-stat-box">
              <span class="pulse-dot"></span> Over 1M+ active AI shoppers
            </div>
          </div>
        </div>

        <!-- Right Side: Auth Form -->
        <div class="auth-form-side fade-up fade-up-delay-1">
          <div class="auth-form-inner">
            <h2 class="auth-title">Create Account</h2>
            <p class="auth-subtitle">Join the future of e-commerce.</p>
            
            <button class="btn btn-google" (click)="signUpWithGoogle()">
              <svg viewBox="0 0 24 24" width="20" height="20" xmlns="http://www.w3.org/2000/svg">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.16v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.16C1.43 8.55 1 10.22 1 12s.43 3.45 1.16 4.93l3.68-2.84z" fill="#FBBC05"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.16 7.07l3.68 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
              </svg>
              Sign up with Google
            </button>
            
            <div class="auth-divider">
              <span>OR SIGN UP WITH EMAIL</span>
            </div>

            <form (ngSubmit)="onSubmit()" #authForm="ngForm">
              <div class="form-group">
                <label class="form-label">Full Name</label>
                <div class="input-wrap">
                  <span class="input-icon">👤</span>
                  <input type="text" class="form-control" [(ngModel)]="user.fullName" name="fullName" required placeholder="John Doe" autofocus>
                </div>
              </div>

              <div class="form-group">
                <label class="form-label">Email Address</label>
                <div class="input-wrap">
                  <span class="input-icon">✉️</span>
                  <input type="email" class="form-control" [(ngModel)]="user.email" name="email" required placeholder="name@company.com">
                </div>
              </div>

              <div class="form-group">
                <label class="form-label">Password</label>
                <div class="input-wrap">
                  <span class="input-icon">🔒</span>
                  <input type="password" class="form-control" [(ngModel)]="user.password" name="password" required placeholder="At least 6 characters">
                </div>
              </div>

              <button type="submit" class="btn btn-primary btn-block" [disabled]="!authForm.form.valid || loading">
                <span *ngIf="!loading">Create Account</span>
                <span *ngIf="loading">Creating...</span>
              </button>
            </form>
            
            <p class="auth-switch">
              Already have an account? <a routerLink="/login" class="auth-link font-bold">Sign in</a>
            </p>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-page {
      min-height: calc(100vh - var(--navbar-h));
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 40px 20px;
    }
    .auth-split {
      display: flex;
      width: 100%;
      max-width: 1000px;
      min-height: 600px;
      background: var(--bg-card);
      border: 1px solid var(--border-default);
      border-radius: 24px;
      overflow: hidden;
      box-shadow: var(--shadow-2xl);
    }
    
    /* Left Side */
    .auth-brand-side {
      flex: 1;
      position: relative;
      padding: 60px 48px;
      display: flex;
      flex-direction: column;
      justify-content: center;
      background: linear-gradient(135deg, rgba(15,23,42,0.95), rgba(99,102,241,0.15)), url('https://images.unsplash.com/photo-1620712943543-bcc4688e7485?q=80&w=800&auto=format&fit=crop') center/cover;
      overflow: hidden;
    }
    @media (max-width: 800px) {
      .auth-brand-side { display: none; }
    }
    .brand-overlay {
      position: absolute;
      inset: 0;
      background: rgba(15, 23, 42, 0.6);
      backdrop-filter: blur(12px);
    }
    .brand-content {
      position: relative;
      z-index: 1;
    }
    .brand-logo {
      font-family: var(--font-head);
      font-size: 2.5rem;
      font-weight: 900;
      color: #fff;
      margin-bottom: 24px;
    }
    .text-glow {
      background: linear-gradient(135deg, #e0e7ff, #818cf8);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .brand-tagline {
      font-size: 1.8rem;
      font-weight: 700;
      color: #fff;
      margin-bottom: 16px;
      line-height: 1.2;
    }
    .brand-desc {
      font-size: 1.1rem;
      color: var(--text-muted);
      line-height: 1.6;
      margin-bottom: 48px;
      max-width: 90%;
    }
    .ai-stat-box {
      display: inline-flex;
      align-items: center;
      gap: 12px;
      background: rgba(255,255,255,0.05);
      border: 1px solid rgba(255,255,255,0.1);
      padding: 12px 20px;
      border-radius: 12px;
      font-size: 13px;
      font-weight: 600;
      color: var(--text-secondary);
      backdrop-filter: blur(8px);
    }
    .pulse-dot {
      width: 8px;
      height: 8px;
      background: var(--success);
      border-radius: 50%;
      box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
      animation: pulse-green 2s infinite;
    }
    @keyframes pulse-green {
      0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
      70% { box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
      100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
    }

    /* Right Side */
    .auth-form-side {
      flex: 1;
      padding: 60px 48px;
      display: flex;
      flex-direction: column;
      justify-content: center;
      background: var(--bg-surface);
    }
    .auth-form-inner {
      max-width: 360px;
      width: 100%;
      margin: 0 auto;
    }
    .auth-title {
      font-family: var(--font-head);
      font-size: 2rem;
      font-weight: 800;
      color: var(--text-primary);
      margin-bottom: 8px;
    }
    .auth-subtitle {
      color: var(--text-muted);
      font-size: 14px;
      margin-bottom: 32px;
    }
    
    .btn-google {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      background: var(--bg-card);
      border: 1px solid var(--border-default);
      color: var(--text-primary);
      font-weight: 600;
      font-size: 14px;
      padding: 12px;
      border-radius: 12px;
      transition: var(--transition);
    }
    .btn-google:hover {
      background: var(--bg-surface);
      border-color: var(--border-subtle);
    }
    
    .auth-divider {
      display: flex;
      align-items: center;
      text-align: center;
      margin: 32px 0;
      color: var(--text-dim);
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.1em;
    }
    .auth-divider::before, .auth-divider::after {
      content: '';
      flex: 1;
      border-bottom: 1px solid var(--border-default);
    }
    .auth-divider span {
      padding: 0 16px;
    }
    
    .form-group {
      margin-bottom: 24px;
    }
    .form-label {
      display: block;
      font-size: 13px;
      font-weight: 600;
      color: var(--text-secondary);
      margin-bottom: 8px;
    }
    .input-wrap {
      position: relative;
      display: flex;
      align-items: center;
    }
    .input-icon {
      position: absolute;
      left: 14px;
      font-size: 14px;
      color: var(--text-dim);
      pointer-events: none;
    }
    .form-control {
      width: 100%;
      background: var(--bg-card);
      border: 1px solid var(--border-default);
      color: var(--text-primary);
      padding: 12px 12px 12px 40px;
      border-radius: 12px;
      font-size: 14px;
      transition: var(--transition);
    }
    .form-control:focus {
      outline: none;
      border-color: var(--primary);
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
      background: var(--bg-surface);
    }
    .form-control::placeholder {
      color: var(--text-dim);
    }
    
    .btn-block {
      width: 100%;
      padding: 14px;
      font-size: 15px;
      border-radius: 12px;
    }
    
    .auth-switch {
      text-align: center;
      margin-top: 32px;
      font-size: 14px;
      color: var(--text-muted);
    }
    .auth-link {
      color: var(--primary);
      text-decoration: none;
      transition: var(--transition);
    }
    .auth-link:hover {
      color: var(--primary-light);
    }
    .font-bold {
      font-weight: 700;
    }
  `]
})
export class RegisterComponent {
  user = { fullName: '', email: '', password: '', role: 'ROLE_CUSTOMER' };
  loading = false;

  constructor(private auth: AuthService, private router: Router, private toast: ToastService) {}

  onSubmit() {
    this.loading = true;
    this.auth.register(this.user).subscribe({
      next: () => {
        this.toast.success('Account created! Please sign in.');
        this.loading = false;
        this.router.navigate(['/login']);
      },
      error: () => {
        this.toast.error('Registration failed.');
        this.loading = false;
      }
    });
  }

  signUpWithGoogle() {
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  }
}
