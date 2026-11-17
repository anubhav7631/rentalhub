// ─────────────────────────────────────────────────────────────────
// auth.guard.ts — FUNCTIONAL ROUTE GUARD (Angular 17+)
// ─────────────────────────────────────────────────────────────────
// Protects all routes inside MainLayoutComponent.
// Not logged in → redirect to /login?returnUrl=<attempted-url>
// Logged in     → allow navigation
// ─────────────────────────────────────────────────────────────────
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn()) return true;

  // Preserve the attempted URL so the user lands there after login
  return router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url },
  });
};
