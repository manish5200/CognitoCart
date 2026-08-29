import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AddressService } from '../../../services/address.service';
import { ToastService } from '../../../services/toast.service';
import { AuthService } from '../../../services/auth.service';
import { UserIdentityComponent } from '../../shared/user-identity/user-identity.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, UserIdentityComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <h1 class="page-title" style="display:flex; align-items:center; gap:12px;">
          <div style="background:var(--grad-brand); border-radius:8px; padding:6px; display:flex; align-items:center; justify-content:center; box-shadow:0 4px 14px rgba(99,102,241,0.3);">
            <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
          </div>
          My Profile
        </h1>
      </div>

      <div class="grid-2">
        <!-- Account Info -->
        <div>
          <div class="glass-card" style="margin-bottom:24px;">
            <div class="card-body">
              <h3 style="margin-bottom:32px; display:flex; align-items:center; gap:8px; font-family:var(--font-head); font-size:1.4rem;">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:24px;height:24px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z" /></svg>
                Account Info
              </h3>
              <div style="display:flex; flex-direction:column; align-items:center; text-align:center;">
                <app-user-identity 
                  [name]="user?.name" 
                  [size]="'lg'" 
                  [showDetails]="false">
                </app-user-identity>
                <div style="margin-top:16px;">
                  <div style="font-size:1.4rem; font-weight:700; color:var(--text-primary); font-family:var(--font-head);">{{user?.name}}</div>
                  <div style="color:var(--text-muted); font-size:14px; margin-bottom:12px;">{{user?.email}}</div>
                  <span class="badge" [ngClass]="user?.role === 'ROLE_ADMIN' ? 'badge-purple' : (user?.role === 'ROLE_SELLER' ? 'badge-green' : 'badge-blue')">{{user?.role?.replace('ROLE_', '')}}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Address Book -->
        <div>
          <div class="glass-card">
            <div class="card-body">
              <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
                <h3 style="display:flex; align-items:center; gap:8px;">
                  <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:20px;height:20px;color:var(--primary);"><path stroke-linecap="round" stroke-linejoin="round" d="M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" /><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1 1 15 0Z" /></svg>
                  Address Book
                </h3>
                <button class="btn btn-primary btn-sm" (click)="showAddForm = !showAddForm" style="display:flex; align-items:center; gap:6px;">
                  <svg *ngIf="showAddForm" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18 18 6M6 6l12 12" /></svg>
                  <svg *ngIf="!showAddForm" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" /></svg>
                  {{showAddForm ? 'Cancel' : 'Add Address'}}
                </button>
              </div>

              <!-- Add/Edit Form -->
              <div *ngIf="showAddForm" class="address-form glass-card" style="margin-bottom:24px; background:var(--bg-card); border-color:var(--primary-transparent);">
                <div class="card-body" style="padding:24px;">
                  <h4 style="margin-bottom:16px; font-family:var(--font-head); color:var(--text-primary);">{{editingId ? 'Edit' : 'New'}} Address</h4>
                  <form [formGroup]="addrForm" (ngSubmit)="saveAddress()">
                    <div class="form-row form-row-2">
                      <div class="form-group">
                        <label class="form-label">Full Name *</label>
                        <input type="text" formControlName="fullName" class="form-input" placeholder="John Doe" />
                      </div>
                      <div class="form-group">
                        <label class="form-label">Phone *</label>
                        <input type="tel" formControlName="phoneNumber" class="form-input" />
                      </div>
                    </div>
                    <div class="form-group">
                      <label class="form-label">Address Line 1 *</label>
                      <input type="text" formControlName="streetAddress" class="form-input" placeholder="Building, Street" />
                    </div>
                    <div class="form-group">
                      <label class="form-label">Address Line 2</label>
                      <input type="text" formControlName="addressLine2" class="form-input" placeholder="Landmark (optional)" />
                    </div>
                    <div class="form-row form-row-3">
                      <div class="form-group">
                        <label class="form-label">City *</label>
                        <input type="text" formControlName="city" class="form-input" />
                      </div>
                      <div class="form-group">
                        <label class="form-label">State *</label>
                        <input type="text" formControlName="state" class="form-input" />
                      </div>
                      <div class="form-group">
                        <label class="form-label">Pincode *</label>
                        <input type="text" formControlName="zipCode" class="form-input" />
                      </div>
                    </div>
                    <div style="display:flex; gap:8px; align-items:center; margin-bottom:16px;">
                      <input type="checkbox" formControlName="isDefault" id="isDefault" />
                      <label for="isDefault" style="font-size:14px; color:var(--text-muted); cursor:pointer;">Set as default address</label>
                    </div>
                    <div style="display:flex; gap:8px;">
                      <button type="submit" class="btn btn-primary" [disabled]="saving">
                        {{saving ? 'Saving...' : 'Save Address'}}
                      </button>
                      <button type="button" class="btn btn-secondary" (click)="cancelEdit()">Cancel</button>
                    </div>
                  </form>
                </div>
              </div>

              <!-- Address List -->
              <div class="loading-center" *ngIf="loadingAddr" style="min-height:100px;"><div class="spinner spinner-sm"></div></div>

              <div *ngFor="let addr of addresses" class="address-item">
                <div class="addr-content">
                  <div style="display:flex; gap:8px; align-items:center; margin-bottom:4px;">
                    <span style="font-weight:600; color:#fff;">{{addr.fullName}}</span>
                    <span class="badge badge-green" *ngIf="addr.isDefault" style="display:inline-flex; align-items:center; gap:4px;">
                      <svg fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" style="width:12px;height:12px;"><path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" /></svg>
                      Default
                    </span>
                  </div>
                  <div style="font-size:13px; color:var(--text-muted);">{{addr.streetAddress}}<span *ngIf="addr.addressLine2">, {{addr.addressLine2}}</span></div>
                  <div style="font-size:13px; color:var(--text-muted);">{{addr.city}}, {{addr.state}} - {{addr.zipCode}}</div>
                  <div style="font-size:13px; color:var(--text-muted);">📞 {{addr.phoneNumber}}</div>
                </div>
                <div class="addr-actions" style="display:flex; gap:4px;">
                  <button class="btn-icon" (click)="editAddress(addr)" title="Edit" style="display:flex; align-items:center; justify-content:center;">
                    <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="m16.862 4.487 1.687-1.688a1.875 1.875 0 1 1 2.652 2.652L10.582 16.07a4.5 4.5 0 0 1-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 0 1 1.13-1.897l8.932-8.931Zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0 1 15.75 21H5.25A2.25 2.25 0 0 1 3 18.75V8.25A2.25 2.25 0 0 1 5.25 6H10" /></svg>
                  </button>
                  <button class="btn-icon" (click)="setDefault(addr)" title="Set Default" *ngIf="!addr.isDefault" style="display:flex; align-items:center; justify-content:center;">
                    <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M11.48 3.499a.562.562 0 0 1 1.04 0l2.125 5.111a.563.563 0 0 0 .475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 0 0-.182.557l1.285 5.385a.562.562 0 0 1-.84.61l-4.725-2.885a.562.562 0 0 0-.586 0L6.982 20.54a.562.562 0 0 1-.84-.61l1.285-5.386a.562.562 0 0 0-.182-.557l-4.204-3.602a.562.562 0 0 1 .321-.988l5.518-.442a.563.563 0 0 0 .475-.345L11.48 3.5Z" /></svg>
                  </button>
                  <button class="btn-icon" (click)="deleteAddress(addr)" title="Delete" style="color:var(--danger); display:flex; align-items:center; justify-content:center;">
                    <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" /></svg>
                  </button>
                </div>
              </div>

              <div *ngIf="!loadingAddr && addresses.length === 0" class="empty-state" style="min-height:150px; padding:32px; display:flex; flex-direction:column; align-items:center; justify-content:center; color:var(--text-muted);">
                <svg fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" style="width:48px;height:48px;margin-bottom:16px;color:var(--text-dim);"><path stroke-linecap="round" stroke-linejoin="round" d="M15 10.5a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" /><path stroke-linecap="round" stroke-linejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1 1 15 0Z" /></svg>
                <div class="empty-title" style="font-size:18px; font-weight:600;">No addresses yet</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .address-item {
      display: flex; align-items: flex-start; justify-content: space-between; gap: 12px;
      padding: 16px; border: 1px solid var(--border-default); border-radius: 12px;
      margin-bottom: 12px; transition: var(--transition); background: var(--bg-surface);
    }
    .address-item:hover { border-color: rgba(99,102,241,0.5); transform: translateY(-1px); box-shadow: var(--shadow-sm); }
    .addr-actions { display: flex; gap: 4px; flex-shrink: 0; }
    
    .badge-purple { background: rgba(168, 85, 247, 0.15); color: #c084fc; padding: 4px 8px; border-radius: 6px; font-size: 12px; font-weight: 700; }
    .badge-blue { background: rgba(59, 130, 246, 0.15); color: #60a5fa; padding: 4px 8px; border-radius: 6px; font-size: 12px; font-weight: 700; }
    .badge-green { background: rgba(34, 197, 94, 0.15); color: #4ade80; padding: 4px 8px; border-radius: 6px; font-size: 12px; font-weight: 700; }
  `]
})
export class ProfileComponent implements OnInit {
  addresses: any[] = [];
  loadingAddr = true;
  showAddForm = false;
  editingId: string | null = null;
  saving = false;
  addrForm: FormGroup;

  constructor(
    private addressService: AddressService,
    private toast: ToastService,
    public auth: AuthService,
    private fb: FormBuilder
  ) {
    this.addrForm = this.fb.group({
      fullName: ['', Validators.required],
      phoneNumber: ['', Validators.required],
      streetAddress: ['', Validators.required],
      addressLine2: [''],
      city: ['', Validators.required],
      state: ['', Validators.required],
      zipCode: ['', Validators.required],
      country: ['India'], // Hidden by default, required by backend
      isDefault: [false]
    });
  }

  get user() { return this.auth.getCurrentUser(); }

  ngOnInit(): void { this.loadAddresses(); }

  loadAddresses(): void {
    this.loadingAddr = true;
    this.addressService.getAll().subscribe({
      next: a => { this.addresses = a; this.loadingAddr = false; },
      error: () => { this.loadingAddr = false; }
    });
  }

  saveAddress(): void {
    if (this.addrForm.invalid) { this.addrForm.markAllAsTouched(); return; }
    this.saving = true;
    const obs = this.editingId
      ? this.addressService.update(this.editingId, this.addrForm.value)
      : this.addressService.add(this.addrForm.value);

    obs.subscribe({
      next: () => {
        this.toast.success(this.editingId ? 'Address updated!' : 'Address added!');
        this.saving = false; this.cancelEdit(); this.loadAddresses();
      },
      error: (e) => { this.saving = false; this.toast.error(e.error?.message || 'Failed to save'); }
    });
  }

  editAddress(addr: any): void {
    this.editingId = addr.publicId;
    this.addrForm.patchValue(addr);
    this.showAddForm = true;
  }

  cancelEdit(): void { this.editingId = null; this.addrForm.reset(); this.showAddForm = false; }

  deleteAddress(addr: any): void {
    if (!confirm('Delete this address?')) return;
    this.addressService.delete(addr.publicId).subscribe({
      next: () => { this.toast.success('Address deleted'); this.loadAddresses(); },
      error: () => this.toast.error('Failed to delete')
    });
  }

  setDefault(addr: any): void {
    this.addressService.setDefault(addr.publicId).subscribe({
      next: () => { this.toast.success('Default address updated'); this.loadAddresses(); },
      error: () => this.toast.error('Failed to update')
    });
  }
}
