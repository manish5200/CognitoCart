import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';

export interface UserProfile {
  id: number;
  name: string;
  email: string;
  role: 'ROLE_CUSTOMER' | 'ROLE_SELLER' | 'ROLE_ADMIN';
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = 'https://cognitocart-api.onrender.com/api/v1/auth';
  private currentUserSubject = new BehaviorSubject<UserProfile | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('cognitocart_user');
      if (saved) {
        const user = JSON.parse(saved);
        if (user.role && !user.role.startsWith('ROLE_')) {
          user.role = `ROLE_${user.role}`;
          localStorage.setItem('cognitocart_user', JSON.stringify(user));
        }
        this.currentUserSubject.next(user);
      }
    }
  }

  login(credentials: { email: string; password: string }): Observable<any> {
    const guestSessionId = typeof window !== 'undefined' ? localStorage.getItem('guestSessionId') : null;
    const payload = { ...credentials, guestSessionId };
    return this.http.post<any>(`${this.API}/login`, payload).pipe(
      tap(res => {
        const user: UserProfile = {
          id: res.userId,
          name: res.fullName,
          email: res.email,
          role: (res.role?.startsWith('ROLE_') ? res.role : `ROLE_${res.role}`) as any
        };
        this.saveSession(res.accessToken, res.refreshToken, user);
        if (typeof window !== 'undefined') {
          localStorage.removeItem('guestSessionId');
        }
      })
    );
  }

  registerCustomer(data: any): Observable<any> {
    const guestSessionId = typeof window !== 'undefined' ? localStorage.getItem('guestSessionId') : null;
    return this.http.post(`${this.API}/register/customer`, { ...data, guestSessionId }).pipe(
      tap(() => {
        if (typeof window !== 'undefined') localStorage.removeItem('guestSessionId');
      })
    );
  }

  registerSeller(data: any): Observable<any> {
    const guestSessionId = typeof window !== 'undefined' ? localStorage.getItem('guestSessionId') : null;
    return this.http.post(`${this.API}/register/seller`, { ...data, guestSessionId }).pipe(
      tap(() => {
        if (typeof window !== 'undefined') localStorage.removeItem('guestSessionId');
      })
    );
  }

  logout(): Observable<any> {
    const token = this.getToken();
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
    return this.http.post(`${this.API}/logout`, {}, { headers, responseType: 'text' }).pipe(
      tap(() => this.clearSession())
    );
  }

  forgotPassword(email: string): Observable<any> {
    return this.http.post(`${this.API}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.API}/reset-password`, { token, newPassword });
  }

  verifyEmail(email: string, otp: string): Observable<any> {
    return this.http.post(`${this.API}/verify-email`, { email, otp });
  }

  resendOtp(email: string): Observable<any> {
    return this.http.post(`${this.API}/resend-otp`, null, { params: { email } });
  }

  refreshToken(): Observable<any> {
    const refreshToken = localStorage.getItem('cognitocart_refresh');
    if (!refreshToken) {
      this.clearSession();
      return new Observable(obs => obs.error('No refresh token available'));
    }
    return this.http.post<any>(`${this.API}/refresh`, { refreshToken }).pipe(
      tap(res => {
        if (typeof window !== 'undefined') {
          localStorage.setItem('cognitocart_token', res.accessToken);
          if (res.refreshToken) localStorage.setItem('cognitocart_refresh', res.refreshToken);
        }
      })
    );
  }

  private saveSession(accessToken: string, refreshToken: string, user: UserProfile): void {
    if (typeof window !== 'undefined') {
      localStorage.setItem('cognitocart_token', accessToken);
      localStorage.setItem('cognitocart_refresh', refreshToken);
      localStorage.setItem('cognitocart_user', JSON.stringify(user));
    }
    this.currentUserSubject.next(user);
  }

  saveOAuth2Session(accessToken: string, refreshToken: string, id: number, email: string, name: string, role: string): void {
    const user: UserProfile = {
      id: id,
      name: name,
      email: email,
      role: (role?.startsWith('ROLE_') ? role : `ROLE_${role}`) as any
    };
    this.saveSession(accessToken, refreshToken, user);
  }

  private clearSession(): void {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('cognitocart_token');
      localStorage.removeItem('cognitocart_refresh');
      localStorage.removeItem('cognitocart_user');
    }
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    if (typeof window !== 'undefined') return localStorage.getItem('cognitocart_token');
    return null;
  }

  isLoggedIn(): boolean { return !!this.getToken(); }

  getCurrentUser(): UserProfile | null { return this.currentUserSubject.value; }

  isAdmin(): boolean { return this.getCurrentUser()?.role === 'ROLE_ADMIN'; }
  isSeller(): boolean { return this.getCurrentUser()?.role === 'ROLE_SELLER'; }
  isCustomer(): boolean { return this.getCurrentUser()?.role === 'ROLE_CUSTOMER'; }

  logoutAndRedirect(): void {
    const token = this.getToken();
    if (token) {
      const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
      this.http.post(`${this.API}/logout`, {}, { headers, responseType: 'text' }).subscribe({
        complete: () => { this.clearSession(); this.router.navigate(['/login']); },
        error: () => { this.clearSession(); this.router.navigate(['/login']); }
      });
    } else {
      this.clearSession();
      this.router.navigate(['/login']);
    }
  }
}
