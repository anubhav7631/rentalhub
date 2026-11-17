import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AgreementRequest,
  AgreementResponse,
  PageResponse,
} from '../../core/models/agreement.models';

@Injectable({ providedIn: 'root' })
export class AgreementService {
  private readonly apiUrl = `${environment.apiUrl}/agreements`;

  constructor(private http: HttpClient) {}

  /**
   * upload — sends multipart/form-data with JSON metadata + PDF file.
   * Same pattern as PropertyService.create() from Phase 9.
   */
  upload(request: AgreementRequest, file: File): Observable<AgreementResponse> {
    const formData = new FormData();

    const metadataBlob = new Blob([JSON.stringify(request)], {
      type: 'application/json',
    });
    formData.append('metadata', metadataBlob);
    formData.append('file', file, file.name);

    return this.http.post<AgreementResponse>(this.apiUrl, formData);
  }

  list(
    page: number,
    size: number,
  ): Observable<PageResponse<AgreementResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<AgreementResponse>>(this.apiUrl, {
      params,
    });
  }

  getById(id: number): Observable<AgreementResponse> {
    return this.http.get<AgreementResponse>(`${this.apiUrl}/${id}`);
  }

  getByProperty(propertyId: number): Observable<AgreementResponse[]> {
    return this.http.get<AgreementResponse[]>(
      `${this.apiUrl}/property/${propertyId}`,
    );
  }

  getActiveByProperty(propertyId: number): Observable<AgreementResponse> {
    return this.http.get<AgreementResponse>(
      `${this.apiUrl}/property/${propertyId}/active`,
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  download(id: number, filename: string): void {
    this.http
      .get(`${this.apiUrl}/${id}/download`, {
        responseType: 'blob',
        observe: 'response', // get full response including headers
      })
      .subscribe({
        next: (response) => {
          const blob = response.body as Blob;

          // Create a temporary object URL for the blob
          const url = URL.createObjectURL(blob);

          // Create a hidden <a> element, click it, then remove it
          const anchor = document.createElement('a');
          anchor.href = url;
          anchor.download = filename;
          document.body.appendChild(anchor);
          anchor.click();

          // Clean up
          document.body.removeChild(anchor);
          URL.revokeObjectURL(url);
        },
        error: (err) => {
          console.error('Download failed', err);
        },
      });
  }
}
