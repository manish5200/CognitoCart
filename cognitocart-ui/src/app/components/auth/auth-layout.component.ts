import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="auth-split-layout">
      <!-- Left Side Visual (Hidden on Mobile) -->
      <div class="auth-visual">
        <div class="auth-visual-content">
          <div class="brand-logo">
            <span style="font-weight:900; letter-spacing:-1px;">Cognito</span><span style="color:var(--primary); font-weight:900;">Cart</span><span style="color:var(--brand);">.</span>
          </div>
          <h2>{{ heading }}</h2>
          <p [innerHTML]="subheading"></p>
          
          <ng-content select="[auth-visual-extra]"></ng-content>
        </div>
        
        <div class="auth-decoration">
           <svg width="100%" height="100%" viewBox="0 0 800 800" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid slice">
             <defs>
               <linearGradient id="grad1" x1="0%" y1="0%" x2="100%" y2="100%">
                 <stop offset="0%" style="stop-color:var(--primary);stop-opacity:0.15" />
                 <stop offset="100%" style="stop-color:var(--brand);stop-opacity:0.05" />
               </linearGradient>
               <linearGradient id="grad2" x1="100%" y1="0%" x2="0%" y2="100%">
                 <stop offset="0%" style="stop-color:var(--primary-light);stop-opacity:0.1" />
                 <stop offset="100%" style="stop-color:var(--primary);stop-opacity:0.02" />
               </linearGradient>
             </defs>
             <circle cx="20%" cy="80%" r="40%" fill="url(#grad1)" filter="blur(80px)" />
             <circle cx="80%" cy="20%" r="50%" fill="url(#grad2)" filter="blur(60px)" />
           </svg>
        </div>
      </div>

      <!-- Right Side Form -->
      <div class="auth-form-container">
        <div class="auth-form-wrapper">
          
          <!-- Mobile Brand -->
          <div class="mobile-brand">
             <span style="font-weight:900; letter-spacing:-1px;">Cognito</span><span style="color:var(--primary); font-weight:900;">Cart</span><span style="color:var(--brand);">.</span>
          </div>

          <div class="auth-header">
            <h1>{{ formHeading }}</h1>
            <p>{{ formSubheading }}</p>
          </div>

          <ng-content></ng-content>

        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-split-layout {
      display: flex;
      min-height: 100vh;
      width: 100%;
      background: var(--bg-base);
      overflow: hidden;
    }
    
    .auth-visual {
      flex: 1;
      display: none;
      position: relative;
      background: var(--bg-surface);
      border-right: 1px solid var(--border-default);
      overflow: hidden;
    }

    @media (min-width: 900px) {
      .auth-visual { display: flex; align-items: center; justify-content: center; }
    }

    .auth-decoration {
      position: absolute;
      top: 0; left: 0; right: 0; bottom: 0;
      z-index: 0;
      pointer-events: none;
      opacity: 0.8;
    }

    .auth-visual-content {
      position: relative;
      z-index: 1;
      max-width: 480px;
      padding: 40px;
      animation: fadeRight 0.8s cubic-bezier(0.16, 1, 0.3, 1);
    }

    @keyframes fadeRight {
      from { opacity: 0; transform: translateX(-20px); }
      to { opacity: 1; transform: translateX(0); }
    }

    .brand-logo {
      font-size: 28px;
      font-family: var(--font-head);
      margin-bottom: 40px;
      display: inline-block;
    }

    .mobile-brand {
      display: none;
      font-size: 24px;
      font-family: var(--font-head);
      margin-bottom: 32px;
      text-align: center;
    }

    @media (max-width: 899px) {
      .mobile-brand { display: block; }
    }

    .auth-visual-content h2 {
      font-family: var(--font-head);
      font-size: 3rem;
      font-weight: 800;
      line-height: 1.1;
      letter-spacing: -1px;
      color: var(--text-primary);
      margin-bottom: 20px;
    }

    .auth-visual-content p {
      font-size: 1.15rem;
      color: var(--text-muted);
      line-height: 1.6;
      margin-bottom: 48px;
    }

    .auth-form-container {
      flex: 1;
      overflow-y: auto;
      padding: 32px 24px;
      background: var(--bg-base);
    }

    .auth-form-wrapper {
      width: 100%;
      max-width: 400px;
      margin: 0 auto;
      min-height: 100%;
      display: flex;
      flex-direction: column;
      justify-content: center;
      padding: 32px 0;
      animation: fadeUp 0.6s cubic-bezier(0.16, 1, 0.3, 1);
    }

    @keyframes fadeUp {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .auth-header { margin-bottom: 32px; text-align: center; }
    .auth-header h1 { 
      font-family: var(--font-head);
      font-size: 2rem; 
      font-weight: 700;
      margin-bottom: 12px; 
      letter-spacing: -0.5px;
      color: var(--text-primary);
    }
    .auth-header p { font-size: 15px; color: var(--text-muted); }
  `]
})
export class AuthLayoutComponent {
  @Input() heading: string = 'The Intelligent Commerce Platform';
  @Input() subheading: string = 'Discover products. Compare smarter. Shop with confidence.';
  @Input() formHeading: string = 'Welcome Back';
  @Input() formSubheading: string = 'Sign in to continue to CognitoCart.';
}
