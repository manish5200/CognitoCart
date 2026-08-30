import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';
import { AdminShellComponent } from '../admin-shell/admin-shell.component';

@Component({
  selector: 'app-admin-reviews',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminShellComponent],
  template: `
    <app-admin-shell>
      <div class="page-header" style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <h1 class="page-title" style="display:flex; align-items:center; gap:12px;">
            <div style="background:var(--grad-brand); border-radius:8px; padding:6px; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 14px rgba(99,102,241,0.3);">
              <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 9a2 2 0 0 1-2 2H6l-4 4V4c0-1.1.9-2 2-2h8a2 2 0 0 1 2 2v5Z"/><path d="M18 9h2a2 2 0 0 1 2 2v11l-4-4h-6a2 2 0 0 1-2-2v-1"/></svg>
            </div>
            Review Moderation
          </h1>
          <p class="page-subtitle">Monitor and moderate all customer product reviews</p>
        </div>
      </div>

      <div class="loading-center" *ngIf="loading"><div class="spinner"></div></div>

      <div *ngIf="!loading && reviews.length === 0" class="empty-state">
        <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:48px;height:48px;margin-bottom:16px;color:var(--text-dim);"><path stroke-linecap="round" stroke-linejoin="round" d="M12 20.25c4.97 0 9-3.694 9-8.25s-4.03-8.25-9-8.25S3 7.43 3 12c0 2.104.859 4.023 2.273 5.48.432.447.74 1.04.586 1.641a4.483 4.483 0 0 1-.923 1.785A5.969 5.969 0 0 0 6 21c1.282 0 2.47-.402 3.445-1.087.81.22 1.668.337 2.555.337Z" /></svg>
        <div class="empty-title">No reviews found!</div>
      </div>

      <div *ngIf="!loading && reviews.length > 0" style="display:flex; flex-direction:column; gap:16px;">
        <div *ngFor="let review of reviews" class="review-card" [class.low-rating]="review.rating <= 2">
          <div class="review-header">
            <div class="reviewer-info">
              <div class="avatar">{{ review.reviewerName.charAt(0) }}</div>
              <div>
                <div style="font-weight:600; color:var(--text-primary);">{{ review.reviewerName }}</div>
                <div style="font-size:12px; color:var(--text-muted);">
                  {{ review.createdAt | date:'medium' }}
                  <span *ngIf="review.verifiedPurchase" class="verified-badge">✓ Verified</span>
                </div>
              </div>
            </div>
            <div class="rating-badge" [class.high]="review.rating >= 4" [class.med]="review.rating === 3" [class.low]="review.rating <= 2">
              {{ review.rating }} ★
            </div>
          </div>
          
          <div class="review-body">
            <div style="font-size:13px; color:var(--text-secondary); margin-bottom:8px; font-weight:500;">
              Product: <span style="color:var(--brand);">{{ review.productName }}</span>
            </div>
            <p>{{ review.comment }}</p>
            <div *ngIf="review.imageUrls && review.imageUrls.length > 0" class="review-images">
              <img *ngFor="let img of review.imageUrls" [src]="img" alt="Review Image" />
            </div>
          </div>

          <div class="review-footer">
            <button class="btn btn-danger btn-sm" (click)="deleteReview(review.reviewPublicId)" style="display:flex; align-items:center; gap:6px;">
              <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" /></svg>
              Delete Review
            </button>
          </div>
        </div>
        
        <!-- Pagination -->
        <div class="pagination" *ngIf="totalPages > 1">
          <button class="btn btn-secondary btn-sm" [disabled]="currentPage === 0" (click)="loadPage(currentPage - 1)">Previous</button>
          <span style="font-size:14px; color:var(--text-muted); font-weight:500;">Page {{ currentPage + 1 }} of {{ totalPages }}</span>
          <button class="btn btn-secondary btn-sm" [disabled]="currentPage >= totalPages - 1" (click)="loadPage(currentPage + 1)">Next</button>
        </div>
      </div>
    </app-admin-shell>
  `,
  styles: [`
    .review-card {
      background: #fff; border-radius: 12px; border: 1px solid var(--border-subtle); padding: 20px;
      transition: all 0.2s; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
    }
    .review-card.low-rating {
      border-left: 4px solid var(--danger);
    }
    .review-header {
      display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;
    }
    .reviewer-info { display: flex; align-items: center; gap: 12px; }
    .avatar {
      width: 40px; height: 40px; border-radius: 50%; background: var(--bg-hover); color: var(--brand);
      display: flex; align-items: center; justify-content: center; font-weight: bold; font-size: 16px;
    }
    .verified-badge {
      color: var(--success); font-weight: 600; font-size: 11px; background: rgba(16,185,129,0.1);
      padding: 2px 6px; border-radius: 4px; margin-left: 6px;
    }
    .rating-badge {
      padding: 4px 10px; border-radius: 20px; font-weight: bold; font-size: 14px;
    }
    .rating-badge.high { background: rgba(16,185,129,0.1); color: var(--success); }
    .rating-badge.med { background: rgba(245,158,11,0.1); color: var(--warning); }
    .rating-badge.low { background: rgba(239,68,68,0.1); color: var(--danger); }
    
    .review-body p { margin: 0; color: var(--text-primary); line-height: 1.5; font-size: 14px; }
    .review-images { display: flex; gap: 8px; margin-top: 12px; }
    .review-images img {
      width: 60px; height: 60px; object-fit: cover; border-radius: 6px; border: 1px solid var(--border-subtle);
    }
    .review-footer { margin-top: 16px; display: flex; justify-content: flex-end; padding-top: 16px; border-top: 1px dashed var(--border-subtle); }
    
    .pagination { display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 24px; }
  `]
})
export class AdminReviewsComponent implements OnInit {
  loading = false;
  reviews: any[] = [];
  currentPage = 0;
  totalPages = 1;
  
  constructor(private adminService: AdminService, private toast: ToastService) {}

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(page: number): void {
    this.loading = true;
    this.adminService.getAllReviews(page, 20).subscribe({
      next: (data) => {
        this.reviews = data.content;
        this.currentPage = data.pageable.pageNumber;
        this.totalPages = data.totalPages;
        this.loading = false;
      },
      error: () => {
        this.toast.error('Failed to load reviews');
        this.loading = false;
      }
    });
  }

  deleteReview(publicId: string): void {
    if (confirm('Are you sure you want to delete this review? This action cannot be undone.')) {
      this.adminService.deleteReview(publicId).subscribe({
        next: () => {
          this.toast.success('Review deleted successfully');
          this.loadPage(this.currentPage);
        },
        error: () => this.toast.error('Failed to delete review')
      });
    }
  }
}
