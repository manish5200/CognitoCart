import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../services/product.service';

interface PendingProduct {
  productPublicId: string;
  productName: string;
  storeName?: string;
  price?: number;
  discountPrice?: number;
  createdAt?: Date;
  insightLastGenerated?: Date;
  mediaGallery?: any[];
}

interface ModerationHistory {
  actorType: string;
  action: string;
  approvalStatusFrom: string;
  approvalStatusTo: string;
  reason: string;
  createdAt: Date;
}

@Component({
  selector: 'app-admin-moderation',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './admin-moderation.component.html',
  styleUrls: ['./admin-moderation.component.scss']
})
export class AdminModerationComponent implements OnInit {
  
  pendingProducts: PendingProduct[] = [];
  isLoading = false;

  // Modal States
  showRejectModal = false;
  showHistoryDrawer = false;
  selectedProduct: PendingProduct | null = null;
  rejectReason = '';
  
  mockHistory: ModerationHistory[] = [];

  constructor(private productService: ProductService) {}

  ngOnInit(): void {
    this.loadPendingProducts();
  }

  loadPendingProducts(): void {
    this.isLoading = true;
    this.productService.getPendingProducts(0, 50).subscribe({
      next: (res) => {
        this.pendingProducts = res.content || [];
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading pending products', err);
        this.isLoading = false;
      }
    });
  }

  approveProduct(product: PendingProduct): void {
    const payload = { action: 'APPROVED', reason: 'Approved by Admin' };
    this.productService.moderateProduct(product.productPublicId, payload).subscribe({
      next: () => {
        this.pendingProducts = this.pendingProducts.filter(p => p.productPublicId !== product.productPublicId);
      },
      error: (err) => console.error('Error approving product', err)
    });
  }

  openRejectModal(product: PendingProduct): void {
    this.selectedProduct = product;
    this.rejectReason = '';
    this.showRejectModal = true;
  }

  submitRejection(): void {
    if (!this.rejectReason.trim() || !this.selectedProduct) return;
    
    const payload = { action: 'REQUIRES_CHANGES', reason: this.rejectReason };
    this.productService.moderateProduct(this.selectedProduct.productPublicId, payload).subscribe({
      next: () => {
        this.pendingProducts = this.pendingProducts.filter(p => p.productPublicId !== this.selectedProduct?.productPublicId);
        this.closeModal();
      },
      error: (err) => console.error('Error rejecting product', err)
    });
  }

  closeModal(): void {
    this.showRejectModal = false;
    this.selectedProduct = null;
  }

  openHistory(product: PendingProduct): void {
    this.selectedProduct = product;
    this.showHistoryDrawer = true;
    
    this.productService.getModerationHistory(product.productPublicId).subscribe({
      next: (res) => {
        this.mockHistory = res;
      },
      error: (err) => console.error('Error loading history', err)
    });
  }

  closeHistory(): void {
    this.showHistoryDrawer = false;
    this.selectedProduct = null;
  }

  getPrimaryImage(product: PendingProduct): string {
    if (product.mediaGallery && product.mediaGallery.length > 0) {
      const primary = product.mediaGallery.find((m: any) => m.isPrimary);
      return primary ? primary.mediaUrl : product.mediaGallery[0].mediaUrl;
    }
    return 'assets/placeholder-image.png'; // Fallback
  }
}

