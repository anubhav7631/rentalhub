import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AuthResponse,
  CurrentUser,
  LoginRequest,
  RegisterRequest,
} from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  // BehaviorSubject holds the current user state.
  // It emits immediately to new subscribers (unlike a plain Subject).
  // null = not logged in, CurrentUser = logged in.
  private currentUserSubject = new BehaviorSubject<CurrentUser | null>(
    this.loadUserFromStorage(),
  );

  // Public observable — components subscribe to this
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  // ── API CALLS ──────────────────────────────────────────────────

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, request)
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/register`, request)
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  // Called by AuthInterceptor when access token expires
  refreshToken(refreshToken: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/refresh`, { refreshToken })
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  logout(): void {
    // Fire-and-forget call to backend to revoke refresh token
    const token = this.getToken();
    if (token) {
      this.http
        .post(`${this.apiUrl}/logout`, {})
        .subscribe({ error: () => {} });
    }
    this.clearSession();
  }

  // ── TOKEN HELPERS ──────────────────────────────────────────────

  getToken(): string | null {
    return localStorage.getItem(environment.jwtTokenKey);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(environment.jwtRefreshKey);
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) return false;
    // Check token expiry client-side by reading the exp claim from payload
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return Date.now() < payload.exp * 1000;
    } catch {
      return false;
    }
  }

  getCurrentUser(): CurrentUser | null {
    return this.currentUserSubject.getValue();
  }

  hasRole(role: string): boolean {
    return this.getCurrentUser()?.roles?.includes(role) ?? false;
  }

  hasAnyRole(roles: string[]): boolean {
    return roles.some((r) => this.hasRole(r));
  }

  // ── PRIVATE ────────────────────────────────────────────────────

  // Called after every successful login / register / refresh
  private handleAuthResponse(res: AuthResponse): void {
    if (res.token) localStorage.setItem(environment.jwtTokenKey, res.token);
    if (res.refreshToken)
      localStorage.setItem(environment.jwtRefreshKey, res.refreshToken);

    const user: CurrentUser = {
      userId: res.userId,
      fullName: res.fullName,
      email: res.email,
      roles: res.roles,
      token: res.token,
    };
    localStorage.setItem('rentalhub_user', JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  private clearSession(): void {
    localStorage.removeItem(environment.jwtTokenKey);
    localStorage.removeItem(environment.jwtRefreshKey);
    localStorage.removeItem('rentalhub_user');
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  private loadUserFromStorage(): CurrentUser | null {
    try {
      const raw = localStorage.getItem('rentalhub_user');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
