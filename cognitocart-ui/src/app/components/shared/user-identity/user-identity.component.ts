import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-user-identity',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="user-identity" [ngClass]="size">
      <!-- 1. Profile Image -->
      <img *ngIf="profileImage && !imageLoadError"
           [src]="profileImage"
           (error)="onImageError()"
           alt="Profile avatar"
           class="avatar-img" />

      <!-- 2. Initials Fallback -->
      <div *ngIf="(!profileImage || imageLoadError) && initials"
           class="avatar-initials"
           [style.background]="getDeterministicColor()">
        {{ initials }}
      </div>

      <!-- 3. Default Icon Fallback -->
      <div *ngIf="(!profileImage || imageLoadError) && !initials"
           class="avatar-fallback">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
          <path fill-rule="evenodd" d="M7.5 6a4.5 4.5 0 119 0 4.5 4.5 0 01-9 0zM3.751 20.105a8.25 8.25 0 0116.498 0 .75.75 0 01-.437.695A18.683 18.683 0 0112 22.5c-2.786 0-5.433-.608-7.812-1.7a.75.75 0 01-.437-.695z" clip-rule="evenodd" />
        </svg>
      </div>

      <!-- Optional Details Context -->
      <div *ngIf="showDetails" class="user-details">
        <div class="user-name">{{ name || 'Guest User' }}</div>
        <div *ngIf="subtitle" class="user-subtitle">{{ subtitle }}</div>
        <span *ngIf="roleBadge" class="user-badge" [ngClass]="'badge-' + roleBadge.color">{{ roleBadge.text }}</span>
      </div>
    </div>
  `,
  styles: [`
    .user-identity {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    
    .avatar-img, .avatar-initials, .avatar-fallback {
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 12px; /* modern squircle feel */
      flex-shrink: 0;
      color: #fff;
      font-weight: 700;
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
      border: 1px solid rgba(255,255,255,0.1);
      overflow: hidden;
    }
    
    .avatar-img {
      object-fit: cover;
    }

    .avatar-fallback {
      background: var(--surface-light);
      color: var(--text-muted);
    }
    .avatar-fallback svg {
      width: 60%;
      height: 60%;
    }

    .user-details {
      display: flex;
      flex-direction: column;
      gap: 2px;
      overflow: hidden;
    }

    .user-name {
      font-size: 14px;
      font-weight: 700;
      color: var(--text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .user-subtitle {
      font-size: 11px;
      color: var(--text-dim);
      font-weight: 600;
      letter-spacing: 0.05em;
      text-transform: uppercase;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .user-badge {
      font-size: 10px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 4px;
      text-transform: uppercase;
      align-self: flex-start;
      margin-top: 2px;
    }
    
    .badge-purple { background: rgba(168, 85, 247, 0.15); color: #c084fc; }
    .badge-blue { background: rgba(59, 130, 246, 0.15); color: #60a5fa; }
    .badge-green { background: rgba(34, 197, 94, 0.15); color: #4ade80; }

    /* Sizes */
    .sm .avatar-img, .sm .avatar-initials, .sm .avatar-fallback { width: 32px; height: 32px; font-size: 14px; border-radius: 8px; }
    .md .avatar-img, .md .avatar-initials, .md .avatar-fallback { width: 40px; height: 40px; font-size: 16px; border-radius: 12px; }
    .lg .avatar-img, .lg .avatar-initials, .lg .avatar-fallback { width: 56px; height: 56px; font-size: 20px; border-radius: 16px; }
  `]
})
export class UserIdentityComponent implements OnChanges {
  @Input() name?: string;
  @Input() profileImage?: string;
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() showDetails: boolean = false;
  @Input() subtitle?: string;
  @Input() roleBadge?: { text: string, color: 'purple' | 'blue' | 'green' };

  imageLoadError = false;
  initials = '';

  private readonly colors = [
    'linear-gradient(135deg, #6366f1, #a855f7)', // Indigo-Purple
    'linear-gradient(135deg, #ec4899, #f43f5e)', // Pink-Rose
    'linear-gradient(135deg, #14b8a6, #06b6d4)', // Teal-Cyan
    'linear-gradient(135deg, #f59e0b, #ef4444)', // Amber-Red
    'linear-gradient(135deg, #3b82f6, #2dd4bf)', // Blue-Teal
  ];

  ngOnChanges() {
    this.imageLoadError = false;
    this.generateInitials();
  }

  onImageError() {
    this.imageLoadError = true;
  }

  private generateInitials() {
    if (!this.name) {
      this.initials = '';
      return;
    }

    const parts = this.name.trim().split(/\s+/);
    if (parts.length === 1) {
      this.initials = parts[0].substring(0, 2).toUpperCase();
    } else {
      this.initials = (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
  }

  getDeterministicColor(): string {
    if (!this.name) return this.colors[0];
    let hash = 0;
    for (let i = 0; i < this.name.length; i++) {
      hash = this.name.charCodeAt(i) + ((hash << 5) - hash);
    }
    const index = Math.abs(hash) % this.colors.length;
    return this.colors[index];
  }
}
