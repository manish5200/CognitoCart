import { Routes } from '@angular/router';
import { authGuard, adminGuard, sellerGuard, customerGuard } from './guards/auth.guard';

export const routes: Routes = [
  // Public
  { path: '', loadComponent: () => import('./components/home/home.component').then(m => m.HomeComponent) },
  { path: 'login', loadComponent: () => import('./components/auth/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./components/auth/register/register.component').then(m => m.RegisterComponent) },
  { path: 'register/seller', loadComponent: () => import('./components/auth/register-seller/register-seller.component').then(m => m.RegisterSellerComponent) },
  { path: 'verify-email', loadComponent: () => import('./components/auth/verify-email/verify-email.component').then(m => m.VerifyEmailComponent) },
  { path: 'oauth2/callback', loadComponent: () => import('./components/auth/oauth2-callback.component').then(m => m.Oauth2CallbackComponent) },

  { path: 'forgot-password', loadComponent: () => import('./components/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent) },
  { path: 'reset-password', loadComponent: () => import('./components/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent) },
  { path: 'products', loadComponent: () => import('./components/products/product-list/product-list.component').then(m => m.ProductListComponent) },
  { path: 'product/:slug', loadComponent: () => import('./components/products/product-detail/product-detail.component').then(m => m.ProductDetailComponent) },
  { path: 'search', loadComponent: () => import('./components/products/product-list/product-list.component').then(m => m.ProductListComponent) },
  { path: 'flash-sales', loadComponent: () => import('./components/home/flash-sales/flash-sales.component').then(m => m.FlashSalesComponent) },
  { path: 'track', loadComponent: () => import('./components/orders/guest-tracking/guest-tracking.component').then(m => m.GuestTrackingComponent) },

  // Customer
  { path: 'cart', loadComponent: () => import('./components/cart/cart.component').then(m => m.CartComponent) },
  { path: 'checkout', canActivate: [customerGuard], loadComponent: () => import('./components/checkout/checkout.component').then(m => m.CheckoutComponent) },
  { path: 'orders', canActivate: [customerGuard], loadComponent: () => import('./components/orders/order-history/order-history.component').then(m => m.OrderHistoryComponent) },
  { path: 'orders/:id', canActivate: [customerGuard], loadComponent: () => import('./components/orders/order-detail/order-detail.component').then(m => m.OrderDetailComponent) },
  { path: 'dashboard', canActivate: [customerGuard], loadComponent: () => import('./components/customer/customer-home/customer-home.component').then(m => m.CustomerHomeComponent) },
  { path: 'wishlist', canActivate: [customerGuard], loadComponent: () => import('./components/customer/wishlist/wishlist.component').then(m => m.WishlistComponent) },
  { path: 'profile', canActivate: [authGuard], loadComponent: () => import('./components/customer/profile/profile.component').then(m => m.ProfileComponent) },

  // Seller
  { path: 'seller', canActivate: [sellerGuard], loadComponent: () => import('./components/seller/seller-dashboard/seller-dashboard.component').then(m => m.SellerDashboardComponent) },
  { path: 'seller/products', canActivate: [sellerGuard], loadComponent: () => import('./components/seller/seller-products/seller-products.component').then(m => m.SellerProductsComponent) },
  { path: 'seller/products/:id', canActivate: [sellerGuard], loadComponent: () => import('./components/products/product-details-workspace/product-details-workspace.component').then(m => m.ProductDetailsWorkspaceComponent) },
  { path: 'seller/orders', canActivate: [sellerGuard], loadComponent: () => import('./components/seller/seller-orders/seller-orders.component').then(m => m.SellerOrdersComponent) },
  { path: 'seller/analytics', canActivate: [sellerGuard], loadComponent: () => import('./components/seller/seller-analytics/seller-analytics.component').then(m => m.SellerAnalyticsComponent) },
  { path: 'seller/sales', canActivate: [sellerGuard], loadComponent: () => import('./components/seller/seller-sales/seller-sales.component').then(m => m.SellerSalesComponent) },
  { path: 'seller/policies', canActivate: [sellerGuard], loadComponent: () => import('./components/seller/seller-policies/seller-policies.component').then(m => m.SellerPoliciesComponent) },

  // Admin
  { path: 'admin', canActivate: [adminGuard], loadComponent: () => import('./components/admin/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent) },
  { path: 'admin/categories', canActivate: [adminGuard], loadComponent: () => import('./components/admin/admin-categories/admin-categories.component').then(m => m.AdminCategoriesComponent) },
  { path: 'admin/products/:id', canActivate: [adminGuard], loadComponent: () => import('./components/products/product-details-workspace/product-details-workspace.component').then(m => m.ProductDetailsWorkspaceComponent) },
  { path: 'admin/orders', canActivate: [adminGuard], loadComponent: () => import('./components/admin/admin-orders/admin-orders.component').then(m => m.AdminOrdersComponent) },
  { path: 'admin/returns', canActivate: [adminGuard], loadComponent: () => import('./components/admin/admin-returns/admin-returns.component').then(m => m.AdminReturnsComponent) },
  { path: 'admin/kyc', canActivate: [adminGuard], loadComponent: () => import('./components/admin/admin-kyc/admin-kyc.component').then(m => m.AdminKycComponent) },
  { path: 'admin/coupons', canActivate: [adminGuard], loadComponent: () => import('./components/admin/admin-coupons/admin-coupons.component').then(m => m.AdminCouponsComponent) },
  { path: 'admin/sales', canActivate: [adminGuard], loadComponent: () => import('./components/admin/admin-sales/admin-sales.component').then(m => m.AdminSalesComponent) },
  { path: 'admin/webhooks', canActivate: [adminGuard], loadComponent: () => import('./components/admin/admin-webhooks/admin-webhooks.component').then(m => m.AdminWebhooksComponent) },
  { path: 'admin/intelligence', canActivate: [adminGuard], loadComponent: () => import('./components/admin/admin-intelligence/admin-intelligence.component').then(m => m.AdminIntelligenceComponent) },
  { path: 'admin/reviews', canActivate: [adminGuard], loadComponent: () => import('./components/admin/admin-reviews/admin-reviews.component').then(m => m.AdminReviewsComponent) },

  { path: '**', redirectTo: '' }
];
