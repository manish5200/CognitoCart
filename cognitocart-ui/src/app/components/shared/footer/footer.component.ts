import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <footer class="footer">
      <div class="footer-container">
        <!-- Brand & Description -->
        <div class="footer-brand">
          <a routerLink="/" class="nav-logo" style="margin-bottom: 16px; display: inline-block;">
            <span class="logo-cognito">Cognito</span><span class="logo-cart">Cart</span><span class="logo-dot">.</span>
          </a>
          <p class="footer-description">
            Experience the future of commerce. Powered by AI, designed for modern shoppers and elite sellers.
          </p>
          <div class="footer-socials">
            <a href="#" aria-label="Twitter">🐦</a>
            <a href="#" aria-label="GitHub">🐙</a>
            <a href="#" aria-label="Discord">👾</a>
          </div>
        </div>

        <!-- Links Grid -->
        <div class="footer-links-grid">
          <div class="footer-column">
            <h4>Shop</h4>
            <ul>
              <li><a routerLink="/products">All Products</a></li>
              <li><a routerLink="/products" [queryParams]="{category:'Electronics'}">Electronics</a></li>
              <li><a routerLink="/products" [queryParams]="{category:'Fashion'}">Fashion</a></li>
              <li><a routerLink="/products" [queryParams]="{sale:'true'}">Flash Sales</a></li>
            </ul>
          </div>
          
          <div class="footer-column">
            <h4>Support</h4>
            <ul>
              <li><a href="#">Help Center</a></li>
              <li><a href="#">Track Order</a></li>
              <li><a href="#">Returns & Refunds</a></li>
              <li><a href="#">Contact Us</a></li>
            </ul>
          </div>

          <div class="footer-column">
            <h4>Company</h4>
            <ul>
              <li><a href="#">About Us</a></li>
              <li><a href="#">Careers</a></li>
              <li><a href="#">Privacy Policy</a></li>
              <li><a href="#">Terms of Service</a></li>
            </ul>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <p>&copy; 2026 CognitoCart. All rights reserved. Redesigned with ✨</p>
      </div>
    </footer>
  `,
  styles: [`
    .footer {
      background: var(--bg-surface);
      border-top: 1px solid var(--border-default);
      padding-top: 64px;
      margin-top: auto;
    }
    
    .footer-container {
      max-width: var(--content-max);
      margin: 0 auto;
      padding: 0 32px 48px;
      display: grid;
      grid-template-columns: 300px 1fr;
      gap: 64px;
    }
    
    .footer-description {
      color: var(--text-muted);
      font-size: 14px;
      line-height: 1.6;
      margin-bottom: 24px;
    }
    
    .footer-socials {
      display: flex;
      gap: 16px;
    }
    
    .footer-socials a {
      width: 40px;
      height: 40px;
      border-radius: var(--r-full);
      background: var(--glass-sm);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      transition: var(--transition);
      border: 1px solid var(--border-subtle);
    }
    
    .footer-socials a:hover {
      background: var(--primary-glow);
      border-color: var(--primary);
      transform: translateY(-2px);
    }
    
    .footer-links-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 32px;
    }
    
    .footer-column h4 {
      color: var(--text-primary);
      font-family: var(--font-head);
      font-size: 16px;
      font-weight: 700;
      margin-bottom: 20px;
      letter-spacing: 0.02em;
    }
    
    .footer-column ul {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    
    .footer-column a {
      color: var(--text-muted);
      font-size: 14px;
      transition: var(--transition-fast);
    }
    
    .footer-column a:hover {
      color: var(--primary);
      padding-left: 4px;
    }
    
    .footer-bottom {
      border-top: 1px solid var(--border-subtle);
      padding: 24px 32px;
      text-align: center;
      background: rgba(4,6,15,0.5);
    }
    
    .footer-bottom p {
      color: var(--text-dim);
      font-size: 13px;
    }
    
    @media (max-width: 900px) {
      .footer-container {
        grid-template-columns: 1fr;
        gap: 40px;
      }
    }
    
    @media (max-width: 600px) {
      .footer-links-grid {
        grid-template-columns: 1fr;
        gap: 32px;
      }
    }
  `]
})
export class FooterComponent {}
