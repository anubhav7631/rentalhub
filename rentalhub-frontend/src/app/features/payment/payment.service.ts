import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PaymentResponse,
  PaymentSummary,
  RecordPaymentRequest,
  PageResponse,
} from '../../core/models/payment.models';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly apiUrl = `${environment.apiUrl}/payments`;

  constructor(private http: HttpClient) {}

  list(
    page: number,
    size: number,
    propertyId?: number,
    tenantId?: number,
  ): Observable<PageResponse<PaymentResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (propertyId) params = params.set('propertyId', propertyId.toString());
    if (tenantId) params = params.set('tenantId', tenantId.toString());

    return this.http.get<PageResponse<PaymentResponse>>(this.apiUrl, {
      params,
    });
  }

  getById(id: number): Observable<PaymentResponse> {
    return this.http.get<PaymentResponse>(`${this.apiUrl}/${id}`);
  }

  getSummary(): Observable<PaymentSummary> {
    return this.http.get<PaymentSummary>(`${this.apiUrl}/summary`);
  }

  markPaid(
    id: number,
    request: RecordPaymentRequest,
  ): Observable<PaymentResponse> {
    return this.http.patch<PaymentResponse>(
      `${this.apiUrl}/${id}/paid`,
      request,
    );
  }

  waive(id: number, reason?: string): Observable<PaymentResponse> {
    return this.http.patch<PaymentResponse>(
      `${this.apiUrl}/${id}/waive`,
      reason ? { reason } : {},
    );
  }

  generateForMonth(month: number, year: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/generate`, { month, year });
  }
}
