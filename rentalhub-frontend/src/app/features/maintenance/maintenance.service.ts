import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  MaintenanceCreateRequest,
  MaintenanceResponse,
  MaintenanceStatusUpdate,
  PageResponse,
} from '../../core/models/maintenance.models';

@Injectable({ providedIn: 'root' })
export class MaintenanceService {
  private readonly apiUrl = `${environment.apiUrl}/maintenance`;

  constructor(private http: HttpClient) {}

  create(
    request: MaintenanceCreateRequest,
    images: File[],
  ): Observable<MaintenanceResponse> {
    const formData = new FormData();
    const blob = new Blob([JSON.stringify(request)], {
      type: 'application/json',
    });
    formData.append('metadata', blob);
    images.forEach((f) => formData.append('images', f, f.name));
    return this.http.post<MaintenanceResponse>(this.apiUrl, formData);
  }

  list(
    page: number,
    size: number,
    status?: string,
  ): Observable<PageResponse<MaintenanceResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (status) params = params.set('status', status);
    return this.http.get<PageResponse<MaintenanceResponse>>(this.apiUrl, {
      params,
    });
  }

  getById(id: number): Observable<MaintenanceResponse> {
    return this.http.get<MaintenanceResponse>(`${this.apiUrl}/${id}`);
  }

  updateStatus(
    id: number,
    update: MaintenanceStatusUpdate,
  ): Observable<MaintenanceResponse> {
    return this.http.patch<MaintenanceResponse>(
      `${this.apiUrl}/${id}/status`,
      update,
    );
  }

  getImageUrl(filePath: string): string {
    const clean = filePath.startsWith('uploads/')
      ? filePath.substring('uploads/'.length)
      : filePath;
    return `${environment.apiUrl}/files/${clean}`;
  }
}
