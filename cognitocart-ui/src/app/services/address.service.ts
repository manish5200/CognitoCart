import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AddressService {
  private readonly API = 'https://cognitocart-api.onrender.com/api/v1/addresses';

  constructor(private http: HttpClient) {}

  getAll(): Observable<any> { return this.http.get(this.API); }

  add(data: {
    fullName: string; phone: string; addressLine1: string; addressLine2?: string;
    city: string; state: string; pincode: string; country?: string; isDefault?: boolean;
  }): Observable<any> { return this.http.post(this.API, data); }

  update(addressPublicId: string, data: any): Observable<any> {
    return this.http.put(`${this.API}/${addressPublicId}`, data);
  }

  delete(addressPublicId: string): Observable<any> {
    return this.http.delete(`${this.API}/${addressPublicId}`);
  }

  setDefault(addressPublicId: string): Observable<any> {
    return this.http.patch(`${this.API}/${addressPublicId}/default`, {});
  }
}
