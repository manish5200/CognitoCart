import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly API = environment.apiUrl + '/customers';

  constructor(private http: HttpClient) {}

  getDashboard(pageNumber = 0, pageSize = 5): Observable<any> {
    return this.http.get(`${this.API}/dashboard?pageNumber=${pageNumber}&pageSize=${pageSize}`);
  }
}

