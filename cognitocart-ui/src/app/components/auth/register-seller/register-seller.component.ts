import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { AuthLayoutComponent } from '../auth-layout.component';

@Component({
  selector: 'app-register-seller',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink, AuthLayoutComponent],
  template: `
    <app-auth-layout
      heading="Scale Your Business with AI"
      subheading="Join as a Seller and leverage our intelligence engine to maximize your sales, manage inventory, and launch flash sales."
      [formHeading]="step === 1 ? 'Seller Partner' : 'Verify Your Business Email'"
      [formSubheading]="step === 1 ? 'Start selling on CognitoCart today' : 'Enter the OTP sent to your email'"
    >
      <!-- Step 1: Registration -->
      <form *ngIf="step === 1" [formGroup]="form" (ngSubmit)="register()" class="auth-form">
        
        <div class="form-row form-row-2">
          <div class="form-group floating">
            <label class="form-label sr-only">Owner Name</label>
            <div class="input-icon-wrapper">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z" /></svg>
              <input type="text" formControlName="ownerName" class="form-input with-icon" placeholder="Owner Name *" />
            </div>
            <div class="form-error" *ngIf="f['ownerName']?.invalid && f['ownerName']?.touched">Name is required</div>
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
            <input type="email" formControlName="email" class="form-input with-icon" placeholder="Business Email *" />
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

        <div class="form-group floating">
          <label class="form-label sr-only">Business Address</label>
          <div class="input-icon-wrapper">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" /><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1 1 15 0Z" /></svg>
            <input type="text" formControlName="businessAddress" class="form-input with-icon" placeholder="Business Address *" />
          </div>
          <div class="form-error" *ngIf="f['businessAddress']?.invalid && f['businessAddress']?.touched">Address required</div>
        </div>

        <div class="form-row form-row-2">
          <div class="form-group floating">
            <label class="form-label sr-only">Store Name</label>
            <div class="input-icon-wrapper">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M13.5 21v-7.5a.75.75 0 0 1 .75-.75h3a.75.75 0 0 1 .75.75V21m-4.5 0H2.36m11.14 0H18m0 0h3.64m-1.39 0V9.349M3.75 21V9.349m0 0a3.001 3.001 0 0 0 3.75-.615A2.993 2.993 0 0 0 9.75 9.75c.896 0 1.7-.393 2.25-1.016a2.993 2.993 0 0 0 2.25 1.016c.896 0 1.7-.393 2.25-1.015a3.001 3.001 0 0 0 3.75.614m-16.5 0a3.004 3.004 0 0 1-.621-4.72l1.189-1.19A1.5 1.5 0 0 1 5.378 3h13.243a1.5 1.5 0 0 1 1.06.44l1.19 1.189a3 3 0 0 1-.621 4.72M6.75 18h3.75a.75.75 0 0 0 .75-.75V13.5a.75.75 0 0 0-.75-.75H6.75a.75.75 0 0 0-.75.75v3.75c0 .414.336.75.75.75Z" /></svg>
              <input type="text" formControlName="storeName" class="form-input with-icon" placeholder="Store Name *" />
            </div>
            <div class="form-error" *ngIf="f['storeName']?.invalid && f['storeName']?.touched">Store Name required</div>
          </div>
          
          <div class="form-group floating">
            <label class="form-label sr-only">Tax / GSTIN Number</label>
            <div class="input-icon-wrapper">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" /></svg>
              <input type="text" formControlName="gstin" class="form-input with-icon" placeholder="GSTIN (optional)" />
            </div>
          </div>
        </div>

        <button type="submit" class="btn btn-primary btn-full btn-lg auth-submit-btn" [disabled]="loading" style="margin-top:16px;">
          <span *ngIf="loading" class="spinner spinner-sm"></span>
          {{loading ? 'Creating Account...' : 'Become a Seller'}}
        </button>
      </form>

      <!-- Step 2: OTP Verification -->
      <div *ngIf="step === 2" class="otp-section fade-in">
        <div class="otp-header">
          <div class="otp-icon-wrapper">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 0 1-2.25 2.25h-15a2.25 2.25 0 0 1-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25m19.5 0v.243a2.25 2.25 0 0 1-1.07 1.916l-7.5 4.615a2.25 2.25 0 0 1-2.36 0L3.32 8.91a2.25 2.25 0 0 1-1.07-1.916V6.75" /></svg>
          </div>
          <p>We sent a 6-digit OTP to <strong style="color:var(--text-primary);">{{email}}</strong></p>
        </div>

        <div class="form-group">
          <input type="text" [(ngModel)]="otp" class="form-input otp-input" placeholder="• • • • • •" maxlength="6" />
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
          <span>Already a Seller?</span>
        </div>

        <div class="auth-footer-links">
          <div class="link-group">
            <span class="muted-text">Already registered?</span>
            <a routerLink="/login" class="auth-link emphasis">Sign In to your account</a>
          </div>
          <div class="link-group">
            <span class="muted-text">Want to buy?</span>
            <a routerLink="/register" class="auth-link emphasis">Customer Registration</a>
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
export class RegisterSellerComponent {
  form: FormGroup;
  step: number = 1;
  loading = false;
  otp = '';
  email = '';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private toast: ToastService
  ) {
    this.form = this.fb.group({
      ownerName: ['', Validators.required],
      storeName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      phone: ['', Validators.required],
      businessAddress: ['', Validators.required],
      gstin: [''],
      pan: ['']
    });
  }

  get f() { return this.form.controls; }

  register(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    const data: any = { ...this.form.value, panCard: this.form.value.pan };
    delete data.pan;
    if (!data.gstin) delete data.gstin;
    if (!data.panCard) delete data.panCard;

    this.auth.registerSeller(data).subscribe({
      next: () => {
        this.email = this.form.value.email;
        this.step = 2;
        this.loading = false;
        this.toast.info('Seller account created! Verify your email.');
      },
      error: (e) => {
        this.loading = false;
        this.toast.error(e.error?.message || 'Registration failed.');
      }
    });
  }

  verifyOtp(): void {
    this.loading = true;
    this.auth.verifyEmail(this.email, this.otp).subscribe({
      next: () => {
        this.toast.success('Email verified! Login to access your seller dashboard.');
        this.router.navigate(['/login']);
      },
      error: (e) => {
        this.loading = false;
        this.toast.error(e.error?.message || 'Invalid OTP');
      }
    });
  }

  resendOtp(): void {
    if (!this.email) return;
    this.auth.resendOtp(this.email).subscribe({
      next: () => this.toast.success('New OTP sent to your email!'),
      error: () => this.toast.error('Failed to resend OTP. Try again later.')
    });
  }
}
