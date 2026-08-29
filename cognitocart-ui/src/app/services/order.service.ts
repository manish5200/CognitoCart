import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly API = 'http://localhost:8080/api/v1/orders';
  private readonly PAY_API = 'http://localhost:8080/api/v1/payments';

  constructor(private http: HttpClient) {}

  checkout(data: { addressPublicId: string; paymentMethod?: string }): Observable<any> {
    return this.http.post(`${this.API}/checkout`, data);
  }

  verifyPayment(data: { razorpayOrderId: string; razorpayPaymentId: string; razorpaySignature: string }): Observable<any> {
    return this.http.post(`${this.PAY_API}/verify`, data);
  }

  getHistory(page = 0, size = 10): Observable<any> {
    return this.http.get(`${this.API}/history?page=${page}&size=${size}&sort=orderDate,desc`);
  }
  getMyOrders(page = 0, size = 10): Observable<any> {
    return this.http.get(`${this.API}/history?page=${page}&size=${size}&sort=orderDate,desc`);
  }
  getDetail(orderIdentifier: string): Observable<any> {
    return this.http.get(`${this.API}/detail/${orderIdentifier}`);
  }

  cancel(orderIdentifier: string, refundDestination = 'ORIGINAL'): Observable<any> {
    return this.http.put(`${this.API}/${orderIdentifier}/cancel?refundDestination=${refundDestination}`, {});
  }

  requestReturn(orderIdentifier: string, requestData: any, images?: File[]): Observable<any> {
    const form = new FormData();
    form.append('request', JSON.stringify(requestData));
    if (images?.length) images.forEach(img => form.append('images', img));
    return this.http.post(`${this.API}/${orderIdentifier}/request-return`, form);
  }

  getTimeline(orderIdentifier: string): Observable<any> {
    return this.http.get(`${this.API}/${orderIdentifier}/timeline`);
  }

  downloadInvoice(orderIdentifier: string): Observable<Blob> {
    return this.http.get(`${this.API}/${orderIdentifier}/invoice`, { responseType: 'blob' });
  }
}
