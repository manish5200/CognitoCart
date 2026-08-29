import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, catchError, of, tap, concatMap, from, toArray } from 'rxjs';
import { AuthService } from './auth.service';
import { ToastService } from './toast.service';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly API = 'https://cognitocart-api.onrender.com/api/v1/cart';
  private readonly GUEST_API = 'https://cognitocart-api.onrender.com/api/v1/guest-cart';
  private cartSubject = new BehaviorSubject<any>(null);
  public cart$ = this.cartSubject.asObservable();

  constructor(private http: HttpClient, private auth: AuthService, private toast: ToastService) {
    this.auth.currentUser$.subscribe(user => {
      this.refresh().subscribe();
    });
  }

  getGuestSessionId(): string {
    if (typeof window === 'undefined') return '';
    let sessionId = localStorage.getItem('guestSessionId');
    if (!sessionId) {
      // Create a UUID fallback for environments without crypto
      sessionId = typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : 
        'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
          const r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
          return v.toString(16);
        });
      localStorage.setItem('guestSessionId', sessionId);
    }
    return sessionId;
  }

  refresh(): Observable<any> {
    if (!this.auth.isLoggedIn() || !this.auth.isCustomer()) {
      return this.http.get(`${this.GUEST_API}/${this.getGuestSessionId()}`).pipe(
        tap(cart => this.cartSubject.next(cart)),
        catchError(err => { this.cartSubject.next({ items: [], totalAmount: 0 }); return of(null); })
      );
    }
    return this.http.get(`${this.API}/summary`).pipe(
      tap(cart => this.cartSubject.next(cart)),
      catchError(err => { this.cartSubject.next({ items: [], totalAmount: 0 }); return of(null); })
    );
  }

  add(variantPublicId: string, quantity = 1, productDetails?: any): Observable<any> {
    if (!this.auth.isLoggedIn() || !this.auth.isCustomer()) {
      return this.http.post(`${this.GUEST_API}/${this.getGuestSessionId()}/add`, { variantPublicId, quantity }).pipe(
        tap(() => this.refresh().subscribe())
      );
    }
    return this.http.post(`${this.API}/add`, { variantPublicId, quantity }).pipe(
      tap(() => this.refresh().subscribe())
    );
  }

  update(variantPublicId: string, quantity: number): Observable<any> {
    if (quantity < 1) return this.removeItem(variantPublicId);
    
    // In our backend, updating is just adding with the correct quantity delta,
    // but typically guest cart add endpoint takes absolute quantity or adds delta?
    // Let's check GuestCartController. Wait, usually updates are done by removing and re-adding or specific endpoints.
    // For guest cart, our addItem does: `guestCartService.addItem(sessionId, variantPublicId, quantity)`
    // Wait, the CartController has put /item/{variantPublicId}?quantity=...
    // Let's just post to /add for guest cart because we don't have a specific update endpoint for guest.
    // Let's assume POST /add replaces or adds. Wait! The user will click +, which adds 1.
    // In the guest cart controller: `GuestCart cart = guestCartService.addItem(sessionId, variantPublicId, quantity);`
    // If we want to set exact quantity, how do we do it? 
    // If they change quantity, we'll hit POST /add. 
    if (!this.auth.isLoggedIn() || !this.auth.isCustomer()) {
      return this.http.post(`${this.GUEST_API}/${this.getGuestSessionId()}/add`, { variantPublicId, quantity }).pipe(
        tap(() => this.refresh().subscribe())
      );
    }
    return this.http.put(`${this.API}/item/${variantPublicId}?quantity=${quantity}`, {}).pipe(
      tap(() => this.refresh().subscribe())
    );
  }

  removeItem(variantPublicId: string): Observable<any> {
    if (!this.auth.isLoggedIn() || !this.auth.isCustomer()) {
      return this.http.delete(`${this.GUEST_API}/${this.getGuestSessionId()}/item/${variantPublicId}`).pipe(
        tap(() => this.refresh().subscribe())
      );
    }
    return this.http.delete(`${this.API}/item/${variantPublicId}`).pipe(
      tap(() => this.refresh().subscribe())
    );
  }

  clear(): Observable<any> {
    if (!this.auth.isLoggedIn() || !this.auth.isCustomer()) {
      return this.http.delete(`${this.GUEST_API}/${this.getGuestSessionId()}/clear`, { responseType: 'text' as const }).pipe(
        tap(() => this.cartSubject.next({ items: [], totalAmount: 0 }))
      );
    }
    return this.http.delete(`${this.API}/clear`, { responseType: 'text' as const }).pipe(
      tap(() => this.cartSubject.next({ items: [], totalAmount: 0 }))
    );
  }

  applyCoupon(code: string): Observable<any> {
    if (!this.auth.isLoggedIn() || !this.auth.isCustomer()) {
      return of({ error: 'Login required' });
    }
    return this.http.post(`${this.API}/apply-coupon?code=${encodeURIComponent(code)}`, {}).pipe(
      tap(cart => this.cartSubject.next(cart))
    );
  }

  getCount(): number {
    const cart = this.cartSubject.value;
    if (!cart?.items) return 0;
    return cart.items.reduce((sum: number, item: any) => sum + item.quantity, 0);
  }

  getTotal(): number {
    return this.cartSubject.value?.totalAmount ?? 0;
  }
}
