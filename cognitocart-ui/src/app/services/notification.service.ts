import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly API = 'https://cognitocart-api.onrender.com/api/v1/notifications';
  
  public notificationsUpdated$ = new Subject<void>();

  constructor(private http: HttpClient) {}

  getNotifications(page = 0, size = 10): Observable<any> {
    return this.http.get(`${this.API}?page=${page}&size=${size}`);
  }

  markAsRead(publicId: string): Observable<any> {
    return this.http.patch(`${this.API}/${publicId}/read`, {}, { responseType: 'text' })
      .pipe(tap(() => this.notificationsUpdated$.next()));
  }

  markAllAsRead(): Observable<any> {
    return this.http.patch(`${this.API}/read-all`, {}, { responseType: 'text' })
      .pipe(tap(() => this.notificationsUpdated$.next()));
  }
}
