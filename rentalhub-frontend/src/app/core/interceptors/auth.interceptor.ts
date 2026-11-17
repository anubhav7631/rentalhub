import {
  HttpRequest,
  HttpHandlerFn,
  HttpEvent,
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

let isRefreshing = false;
const refreshSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);
  const notifications = inject(NotificationService);

  const token = authService.getToken();
  const authReq = token ? addToken(req, token) : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        return handle401(authReq, next, authService);
      }

      /*
       * For all other errors, extract the message from the
       * standardized ErrorResponse body (Phase 17).
       *
       * err.error is the parsed JSON body:
       * { status, error, message, timestamp, path, errors? }
       *
       * We re-throw so components can still handle specific errors
       * themselves if needed. The message is already on err.error.message.
       */
      if (err.status === 403) {
        notifications.error(
          err.error?.message ||
            'You do not have permission to perform this action',
        );
      }

      if (err.status === 429) {
        notifications.warning(
          'Too many requests. Please wait a minute before trying again.',
        );
      }

      if (err.status === 0) {
        notifications.error(
          'Cannot connect to the server. Please check your connection.',
        );
      }

      return throwError(() => err);
    }),
  );
};

function addToken(
  req: HttpRequest<unknown>,
  token: string,
): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService,
): Observable<HttpEvent<unknown>> {
  if (req.url.includes('/auth/refresh') || req.url.includes('/auth/login')) {
    authService.logout();
    return throwError(() => new Error('Authentication failed'));
  }

  if (!isRefreshing) {
    isRefreshing = true;
    refreshSubject.next(null);

    const refreshToken = authService.getRefreshToken();
    if (!refreshToken) {
      isRefreshing = false;
      authService.logout();
      return throwError(() => new Error('No refresh token'));
    }

    return authService.refreshToken(refreshToken).pipe(
      switchMap((res) => {
        isRefreshing = false;
        refreshSubject.next(res.token);
        return next(addToken(req, res.token));
      }),
      catchError((err) => {
        isRefreshing = false;
        authService.logout();
        return throwError(() => err);
      }),
    );
  }

  return refreshSubject.pipe(
    filter((t) => t !== null),
    take(1),
    switchMap((t) => next(addToken(req, t!))),
  );
}
