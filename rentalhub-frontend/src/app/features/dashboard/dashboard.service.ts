import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminDashboard,
  OwnerDashboard,
  TenantDashboard,
} from '../../core/models/dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly apiUrl = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  getAdminDashboard(): Observable<AdminDashboard> {
    return this.http.get<AdminDashboard>(`${this.apiUrl}/admin`);
  }

  getOwnerDashboard(): Observable<OwnerDashboard> {
    return this.http.get<OwnerDashboard>(`${this.apiUrl}/owner`);
  }

  getTenantDashboard(): Observable<TenantDashboard> {
    return this.http.get<TenantDashboard>(`${this.apiUrl}/tenant`);
  }
}
