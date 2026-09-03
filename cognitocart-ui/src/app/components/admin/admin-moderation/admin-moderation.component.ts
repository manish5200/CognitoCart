import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../services/product.service';

interface PendingProduct {
  publicId: string;
  productName: string;
  sellerId: number;
  approvalStatus: string;
  createdAt: Date;
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
    const payload = { status: 'APPROVED', reason: 'Approved by Admin' };
    this.productService.moderateProduct(product.publicId, payload).subscribe({
      next: () => {
        this.pendingProducts = this.pendingProducts.filter(p => p.publicId !== product.publicId);
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
    
    const payload = { status: 'REQUIRES_CHANGES', reason: this.rejectReason };
    this.productService.moderateProduct(this.selectedProduct.publicId, payload).subscribe({
      next: () => {
        this.pendingProducts = this.pendingProducts.filter(p => p.publicId !== this.selectedProduct?.publicId);
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
    
    this.productService.getModerationHistory(product.publicId).subscribe({
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
}

