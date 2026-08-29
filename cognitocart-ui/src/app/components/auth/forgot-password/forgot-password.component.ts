import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';

@Component({ selector: 'app-forgot-password', standalone: true, imports: [CommonModule, FormsModule, RouterLink],
template: `
    <div class="auth-split-layout">
      <!-- Left Side Visual -->
      <div class="auth-visual">
        <div class="auth-visual-content">
          <div class="brand-logo">
            <span style="font-weight:900; letter-spacing:-1px;">Cognito</span><span style="color:var(--primary); font-weight:900;">Cart</span><span style="color:var(--brand);">.</span>
          </div>
          <h2>Forgot your<br/>Password?</h2>
          <p>Don't worry! Enter your email and we will send you a secure link to reset it.</p>
        </div>
        
        <div class="auth-decoration">
           <svg width="100%" height="100%" viewBox="0 0 800 800" xmlns="http://www.w3.org/2000/svg">
             <defs>
               <linearGradient id="grad1" x1="0%" y1="0%" x2="100%" y2="100%">
                 <stop offset="0%" style="stop-color:#ef4444;stop-opacity:0.4" />
                 <stop offset="100%" style="stop-color:#f472b6;stop-opacity:0.1" />
               </linearGradient>
             </defs>
             <circle cx="500" cy="500" r="350" fill="url(#grad1)" filter="blur(70px)" />
           </svg>
        </div>
      </div>

      <!-- Right Side Form -->
      <div class="auth-form-container">
        <div class="auth-form-wrapper">
          
          <div class="auth-header">
            <h1>Account Recovery</h1>
            <p *ngIf="!sent">Enter your email and we'll send you a reset link.</p>
            <p *ngIf="sent" style="color:var(--primary);">Reset link sent! Check your email inbox.</p>
          </div>

          <div *ngIf="!sent">
            <div class="form-group floating" style="margin-bottom:24px;">
              <label class="form-label sr-only">Email Address</label>
              <div class="input-icon-wrapper">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 0 1-2.25 2.25h-15a2.25 2.25 0 0 1-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25m19.5 0v.243a2.25 2.25 0 0 1-1.07 1.916l-7.5 4.615a2.25 2.25 0 0 1-2.36 0L3.32 8.91a2.25 2.25 0 0 1-1.07-1.916V6.75" /></svg>
                <input type="email" [(ngModel)]="email" class="form-input with-icon" placeholder="Email Address" />
              </div>
            </div>
            
            <button class="btn btn-primary btn-full btn-lg auth-submit-btn" (click)="submit()" [disabled]="loading || !email">
              <span *ngIf="loading" class="spinner spinner-sm"></span>
              {{loading ? 'Sending...' : 'Send Reset Link'}}
            </button>
          </div>

          <div class="auth-footer" style="text-align:center; margin-top:32px;">
            <a routerLink="/login" class="auth-link">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width:14px; height:14px; display:inline-block; vertical-align:middle; margin-right:4px;"><path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" /></svg>
              Back to Login
            </a>
          </div>

        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-split-layout { display: flex; min-height: 100vh; width: 100%; background: var(--bg-base); overflow: hidden; }
    .auth-visual { flex: 1; display: none; position: relative; background: var(--bg-surface); border-right: 1px solid var(--border-subtle); overflow: hidden; }
    @media (min-width: 900px) { .auth-visual { display: flex; align-items: center; justify-content: center; } }
    .auth-decoration { position: absolute; top: 0; left: 0; right: 0; bottom: 0; z-index: 0; pointer-events: none; opacity: 0.7; }
    .auth-visual-content { position: relative; z-index: 1; max-width: 480px; padding: 40px; }
    .brand-logo { font-size: 28px; font-family: var(--font-head); margin-bottom: 40px; }
    .auth-visual-content h2 { font-family: var(--font-head); font-size: 3.5rem; font-weight: 800; line-height: 1.1; letter-spacing: -1px; background: linear-gradient(135deg, #fff 0%, #a1a1aa 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; margin-bottom: 24px; }
    .auth-visual-content p { font-size: 1.1rem; color: var(--text-muted); line-height: 1.6; }
    .auth-form-container { flex: 1; display: flex; align-items: center; justify-content: center; padding: 32px 16px; background: var(--bg-base); overflow-y: auto; }
    .auth-form-wrapper { width: 100%; max-width: 420px; animation: fadeUp 0.6s cubic-bezier(0.16, 1, 0.3, 1); }
    @keyframes fadeUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
    .auth-header { margin-bottom: 32px; text-align: center; }
    .auth-header h1 { font-family: var(--font-head); font-size: 2rem; font-weight: 700; margin-bottom: 8px; letter-spacing: -0.5px; }
    .auth-header p { font-size: 14px; color: var(--text-muted); }
    .input-icon-wrapper { position: relative; }
    .input-icon { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); width: 20px; height: 20px; color: var(--text-muted); pointer-events: none; }
    .form-input.with-icon { padding-left: 44px; height: 52px; background: var(--glass-sm); border: 1px solid var(--border-subtle); transition: all 0.2s ease; }
    .form-input.with-icon:focus { background: var(--bg-surface); border-color: var(--primary); box-shadow: 0 0 0 4px var(--primary-glow); }
    .sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); border: 0; }
    .auth-link { font-size: 14px; color: var(--text-muted); text-decoration: none; font-weight: 500; transition: color 0.2s; }
    .auth-link:hover { color: var(--text-primary); }
    .auth-submit-btn { height: 52px; font-size: 16px; font-weight: 600; letter-spacing: 0.5px; box-shadow: 0 8px 24px -8px rgba(59,130,246,0.5); }
  `]
})
export class ForgotPasswordComponent {
  email = ''; loading = false; sent = false;
  constructor(private auth: AuthService, private toast: ToastService) {}
  submit(): void {
    this.loading = true;
    this.auth.forgotPassword(this.email).subscribe({
      next: () => { this.sent = true; this.loading = false; },
      error: () => { this.loading = false; this.sent = true; } // always show same msg (no enumeration)
    });
  }
}
