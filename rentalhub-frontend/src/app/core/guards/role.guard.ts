// ─────────────────────────────────────────────────────────────────
// role.guard.ts — ROLE-BASED FUNCTIONAL GUARD (Angular 17+)
// ─────────────────────────────────────────────────────────────────
// Usage in routes:
//   {
//     path: 'users',
//     canActivate: [authGuard, roleGuard],
//     data: { roles: ['ROLE_ADMIN'] },
//     loadComponent: () => import(...)
//   }
// ─────────────────────────────────────────────────────────────────
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, _state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const required: string[] = route.data['roles'] ?? [];
  if (required.length === 0) return true;
  if (auth.hasAnyRole(required)) return true;

  // Authenticated but wrong role — send to dashboard
  router.navigate(['/dashboard']);
  return false;
};
