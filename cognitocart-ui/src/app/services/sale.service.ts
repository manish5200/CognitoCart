import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class SaleService {
  private readonly PUBLIC_API = environment.apiUrl + '/public/sales';

  constructor(private http: HttpClient) {}

  getLiveSales(): Observable<any> { return this.http.get(`${this.PUBLIC_API}/live`); }
  getUpcomingSales(): Observable<any> { return this.http.get(`${this.PUBLIC_API}/upcoming`); }
}

