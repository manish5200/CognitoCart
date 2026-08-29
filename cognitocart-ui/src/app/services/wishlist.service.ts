import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private readonly API = 'https://cognitocart-api.onrender.com/api/v1/wishlist';

  constructor(private http: HttpClient) {}

  getWishlist(): Observable<any> { return this.http.get(this.API); }
  getAll(): Observable<any> { return this.http.get(this.API); }
  getSummary(): Observable<any> { return this.http.get(`${this.API}/summary`); }
  toggle(productPublicId: string): Observable<any> { return this.http.post(`${this.API}/toggle/${productPublicId}`, {}); }
  add(productPublicId: string): Observable<any> { return this.http.post(`${this.API}/toggle/${productPublicId}`, {}); }
  remove(productPublicId: string): Observable<any> { return this.http.post(`${this.API}/toggle/${productPublicId}`, {}); }
  moveToCart(productPublicId: string, quantity = 1): Observable<any> {
    return this.http.post(`${this.API}/move-to-cart/${productPublicId}?quantity=${quantity}`, {});
  }
}
