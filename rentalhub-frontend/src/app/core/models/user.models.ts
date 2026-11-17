// ─────────────────────────────────────────────────────────────────
// user.models.ts — Role constants and user-related types
// ─────────────────────────────────────────────────────────────────

/**
 * AppRole — matches the RoleName enum in the backend exactly.
 * Using a const object (not a TypeScript enum) gives us string
 * literal types that work cleanly with JSON from the API.
 */
export const AppRole = {
  ADMIN: 'ROLE_ADMIN',
  OWNER: 'ROLE_OWNER',
  TENANT: 'ROLE_TENANT',
} as const;

// Derives a union type: 'ROLE_ADMIN' | 'ROLE_OWNER' | 'ROLE_TENANT'
export type AppRoleType = (typeof AppRole)[keyof typeof AppRole];

export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  profileImagePath: string | null;
  isActive: boolean;
  isEmailVerified: boolean;
  roles: string[];
  createdAt: string;
}

// Matches Spring's Page<T> JSON structure exactly
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page (0-indexed)
  size: number;
  first: boolean;
  last: boolean;
}
