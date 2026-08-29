import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);

  if (!auth.isLoggedIn()) {
    toast.show('Please login to continue', 'warning');
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  const requiredRole = route.data?.['role'] as string;
  if (requiredRole) {
    const user = auth.getCurrentUser();
    if (user?.role !== requiredRole) {
      toast.show('Access denied. Insufficient permissions.', 'error');
      router.navigate(['/']);
      return false;
    }
  }

  return true;
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);

  if (!auth.isAdmin()) {
    toast.show('Admin access required', 'error');
    router.navigate(['/']);
    return false;
  }
  return true;
};

export const sellerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);

  if (!auth.isSeller()) {
    toast.show('Seller access required', 'error');
    router.navigate(['/']);
    return false;
  }
  return true;
};

export const customerGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);

  if (!auth.isLoggedIn()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }
  if (!auth.isCustomer()) {
    toast.show('Customer access required', 'error');
    router.navigate(['/']);
    return false;
  }
  return true;
};
