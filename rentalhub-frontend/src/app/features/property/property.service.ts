// ─────────────────────────────────────────────────────────────────
// property.service.ts — API calls for the Property module
// ─────────────────────────────────────────────────────────────────
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PropertyRequest,
  PropertyResponse,
  PageResponse,
} from '../../core/models/property.models';

@Injectable({ providedIn: 'root' })
export class PropertyService {
  private readonly apiUrl = `${environment.apiUrl}/properties`;

  constructor(private http: HttpClient) {}

  list(
    page: number,
    size: number,
    keyword?: string,
  ): Observable<PageResponse<PropertyResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', 'createdAt,desc');

    if (keyword) {
      params = params.set('keyword', keyword);
    }

    return this.http.get<PageResponse<PropertyResponse>>(this.apiUrl, {
      params,
    });
  }

  getById(id: number): Observable<PropertyResponse> {
    return this.http.get<PropertyResponse>(`${this.apiUrl}/${id}`);
  }

  create(
    request: PropertyRequest,
    images: File[],
  ): Observable<PropertyResponse> {
    const formData = this.buildFormData(request, images);
    return this.http.post<PropertyResponse>(this.apiUrl, formData);
  }

  update(
    id: number,
    request: PropertyRequest,
    newImages: File[],
  ): Observable<PropertyResponse> {
    const formData = this.buildFormData(request, newImages);
    return this.http.put<PropertyResponse>(`${this.apiUrl}/${id}`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  deleteImage(propertyId: number, imageId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${propertyId}/images/${imageId}`,
    );
  }

  setPrimaryImage(propertyId: number, imageId: number): Observable<void> {
    return this.http.patch<void>(
      `${this.apiUrl}/${propertyId}/images/${imageId}/primary`,
      {},
    );
  }

  getImageUrl(filePath: string): string {
    const cleanPath = filePath.startsWith('uploads/')
      ? filePath.substring('uploads/'.length)
      : filePath;
    return `${environment.apiUrl}/files/${cleanPath}`;
  }

  private buildFormData(request: PropertyRequest, images: File[]): FormData {
    const formData = new FormData();

    const propertyBlob = new Blob([JSON.stringify(request)], {
      type: 'application/json',
    });
    formData.append('property', propertyBlob);

    images.forEach((file) => formData.append('images', file, file.name));

    return formData;
  }
}
