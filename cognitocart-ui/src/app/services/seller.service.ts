import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SellerService {
  private readonly API = 'http://localhost:8080/api/v1/sellers';
  private readonly SALE_API = 'http://localhost:8080/api/v1/seller/sales';

  constructor(private http: HttpClient) {}

  // ── Dashboard ──
  getDashboard(): Observable<any> { return this.http.get(`${this.API}/dashboard`); }
  getDashboardStats(): Observable<any> { return this.http.get(`${this.API}/dashboard`); }

  // ── Orders ──
  getOrders(params?: { status?: string; from?: string; to?: string; page?: number; size?: number }): Observable<any> {
    let p = new HttpParams();
    if (params?.status) p = p.set('status', params.status);
    if (params?.from) p = p.set('from', params.from);
    if (params?.to) p = p.set('to', params.to);
    p = p.set('page', (params?.page ?? 0).toString());
    p = p.set('size', (params?.size ?? 20).toString());
    return this.http.get(`${this.API}/orders`, { params: p });
  }

  getOrderDetail(orderPublicId: string): Observable<any> {
    return this.http.get(`${this.API}/orders/${orderPublicId}`);
  }

  searchOrder(orderNumber: string): Observable<any> {
    return this.http.get(`${this.API}/orders/search?orderNumber=${orderNumber}`);
  }

  markAsPacked(orderPublicId: string): Observable<any> {
    return this.http.patch(`${this.API}/orders/${orderPublicId}/pack`, {});
  }

  // ── Analytics ──
  getProductAnalytics(): Observable<any> { return this.http.get(`${this.API}/analytics/products`); }

  downloadRevenueCsvUrl(): string { return `${this.API}/reports/revenue.csv`; }

  downloadRevenueCsv(): Observable<Blob> {
    return this.http.get(`${this.API}/reports/revenue.csv`, { responseType: 'blob' });
  }

  // ── Return Policies ──
  getPolicies(): Observable<any> { return this.http.get(`${this.API}/return-policy`); }

  createPolicy(data: any): Observable<any> { return this.http.post(`${this.API}/return-policy`, data); }

  updatePolicy(policyPublicId: string, data: any): Observable<any> {
    return this.http.put(`${this.API}/return-policy/${policyPublicId}`, data);
  }

  deletePolicy(policyPublicId: string): Observable<any> {
    return this.http.delete(`${this.API}/return-policy/${policyPublicId}`);
  }

  // ── Flash Sales ──
  submitFlashSaleItem(data: any): Observable<any> { return this.http.post(`${this.SALE_API}/items`, data); }

  getMySubmissions(): Observable<any> { return this.http.get(`${this.SALE_API}/items`); }

  getMyVariants(): Observable<any> { return this.http.get(`${this.API}/variants`); }

  uploadBulkSaleCsv(eventPublicId: string, file: File): Observable<any> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post(`${this.SALE_API}/${eventPublicId}/bulk-upload`, form, { responseType: 'text' });
  }
}
