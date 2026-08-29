import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserIdentityComponent } from '../user-identity/user-identity.component';

@Component({
  selector: 'app-seller-identity',
  standalone: true,
  imports: [CommonModule, UserIdentityComponent],
  template: `
    <app-user-identity
      [name]="name"
      [profileImage]="imageUrl"
      [subtitle]="subtitle"
      [roleBadge]="{text: status || 'VERIFIED SELLER', color: 'purple'}"
      [showDetails]="true"
      size="md">
    </app-user-identity>
  `
})
export class SellerIdentityComponent {
  @Input() name = '';
  @Input() imageUrl?: string;
  @Input() subtitle = 'Seller Hub';
  @Input() status?: string; // VERIFIED SELLER, ACTIVE, etc.
}
