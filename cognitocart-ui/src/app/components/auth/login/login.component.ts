import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';
import { AuthLayoutComponent } from '../auth-layout.component';
import { GoogleBtnComponent } from '../google-btn.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterLink, AuthLayoutComponent, GoogleBtnComponent],
  template: `
    <app-auth-layout
      heading="The Intelligent Commerce Platform"
      subheading="Discover products. Compare smarter. Shop with confidence."
      formHeading="Welcome Back"
      formSubheading="Sign in to continue to CognitoCart."
    >
      <!-- AI Preview Concept (Left Side) -->
      <div auth-visual-extra class="ai-preview-card">
        <div class="ai-preview-header">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="ai-icon">
            <path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
          </svg>
          <span>AI Shopping Assistant</span>
        </div>
        <div class="ai-preview-chat">
          <div class="chat-bubble user">"Looking for wireless headphones under &#8377;5,000 with strong battery life?"</div>
          <div class="chat-bubble ai">AI is ready to help you find the perfect match.</div>
        </div>
      </div>

      <!-- Right Side Form -->
      <form [formGroup]="form" (ngSubmit)="submit()" class="auth-form">
        <div class="form-group floating">
          <label class="form-label sr-only">Email Address</label>
          <div class="input-icon-wrapper">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 0 1-2.25 2.25h-15a2.25 2.25 0 0 1-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25m19.5 0v.243a2.25 2.25 0 0 1-1.07 1.916l-7.5 4.615a2.25 2.25 0 0 1-2.36 0L3.32 8.91a2.25 2.25 0 0 1-1.07-1.916V6.75" /></svg>
            <input type="email" formControlName="email" class="form-input with-icon" placeholder="Email Address" />
          </div>
          <div class="form-error" *ngIf="form.get('email')?.invalid && form.get('email')?.touched">
            Valid email is required
          </div>
        </div>

        <div class="form-group floating">
          <label class="form-label sr-only">Password</label>
          <div class="input-icon-wrapper">
            <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="input-icon"><path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z" /></svg>
            <input
              [type]="showPwd ? 'text' : 'password'"
              formControlName="password"
              class="form-input with-icon"
              placeholder="Password"
              style="padding-right:48px;"
            />
            <button type="button" class="pwd-toggle" (click)="showPwd = !showPwd" aria-label="Toggle password visibility">
              <svg *ngIf="!showPwd" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z" /><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" /></svg>
              <svg *ngIf="showPwd" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88" /></svg>
            </button>
          </div>
        </div>

        <div class="forgot-pwd-row">
          <a routerLink="/forgot-password" class="auth-link">Forgot password?</a>
        </div>

        <button type="submit" class="btn btn-primary btn-full btn-lg auth-submit-btn" [disabled]="loading || oauthLoading">
          <span *ngIf="loading" class="spinner spinner-sm" style="margin-right: 8px;"></span>
          {{loading ? 'Signing in...' : 'Sign In'}}
        </button>
      </form>

      <div class="auth-divider">
        <span>OR</span>
      </div>

      <!-- Premium Google OAuth Button -->
      <app-google-btn 
        [loading]="oauthLoading" 
        [disabled]="loading || oauthLoading"
        (action)="startGoogleOAuth()">
      </app-google-btn>

      <div class="auth-footer-links">
        <div class="link-group">
          <span class="muted-text">New to CognitoCart?</span>
          <a routerLink="/register" class="auth-link emphasis">Create an account</a>
        </div>
        <div class="link-group">
          <span class="muted-text">Want to sell?</span>
          <a routerLink="/register/seller" class="auth-link emphasis">Become a Seller</a>
        </div>
      </div>
    </app-auth-layout>
  `,
  styles: [`
    /* AI Concept Card */
    .ai-preview-card {
      background: var(--bg-glass);
      border: 1px solid var(--border-default);
      border-radius: 16px;
      padding: 24px;
      box-shadow: 0 16px 40px -12px rgba(0,0,0,0.2);
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
    }
    
    .ai-preview-header {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--primary);
      font-weight: 600;
      font-size: 14px;
      margin-bottom: 16px;
      letter-spacing: 0.5px;
      text-transform: uppercase;
    }

    .ai-icon { color: var(--primary); }

    .ai-preview-chat { display: flex; flex-direction: column; gap: 12px; }
    
    .chat-bubble {
      padding: 12px 16px;
      border-radius: 12px;
      font-size: 14px;
      line-height: 1.5;
      max-width: 90%;
    }
    
    .chat-bubble.user {
      background: var(--bg-elevated);
      color: var(--text-secondary);
      align-self: flex-start;
      border-bottom-left-radius: 4px;
    }
    
    .chat-bubble.ai {
      background: linear-gradient(135deg, var(--primary), var(--primary-dark));
      color: #fff;
      align-self: flex-start;
      border-top-left-radius: 4px;
      box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
    }

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
    .form-input.with-icon {
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
    .form-input.with-icon:focus {
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

    .forgot-pwd-row {
      text-align: right;
      margin-top: -8px;
      margin-bottom: 24px;
    }

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

    .pwd-toggle {
      position: absolute;
      right: 14px;
      top: 50%;
      transform: translateY(-50%);
      background: none;
      border: none;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 4px;
      border-radius: 6px;
      color: var(--text-muted);
      transition: all 0.2s;
    }
    .pwd-toggle:hover { 
      background: var(--bg-elevated); 
      color: var(--text-primary);
    }
    .pwd-toggle svg { width: 20px; height: 20px; }
  `]
})
export class LoginComponent {
  form: FormGroup;
  loading = false;
  oauthLoading = false;
  showPwd = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private toast: ToastService
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    if (this.oauthLoading) return;
    
    this.loading = true;

    this.auth.login(this.form.value).subscribe({
      next: (res) => {
        this.toast.success(`Welcome back, ${res.fullName || 'User'}!`);
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || this.getRedirect(res.role);
        this.router.navigateByUrl(returnUrl);
      },
      error: (e) => {
        this.loading = false;
        this.toast.error(e.error?.message || 'Invalid credentials. Please try again.');
      }
    });
  }
  
  startGoogleOAuth(): void {
    if (this.loading || this.oauthLoading) return;
    this.oauthLoading = true;
    
    // Check if we need to remember the intended destination
    const returnUrl = this.route.snapshot.queryParams['returnUrl'];
    if (returnUrl) {
      // In a production app with Spring Security OAuth, passing state across the redirect 
      // can be done via cookies or session storage before redirecting to Google.
      sessionStorage.setItem('oauth_return_url', returnUrl);
    }
    
    // Redirect to the existing backend Google OAuth initiation endpoint
    // We use the standard Spring Security path
    window.location.href = 'https://cognitocart-api.onrender.com/oauth2/authorization/google';
  }

  private getRedirect(role: string): string {
    const r = role?.startsWith('ROLE_') ? role : `ROLE_${role}`;
    if (r === 'ROLE_ADMIN') return '/admin';
    if (r === 'ROLE_SELLER') return '/seller';
    if (r === 'ROLE_CUSTOMER') return '/dashboard';
    return '/';
  }
}
