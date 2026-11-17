import { ApplicationConfig } from '@angular/core';
import {
  provideRouter,
  withInMemoryScrolling,
  withComponentInputBinding,
} from '@angular/router';

import {
  provideHttpClient,
  withInterceptors,
  withFetch,
} from '@angular/common/http';

import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MAT_DATE_LOCALE } from '@angular/material/core';
import { MAT_SNACK_BAR_DEFAULT_OPTIONS } from '@angular/material/snack-bar';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    // Router
    provideRouter(
      routes,
      withInMemoryScrolling({
        scrollPositionRestoration: 'top',
        anchorScrolling: 'enabled',
      }),
      withComponentInputBinding(),
    ),

    // HttpClient
    provideHttpClient(withInterceptors([authInterceptor]), withFetch()),

    // Animations
    provideAnimationsAsync(),

    // Material Date Locale
    {
      provide: MAT_DATE_LOCALE,
      useValue: 'en-IN',
    },

    // Global SnackBar Configuration
    {
      provide: MAT_SNACK_BAR_DEFAULT_OPTIONS,
      useValue: {
        duration: 4000,
        horizontalPosition: 'right',
        verticalPosition: 'top',
      },
    },
  ],
};
