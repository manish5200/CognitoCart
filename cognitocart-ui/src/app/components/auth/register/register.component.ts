import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { AuthLayoutComponent } from '../auth-layout.component';
import { GoogleBtnComponent } from '../google-btn.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink, AuthLayoutComponent, GoogleBtnComponent],
  template: `
    <app-auth-layout
      heading="Join the Future of Shopping"
      subheading="Create your customer account to unlock AI-powered recommendations, lightning-fast checkout, and exclusive flash sales."
      [formHeading]="step === 1 ? 'Create Account' : 'Verify Your Email'"
      [formSubheading]="step === 1 ? 'Join CognitoCart as a customer' : 'Enter the OTP sent to your email'"
    >
      <!-- Step 1: Registration -->
      <form *ngIf="step === 1" [formGroup]="form" (ngSubmit)="register()" class="auth-form">
        
        <div class="form-row form-row-2">
          <div class="form-group floating">
            <label class="form-label sr-only">Full Name</label>
            <div class="input-icon-wrapper">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z" /></svg>
              <input type="text" formControlName="fullName" class="form-input with-icon" placeholder="Full Name *" />
            </div>
            <div class="form-error" *ngIf="f['fullName']?.invalid && f['fullName']?.touched">Name is required</div>
          </div>
          
          <div class="form-group floating">
            <label class="form-label sr-only">Phone Number</label>
            <div class="input-icon-wrapper">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 6.75c0 8.284 6.716 15 15 15h2.25a2.25 2.25 0 0 0 2.25-2.25v-1.372c0-.516-.351-.966-.852-1.091l-4.423-1.106c-.44-.11-.902.055-1.173.417l-.97 1.293c-2.896-1.596-5.48-4.18-7.076-7.076l1.293-.97c.362-.271.527-.734.417-1.173L6.963 3.102a1.125 1.125 0 0 0-1.091-.852H4.5A2.25 2.25 0 0 0 2.25 4.5v2.25Z" /></svg>
              <input type="tel" formControlName="phone" class="form-input with-icon" placeholder="Phone *" />
            </div>
            <div class="form-error" *ngIf="f['phone']?.invalid && f['phone']?.touched">Phone is required</div>
          </div>
        </div>

        <div class="form-group floating">
          <label class="form-label sr-only">Email Address</label>
          <div class="input-icon-wrapper">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 0 1-2.25 2.25h-15a2.25 2.25 0 0 1-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25m19.5 0v.243a2.25 2.25 0 0 1-1.07 1.916l-7.5 4.615a2.25 2.25 0 0 1-2.36 0L3.32 8.91a2.25 2.25 0 0 1-1.07-1.916V6.75" /></svg>
            <input type="email" formControlName="email" class="form-input with-icon" placeholder="Email Address *" />
          </div>
          <div class="form-error" *ngIf="f['email']?.invalid && f['email']?.touched">Valid email is required</div>
        </div>

        <div class="form-group floating">
          <label class="form-label sr-only">Password</label>
          <div class="input-icon-wrapper">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z" /></svg>
            <input type="password" formControlName="password" class="form-input with-icon" placeholder="Password (Min 8 chars) *" />
          </div>
          <div class="form-error" *ngIf="f['password']?.invalid && f['password']?.touched">Min 8 characters required</div>
        </div>

        <div class="form-row form-row-2">
          <div class="form-group floating">
            <label class="form-label sr-only">Date of Birth</label>
            <div class="input-icon-wrapper">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5" /></svg>
              <input type="date" formControlName="dateOfBirth" class="form-input with-icon" />
            </div>
          </div>
          
          <div class="form-group floating">
            <label class="form-label sr-only">Gender</label>
            <div class="input-icon-wrapper">
              <select formControlName="gender" class="form-select with-icon">
                <option value="">Gender (Optional)</option>
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
          </div>
        </div>

        <button type="submit" class="btn btn-primary btn-full btn-lg auth-submit-btn" [disabled]="loading || oauthLoading" style="margin-top:16px;">
          <span *ngIf="loading" class="spinner spinner-sm"></span>
          {{loading ? 'Creating Account...' : 'Create Account'}}
        </button>
      </form>

      <!-- Step 2: OTP Verification -->
      <div *ngIf="step === 2" class="otp-section fade-in">
        <div class="otp-header">
          <div class="otp-icon-wrapper">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 0 1-2.25 2.25h-15a2.25 2.25 0 0 1-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25m19.5 0v.243a2.25 2.25 0 0 1-1.07 1.916l-7.5 4.615a2.25 2.25 0 0 1-2.36 0L3.32 8.91a2.25 2.25 0 0 1-1.07-1.916V6.75" /></svg>
          </div>
          <p>We sent a 6-digit OTP to <strong style="color:var(--text-primary);">{{registeredEmail}}</strong></p>
        </div>

        <div class="form-group">
          <input type="text" [(ngModel)]="otp" class="form-input otp-input" placeholder="* * * * * *" maxlength="6" />
        </div>

        <button class="btn btn-primary btn-full btn-lg auth-submit-btn" (click)="verifyOtp()" [disabled]="loading || otp.length < 6">
          {{loading ? 'Verifying...' : 'Verify Email'}}
        </button>

        <p class="resend-text">
          Didn't receive it?
          <button class="btn btn-ghost btn-sm" (click)="resendOtp()" style="margin-left:8px;">Resend OTP</button>
        </p>
      </div>

      <ng-container *ngIf="step === 1">
        <div class="auth-divider">
          <span>OR</span>
        </div>

        <!-- Premium Google OAuth Button -->
        <app-google-btn 
          [loading]="oauthLoading" 
          [disabled]="loading || oauthLoading"
          text="Sign up with Google"
          (action)="startGoogleOAuth()">
        </app-google-btn>

        <div class="auth-footer-links">
          <div class="link-group">
            <span class="muted-text">Already registered?</span>
            <a routerLink="/login" class="auth-link emphasis">Sign In to your account</a>
          </div>
          <div class="link-group">
            <span class="muted-text">Want to sell?</span>
            <a routerLink="/register/seller" class="auth-link emphasis">Seller Registration</a>
          </div>
        </div>
      </ng-container>

    </app-auth-layout>
  `,
  styles: [`
    .input-icon-wrapper { position: relative; }
    .input-icon {
      position: absolute;
      left: 16px;
      top: 50%;
      transform: translateY(-50%);
      width: 20px;
      height: 20px;
      color: var(--text-muted);
      pointer-events: none;
    }
    .form-group { margin-bottom: 20px; }
    .form-input.with-icon, .form-select.with-icon {
      padding-left: 48px;
      height: 54px;
      background: var(--bg-surface);
      border: 1px solid var(--border-default);
      border-radius: 12px;
      transition: all 0.2s ease;
      font-size: 15px;
      color: var(--text-primary);
      width: 100%;
    }
    .form-input.with-icon:focus, .form-select.with-icon:focus {
      background: var(--bg-elevated);
      border-color: var(--primary);
      box-shadow: 0 0 0 3px var(--primary-glow);
      outline: none;
    }

    .sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); border: 0; }

    .auth-link {
      font-size: 14px;
      color: var(--primary);
      text-decoration: none;
      font-weight: 500;
      transition: color 0.2s;
    }
    .auth-link:hover { color: var(--primary-light); }
    .auth-link.emphasis { font-weight: 600; }

    .auth-submit-btn {
      height: 54px;
      font-size: 16px;
      font-weight: 600;
      letter-spacing: 0.5px;
      border-radius: 12px;
      box-shadow: 0 8px 20px -8px rgba(99,102,241,0.5);
      transition: all 0.2s;
    }
    .auth-submit-btn:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 10px 24px -6px rgba(99,102,241,0.6);
    }

    .auth-divider {
      display: flex;
      align-items: center;
      text-align: center;
      margin: 32px 0;
      color: var(--text-muted);
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 1px;
    }
    .auth-divider::before, .auth-divider::after {
      content: '';
      flex: 1;
      border-bottom: 1px solid var(--border-subtle);
    }
    .auth-divider span { padding: 0 16px; }

    .auth-footer-links {
      display: flex;
      flex-direction: column;
      gap: 16px;
      align-items: center;
    }
    
    .link-group {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
    }
    
    .muted-text { color: var(--text-muted); }
    
    .fade-in { animation: fadeIn 0.4s ease; }
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .otp-section { text-align: center; }
    .otp-header { margin-bottom: 32px; }
    .otp-icon-wrapper {
        width: 64px; 
        height: 64px; 
        background: var(--bg-surface); 
        border: 1px solid var(--border-strong); 
        border-radius: 50%; 
        display: flex; 
        align-items: center; 
        justify-content: center; 
        margin: 0 auto 16px;
    }
    .otp-icon-wrapper svg {
        width: 32px;
        height: 32px;
        color: var(--primary);
    }
    .otp-header p { color: var(--text-muted); font-size: 14px; margin-top: 12px; }
    
    .otp-input { 
      font-size: 28px; 
      text-align: center; 
      letter-spacing: 12px; 
      font-weight: 700; 
      height: 64px;
      background: var(--bg-surface);
      border: 1px solid var(--border-default);
      border-radius: 12px;
    }
    .otp-input:focus {
      border-color: var(--primary);
      box-shadow: 0 0 0 4px var(--primary-glow);
    }
    .resend-text {
        text-align: center;
        margin-top: 24px;
        font-size: 14px;
        color: var(--text-muted);
    }
  `]
})
export class RegisterComponent {
  form: FormGroup;
  step: number = 1;
  loading = false;
  oauthLoading = false;
  otp = '';
  registeredEmail = '';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private toast: ToastService
  ) {
    this.form = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      phone: ['', Validators.required],
      dateOfBirth: [''],
      gender: ['']
    });
  }

  get f() { return this.form.controls; }

  register(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    const data: any = { ...this.form.value, name: this.form.value.fullName };
    delete data.fullName;
    if (!data.dateOfBirth) delete data.dateOfBirth;
    if (!data.gender) delete data.gender;

    this.auth.registerCustomer(data).subscribe({
      next: () => {
        this.registeredEmail = this.form.value.email;
        this.step = 2;
        this.loading = false;
        this.toast.info('Check your email for the OTP!');
      },
      error: (e) => {
        this.loading = false;
        this.toast.error(e.error?.message || 'Registration failed. Try again.');
      }
    });
  }

  verifyOtp(): void {
    this.loading = true;
    this.auth.verifyEmail(this.registeredEmail, this.otp).subscribe({
      next: () => {
        this.toast.success('Email verified! Please login.');
        this.router.navigate(['/login']);
      },
      error: (e) => {
        this.loading = false;
        this.toast.error(e.error?.message || 'Invalid OTP. Try again.');
      }
    });
  }

  resendOtp(): void {
    this.auth.resendOtp(this.registeredEmail).subscribe({
      next: () => this.toast.info('New OTP sent!'),
      error: () => this.toast.error('Failed to resend OTP')
    });
  }
  
  startGoogleOAuth(): void {
    if (this.loading || this.oauthLoading) return;
    this.oauthLoading = true;
    
    // Redirect to the existing backend Google OAuth initiation endpoint
    window.location.href = 'https://cognitocart-api.onrender.com/oauth2/authorization/google';
  }
}
