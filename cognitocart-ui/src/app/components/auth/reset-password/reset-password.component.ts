import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';

@Component({ selector: 'app-reset-password', standalone: true, imports: [CommonModule, FormsModule, RouterLink],
template: `
    <div class="auth-split-layout">
      <!-- Left Side Visual -->
      <div class="auth-visual">
        <div class="auth-visual-content">
          <div class="brand-logo">
            <span style="font-weight:900; letter-spacing:-1px;">Cognito</span><span style="color:var(--primary); font-weight:900;">Cart</span><span style="color:var(--brand);">.</span>
          </div>
          <h2>Secure Your<br/>Account</h2>
          <p>Choose a strong password to protect your account and continue your journey.</p>
        </div>
        
        <div class="auth-decoration">
           <svg width="100%" height="100%" viewBox="0 0 800 800" xmlns="http://www.w3.org/2000/svg">
             <defs>
               <linearGradient id="grad1" x1="0%" y1="0%" x2="100%" y2="100%">
                 <stop offset="0%" style="stop-color:#10b981;stop-opacity:0.4" />
                 <stop offset="100%" style="stop-color:#3b82f6;stop-opacity:0.1" />
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
            <h1>Reset Password</h1>
            <p>Enter your new password below.</p>
          </div>

          <div class="form-group floating">
            <label class="form-label sr-only">New Password</label>
            <div class="input-icon-wrapper">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z" /></svg>
              <input type="password" [(ngModel)]="password" class="form-input with-icon" placeholder="New Password (Min 8 chars)" />
            </div>
          </div>
          
          <div class="form-group floating" style="margin-bottom:8px;">
            <label class="form-label sr-only">Confirm Password</label>
            <div class="input-icon-wrapper">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z" /></svg>
              <input type="password" [(ngModel)]="confirm" class="form-input with-icon" placeholder="Confirm Password" />
            </div>
          </div>
          
          <div class="form-error" *ngIf="password && confirm && password !== confirm" style="margin-bottom:16px;">Passwords do not match</div>
          <div class="form-error" *ngIf="password && password.length < 8" style="margin-bottom:16px;">Minimum 8 characters required</div>

          <button class="btn btn-primary btn-full btn-lg auth-submit-btn" (click)="submit()" [disabled]="loading || !password || password !== confirm || password.length < 8" style="margin-top:16px;">
            <span *ngIf="loading" class="spinner spinner-sm"></span>
            {{loading ? 'Resetting...' : 'Reset Password'}}
          </button>

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
export class ResetPasswordComponent implements OnInit {
  token = ''; password = ''; confirm = ''; loading = false;
  constructor(private route: ActivatedRoute, private auth: AuthService, private router: Router, private toast: ToastService) {}
  ngOnInit(): void { this.token = this.route.snapshot.queryParams['token'] || ''; }
  submit(): void {
    this.loading = true;
    this.auth.resetPassword(this.token, this.password).subscribe({
      next: () => { this.toast.success('Password reset! Please login.'); this.router.navigate(['/login']); },
      error: (e) => { this.loading = false; this.toast.error(e.error?.message || 'Reset failed. Link may have expired.'); }
    });
  }
}
