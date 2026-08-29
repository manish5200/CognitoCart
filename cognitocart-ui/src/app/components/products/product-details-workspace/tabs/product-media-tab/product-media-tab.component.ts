import { Component, Input, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../../../../services/product.service';
import { ToastService } from '../../../../../services/toast.service';

@Component({
  selector: 'app-product-media-tab',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="media-tab">
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:24px;">
        <h2 style="margin:0; color:#fff;">Media & Images</h2>
        <input type="file" #fileInput hidden (change)="onFileSelected($event)" accept="image/jpeg,image/png,image/webp">
        <button class="btn btn-primary" (click)="fileInput.click()" [disabled]="uploading">
          <span *ngIf="uploading" class="spinner spinner-sm"></span>
          <svg *ngIf="!uploading" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><circle cx="8.5" cy="8.5" r="1.5"></circle><polyline points="21 15 16 10 5 21"></polyline></svg>
          {{ uploading ? 'Uploading...' : 'Upload Media' }}
        </button>
      </div>
      
      <div class="upload-zone" (click)="fileInput.click()" (dragover)="onDragOver($event)" (dragleave)="onDragLeave($event)" (drop)="onDrop($event)" [class.drag-over]="isDragging">
        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color:var(--text-muted); margin-bottom:12px;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="17 8 12 3 7 8"></polyline><line x1="12" y1="3" x2="12" y2="15"></line></svg>
        <h3 style="color:#fff; margin-bottom:8px;">Drag & Drop Images Here</h3>
        <p style="color:var(--text-muted); font-size:13px; margin:0;">Supports JPG, PNG, WEBP. Max 5MB per file.</p>
      </div>

      <div class="media-grid" *ngIf="product?.imageUrls?.length">
        <div class="media-card" *ngFor="let img of product.imageUrls; let i = index">
          <img [src]="img" alt="Product Image">
          <div class="media-actions">
            <button class="icon-btn" title="Set as Primary" *ngIf="i !== 0"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg></button>
            <button class="icon-btn danger" title="Delete" (click)="deleteImage(img)"><svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg></button>
          </div>
          <div class="primary-badge" *ngIf="i === 0">Primary</div>
        </div>
      </div>
      
      <div *ngIf="!product?.imageUrls?.length" style="padding:40px; text-align:center; color:var(--text-muted);">
        No media uploaded yet.
      </div>
    </div>
  `,
  styles: [`
    .media-tab { animation: fadeIn 0.3s ease; }
    .btn { display: inline-flex; align-items: center; gap: 8px; padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; border: none; }
    .btn-primary { background: var(--primary); color: #fff; }
    .btn-primary:hover { background: var(--primary-hover); transform: translateY(-1px); box-shadow: 0 4px 12px rgba(99,102,241,0.3); }
    
    .upload-zone {
      border: 2px dashed rgba(255,255,255,0.1); border-radius: 12px;
      padding: 48px; text-align: center; background: rgba(255,255,255,0.01);
      transition: all 0.2s; margin-bottom: 32px; cursor: pointer;
    }
    .upload-zone.drag-over { border-color: var(--primary); background: rgba(99,102,241,0.1); }
    
    .media-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 20px; }
    .media-card {
      position: relative; border-radius: 12px; overflow: hidden;
      aspect-ratio: 1; border: 1px solid rgba(255,255,255,0.05);
      background: var(--bg-card);
    }
    .media-card img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s; }
    .media-card:hover img { transform: scale(1.05); }
    
    .media-actions {
      position: absolute; top: 12px; right: 12px; display: flex; gap: 8px;
      opacity: 0; transition: opacity 0.2s;
    }
    .media-card:hover .media-actions { opacity: 1; }
    
    .icon-btn {
      width: 28px; height: 28px; border-radius: 6px; background: rgba(0,0,0,0.6);
      color: #fff; border: 1px solid rgba(255,255,255,0.1); cursor: pointer;
      display: flex; align-items: center; justify-content: center; backdrop-filter: blur(4px);
    }
    .icon-btn:hover { background: rgba(255,255,255,0.2); }
    .icon-btn.danger:hover { background: rgba(239,68,68,0.8); border-color: rgba(239,68,68,1); }
    
    .primary-badge {
      position: absolute; bottom: 12px; left: 12px; background: rgba(16,185,129,0.9);
      color: #fff; font-size: 10px; font-weight: 700; text-transform: uppercase;
      padding: 4px 8px; border-radius: 4px; letter-spacing: 0.5px;
    }

    @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class ProductMediaTabComponent {
  @Input() product: any = {};
  @ViewChild('fileInput') fileInput!: ElementRef;
  
  uploading = false;
  isDragging = false;

  constructor(private productService: ProductService, private toast: ToastService) {}

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isDragging = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragging = false;
    
    if (event.dataTransfer?.files?.length) {
      this.handleFile(event.dataTransfer.files[0]);
    }
  }

  onFileSelected(event: any) {
    if (event.target.files?.length) {
      this.handleFile(event.target.files[0]);
    }
  }

  private handleFile(file: File) {
    if (!this.product || !this.product.productPublicId) return;
    
    if (!file.type.startsWith('image/')) {
      this.toast.error('Only image files are allowed');
      return;
    }
    
    if (file.size > 5 * 1024 * 1024) {
      this.toast.error('File size must be under 5MB');
      return;
    }

    this.uploading = true;
    this.productService.uploadImage(this.product.productPublicId, file).subscribe({
      next: (res) => {
        this.uploading = false;
        if (!this.product.imageUrls) this.product.imageUrls = [];
        this.product.imageUrls.push(res.imageUrl);
        this.toast.success('Image uploaded successfully');
        if(this.fileInput) this.fileInput.nativeElement.value = '';
      },
      error: (e) => {
        this.uploading = false;
        this.toast.error(e.error?.message || 'Failed to upload image');
        if(this.fileInput) this.fileInput.nativeElement.value = '';
      }
    });
  }

  deleteImage(imageUrl: string) {
    if (!this.product || !this.product.productPublicId || !confirm('Are you sure you want to delete this image?')) return;
    
    // Extract Cloudinary publicId from URL
    // e.g. https://res.cloudinary.com/cloud/image/upload/v12345/products/img.jpg -> products/img
    let publicId = imageUrl;
    const parts = imageUrl.split('/upload/');
    if (parts.length > 1) {
      let withoutVersion = parts[1].replace(/^v\d+\//, '');
      const lastDotIndex = withoutVersion.lastIndexOf('.');
      if (lastDotIndex !== -1) {
        withoutVersion = withoutVersion.substring(0, lastDotIndex);
      }
      publicId = withoutVersion;
    }

    this.productService.deleteImage(this.product.productPublicId, publicId).subscribe({
      next: () => {
        this.product.imageUrls = this.product.imageUrls.filter((url: string) => url !== imageUrl);
        this.toast.success('Image deleted successfully');
      },
      error: (e) => {
        this.toast.error(e.error?.message || 'Failed to delete image');
      }
    });
  }
}
