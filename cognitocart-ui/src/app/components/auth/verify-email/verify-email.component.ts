import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="auth-split-layout">
      <!-- Left Side Visual -->
      <div class="auth-visual">
        <div class="auth-visual-content">
          <div class="brand-logo">
            <span style="font-weight:900; letter-spacing:-1px;">Cognito</span><span style="color:var(--primary); font-weight:900;">Cart</span><span style="color:var(--brand);">.</span>
          </div>
          <h2>Confirm Your<br/>Identity</h2>
          <p>Please enter the one-time password we sent to your email to verify your account.</p>
        </div>
        
        <div class="auth-decoration">
           <svg width="100%" height="100%" viewBox="0 0 800 800" xmlns="http://www.w3.org/2000/svg">
             <defs>
               <linearGradient id="grad1" x1="0%" y1="0%" x2="100%" y2="100%">
                 <stop offset="0%" style="stop-color:#3b82f6;stop-opacity:0.4" />
                 <stop offset="100%" style="stop-color:#8b5cf6;stop-opacity:0.1" />
               </linearGradient>
             </defs>
             <circle cx="500" cy="500" r="350" fill="url(#grad1)" filter="blur(70px)" />
           </svg>
        </div>
      </div>

      <!-- Right Side Form -->
      <div class="auth-form-container">
        <div class="auth-form-wrapper">
          
          <div class="otp-header" style="text-align:center; margin-bottom:32px;">
            <div style="width:64px; height:64px; background:var(--glass-sm); border:1px solid var(--border-subtle); border-radius:50%; display:flex; align-items:center; justify-content:center; margin:0 auto 16px;">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:32px; height:32px; color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 0 1-2.25 2.25h-15a2.25 2.25 0 0 1-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25m19.5 0v.243a2.25 2.25 0 0 1-1.07 1.916l-7.5 4.615a2.25 2.25 0 0 1-2.36 0L3.32 8.91a2.25 2.25 0 0 1-1.07-1.916V6.75" /></svg>
            </div>
            <h1 style="font-family:var(--font-head); font-size:2rem; font-weight:700; margin-bottom:8px; letter-spacing:-0.5px;">Verify Email</h1>
            <p style="font-size:14px; color:var(--text-muted);">We sent a 6-digit OTP to <strong style="color:var(--text-primary);">{{email}}</strong></p>
          </div>

          <div class="form-group">
            <input type="text" [(ngModel)]="otp" class="form-input otp-input" placeholder="&#8226; &#8226; &#8226; &#8226; &#8226; &#8226;" maxlength="6" />
          </div>

          <button class="btn btn-primary btn-full btn-lg auth-submit-btn" (click)="submit()" [disabled]="loading || otp.length !== 6">
            <span *ngIf="loading" class="spinner spinner-sm"></span>
            {{loading ? 'Verifying...' : 'Verify Email'}}
          </button>

          <p style="text-align:center; margin-top:24px; font-size:14px; color:var(--text-muted);">
            Didn't receive it?
            <button class="btn btn-ghost btn-sm" (click)="resend()" style="margin-left:8px;">Resend OTP</button>
          </p>

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
    .otp-input { font-size: 28px; text-align: center; letter-spacing: 12px; font-weight: 700; height: 64px; background: var(--glass-sm); border: 1px solid var(--border-subtle); border-radius: 12px; }
    .otp-input:focus { border-color: var(--primary); box-shadow: 0 0 0 4px var(--primary-glow); }
    .auth-submit-btn { height: 52px; font-size: 16px; font-weight: 600; letter-spacing: 0.5px; box-shadow: 0 8px 24px -8px rgba(59,130,246,0.5); margin-top: 16px; }
  `]
})
export class VerifyEmailComponent implements OnInit {
  email = '';
  otp = '';
  loading = false;

  constructor(
    private route: ActivatedRoute,
    private auth: AuthService,
    private router: Router,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.email = this.route.snapshot.queryParams['email'] || '';
  }

  submit(): void {
    this.loading = true;
    this.auth.verifyEmail(this.email, this.otp).subscribe({
      next: () => {
        this.toast.success('Email verified successfully!');
        
        if (this.auth.isLoggedIn()) {
          this.auth.refreshToken().subscribe({
            next: () => this.router.navigate(['/dashboard']),
            error: () => this.router.navigate(['/login'])
          });
        } else {
          this.router.navigate(['/login']);
        }
      },
      error: (e) => {
        this.loading = false;
        this.toast.error(e.error?.message || 'Invalid or expired OTP.');
      }
    });
  }

  resend(): void {
    if (!this.email) return;
    this.auth.resendOtp(this.email).subscribe({
      next: () => this.toast.success('New OTP sent to your email!'),
      error: () => this.toast.error('Failed to resend OTP. Try again later.')
    });
  }
}
