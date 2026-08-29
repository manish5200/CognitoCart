import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-oauth2-callback',
  standalone: true,
  template: `
    <div style="height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--bg-surface);">
      <div style="text-align: center;">
        <div class="spinner" style="width: 50px; height: 50px; border: 4px solid rgba(99, 102, 241, 0.2); border-top-color: var(--primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 20px;"></div>
        <h2 style="font-family: var(--font-head); color: var(--text-primary);">Authenticating...</h2>
        <p style="color: var(--text-muted); font-size: 14px;">Securely establishing your session.</p>
      </div>
    </div>
  `,
  styles: [`
    @keyframes spin { 100% { transform: rotate(360deg); } }
  `]
})
export class Oauth2CallbackComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const error = params['error'];
      if (error) {
        if (error === 'local_account_exists') {
          this.toast.error('An account already exists with this email. Continue by signing in with your password to link your Google account.');
        } else {
          this.toast.error('We couldn\'t complete your Google sign-in. Please try again.');
        }
        this.router.navigate(['/login']);
        return;
      }

      const token = params['token'];
      const refresh = params['refresh'];
      const id = params['id'];
      const email = params['email'];
      const name = params['name'];
      const role = params['role'];

      if (token && refresh && id && email && name && role) {
        this.authService.saveOAuth2Session(token, refresh, +id, email, name, role);
        this.toast.success(`Welcome, ${name}!`);
        
        // Intelligent Redirection
        const r = role.startsWith('ROLE_') ? role : `ROLE_${role}`;
        
        let returnUrl = sessionStorage.getItem('oauth_return_url') || this.route.snapshot.queryParams['returnUrl'];
        sessionStorage.removeItem('oauth_return_url'); // clear it
        
        if (!returnUrl) {
            if (r === 'ROLE_ADMIN') returnUrl = '/admin';
            else if (r === 'ROLE_SELLER') returnUrl = '/seller'; // Or onboarding if incomplete (handled by guard or seller dashboard)
            else returnUrl = '/dashboard'; // Customer default
        }
        this.router.navigateByUrl(returnUrl);
      } else {
        this.toast.error('Authentication failed: Missing tokens from provider.');
        this.router.navigate(['/login']);
      }
    });
  }
}
