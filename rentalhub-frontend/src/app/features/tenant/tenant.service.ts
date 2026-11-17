import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  TenantRequest,
  TenantResponse,
  MoveOutRequest,
  PageResponse,
} from '../../core/models/tenant.models';

@Injectable({ providedIn: 'root' })
export class TenantService {
  private readonly apiUrl = `${environment.apiUrl}/tenants`;

  constructor(private http: HttpClient) {}

  assignTenant(request: TenantRequest): Observable<TenantResponse> {
    return this.http.post<TenantResponse>(this.apiUrl, request);
  }

  list(page: number, size: number): Observable<PageResponse<TenantResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<TenantResponse>>(this.apiUrl, { params });
  }

  getById(id: number): Observable<TenantResponse> {
    return this.http.get<TenantResponse>(`${this.apiUrl}/${id}`);
  }

  updateProfile(
    id: number,
    request: TenantRequest,
  ): Observable<TenantResponse> {
    return this.http.put<TenantResponse>(`${this.apiUrl}/${id}`, request);
  }

  moveOut(id: number, request: MoveOutRequest): Observable<TenantResponse> {
    return this.http.post<TenantResponse>(
      `${this.apiUrl}/${id}/move-out`,
      request,
    );
  }

  getHistoryByProperty(propertyId: number): Observable<TenantResponse[]> {
    return this.http.get<TenantResponse[]>(
      `${this.apiUrl}/property/${propertyId}/history`,
    );
  }

  getCurrentTenant(propertyId: number): Observable<TenantResponse | null> {
    return this.http.get<TenantResponse | null>(
      `${this.apiUrl}/property/${propertyId}/current`,
    );
  }
}
