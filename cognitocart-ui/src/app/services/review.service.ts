import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly API = environment.apiUrl + '/reviews';

  constructor(private http: HttpClient) {}

  getReviews(productPublicId: string): Observable<any> {
    return this.http.get(`${this.API}/${productPublicId}`);
  }

  getRatingDistribution(productPublicId: string): Observable<any> {
    return this.http.get(`${this.API}/${productPublicId}/distribution`);
  }

  postReview(productPublicId: string, data: { rating: number; comment: string; title?: string }, images?: File[]): Observable<any> {
    const form = new FormData();
    form.append('review', JSON.stringify(data));
    if (images?.length) images.forEach(img => form.append('images', img));
    return this.http.post(`${this.API}/${productPublicId}`, form);
  }

  deleteMyReview(reviewPublicId: string): Observable<any> {
    return this.http.delete(`${this.API}/${reviewPublicId}`);
  }

  adminDeleteReview(reviewPublicId: string): Observable<any> {
    return this.http.delete(`${this.API}/admin/${reviewPublicId}`);
  }
}

