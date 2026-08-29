import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly API = 'https://cognitocart-api.onrender.com/api/v1/customers';

  constructor(private http: HttpClient) {}

  getDashboard(pageNumber = 0, pageSize = 5): Observable<any> {
    return this.http.get(`${this.API}/dashboard?pageNumber=${pageNumber}&pageSize=${pageSize}`);
  }
}
