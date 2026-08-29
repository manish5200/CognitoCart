import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly API = 'https://cognitocart-api.onrender.com/api/v1/admin';
  private readonly SALE_API = 'https://cognitocart-api.onrender.com/api/v1/admin/sales';

  constructor(private http: HttpClient) {}

  // â”€â”€ Analytics â”€â”€
  getStats(pageNumber = 0, pageSize = 50): Observable<any> {
    return this.http.get(`${this.API}/stats?pageNumber=${pageNumber}&pageSize=${pageSize}`);
  }

  getAllOrders(pageNumber = 0, pageSize = 50): Observable<any[]> {
    return this.http.get<any[]>(`${this.API}/orders?pageNumber=${pageNumber}&pageSize=${pageSize}`);
  }

  getPlatformIntelligence(): Observable<any> {
    return this.http.get(`${this.API}/analytics/intelligence`);
  }

  getCategoryRevenue(): Observable<any> {
    return this.http.get(`${this.API}/analytics/category-revenue`);
  }

  getCustomerIntelligence(top = 10, churnAfterDays = 60): Observable<any> {
    return this.http.get(`${this.API}/analytics/customers?top=${top}&churnAfterDays=${churnAfterDays}`);
  }

  // ðŸ“¦ Orders ðŸ“¦
  changeOrderStatus(orderPublicId: string, targetStatus: string, comment?: string): Observable<any> {
    return this.http.patch(`${this.API}/orders/${orderPublicId}/status`, {
      orderStatus: targetStatus,
      comment: comment || `Status updated to ${targetStatus} via Admin UI`
    });
  }

  attachShipment(orderPublicId: string, payload: {
    courier: string; trackingNumber: string; trackingUrl?: string; dispatchedAt?: string;
  }): Observable<any> {
    return this.http.post(`${this.API}/${orderPublicId}/shipment`, payload);
  }

  getPendingReturns(): Observable<any> {
    return this.http.get(`${this.API}/orders/pending-returns`);
  }

  approveReturn(orderPublicId: string): Observable<any> {
    return this.http.put(`${this.API}/${orderPublicId}/approve-return`, {});
  }

  approveReplacement(orderPublicId: string): Observable<any> {
    return this.http.put(`${this.API}/${orderPublicId}/approve-replacement`, {});
  }

  rejectReturn(orderPublicId: string, adminComment = 'Rejected by admin'): Observable<any> {
    return this.http.put(`${this.API}/${orderPublicId}/reject-return?adminComment=${encodeURIComponent(adminComment)}`, {});
  }

  downloadInvoice(orderIdentifier: string): Observable<Blob> {
    return this.http.get(`${this.API}/orders/${orderIdentifier}/invoice`, { responseType: 'blob' });
  }

  // â”€â”€ Sellers & KYC â”€â”€
  getAllSellers(): Observable<any> { return this.http.get(`${this.API}/sellers`); }

  getPendingKycSellers(): Observable<any> { return this.http.get(`${this.API}/sellers/kyc/pending`); }

  updateKyc(sellerPublicId: string, action: string, comment: string): Observable<any> {
    return this.http.patch(`${this.API}/sellers/${sellerPublicId}/kyc`, { status: action, adminComment: comment });
  }

  getSellerAnalytics(sellerPublicId: string): Observable<any> {
    return this.http.get(`${this.API}/sellers/${sellerPublicId}/analytics`);
  }

  // â”€â”€ Coupons â”€â”€
  getCoupons(): Observable<any> { return this.http.get(`${this.API}/coupons`); }

  createCoupon(data: {
    code: string; discountType: string; discountValue: number; maxDiscountAmount?: number;
    minOrderAmount?: number; expiresAt?: string; maxUsage?: number;
  }): Observable<any> { return this.http.post(`${this.API}/coupons`, data); }

  toggleCoupon(couponPublicId: string): Observable<any> {
    return this.http.patch(`${this.API}/coupons/${couponPublicId}/toggle`, {}, { responseType: 'text' });
  }

  // â”€â”€ Webhooks DLQ â”€â”€
  getPendingWebhooks(): Observable<any> { return this.http.get(`${this.API}/webhooks/dlq/pending`); }

  replayWebhook(eventPublicId: string): Observable<any> {
    return this.http.post(`${this.API}/webhooks/dlq/${eventPublicId}/replay`, {}, { responseType: 'text' });
  }

  // â”€â”€ Flash Sales â”€â”€
  createSaleEvent(data: { eventName: string; description?: string; startTime: string; endTime: string }): Observable<any> {
    return this.http.post(`${this.SALE_API}/events`, data);
  }

  getAllSaleEvents(): Observable<any> { return this.http.get(`${this.SALE_API}/events`); }

  getEventSubmissions(eventPublicId: string): Observable<any> {
    return this.http.get(`${this.SALE_API}/events/${eventPublicId}/submissions`);
  }

  reviewSellerSubmission(itemPublicId: string, status: 'APPROVED' | 'REJECTED'): Observable<any> {
    return this.http.patch(`${this.SALE_API}/items/${itemPublicId}/review?status=${status}`, {}, { responseType: 'text' });
  }
}
