// ─────────────────────────────────────────────────────────────────
// auth.models.ts — TypeScript interfaces matching Java DTOs exactly
// ─────────────────────────────────────────────────────────────────

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  phone: string;
  role: string; // 'ROLE_OWNER' | 'ROLE_TENANT'
}

export interface AuthResponse {
  token: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
  fullName: string;
  email: string;
  roles: string[];
  message: string;
}

// Stored in localStorage — represents the logged-in session
export interface CurrentUser {
  userId: number;
  fullName: string;
  email: string;
  roles: string[];
  token: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}
