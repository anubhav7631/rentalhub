import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { AppRole } from './core/models/user.models';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(
        (m) => m.LoginComponent,
      ),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(
        (m) => m.RegisterComponent,
      ),
  },

  // Phase 18 — password reset routes
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password.component').then(
        (m) => m.ForgotPasswordComponent,
      ),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password.component').then(
        (m) => m.ResetPasswordComponent,
      ),
  },

  {
    path: '',
    loadComponent: () =>
      import('./layout/main-layout/main-layout.component').then(
        (m) => m.MainLayoutComponent,
      ),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(
            (m) => m.DashboardComponent,
          ),
      },

      {
        path: 'properties',
        canActivate: [roleGuard],
        data: { roles: [AppRole.ADMIN, AppRole.OWNER] },
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/property/property-list/property-list.component').then(
                (m) => m.PropertyListComponent,
              ),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./features/property/property-form/property-form.component').then(
                (m) => m.PropertyFormComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/property/property-detail/property-detail.component').then(
                (m) => m.PropertyDetailComponent,
              ),
          },
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./features/property/property-form/property-form.component').then(
                (m) => m.PropertyFormComponent,
              ),
          },
        ],
      },

      /*
       * ── Tenant routes ─────────────────────────────────────────
       * Same ordering rule as properties: 'new' before ':id'.
       */
      {
        path: 'tenants',
        canActivate: [roleGuard],
        data: { roles: [AppRole.ADMIN, AppRole.OWNER] },
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/tenant/tenant-list/tenant-list.component').then(
                (m) => m.TenantListComponent,
              ),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./features/tenant/tenant-form/tenant-form.component').then(
                (m) => m.TenantFormComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/tenant/tenant-detail/tenant-detail.component').then(
                (m) => m.TenantDetailComponent,
              ),
          },
        ],
      },
      // Add inside the children array, after tenant routes:
      {
        path: 'agreements',
        canActivate: [roleGuard],
        data: { roles: [AppRole.ADMIN, AppRole.OWNER] },
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/agreement/agreement-list/agreement-list.component').then(
                (m) => m.AgreementListComponent,
              ),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./features/agreement/agreement-upload/agreement-upload.component').then(
                (m) => m.AgreementUploadComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/agreement/agreement-detail/agreement-detail.component').then(
                (m) => m.AgreementDetailComponent,
              ),
          },
        ],
      },
      // Add inside children[], after agreements:
      {
        path: 'payments',
        canActivate: [roleGuard],
        data: { roles: [AppRole.ADMIN, AppRole.OWNER, AppRole.TENANT] },
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/payment/payment-list/payment-list.component').then(
                (m) => m.PaymentListComponent,
              ),
          },
        ],
      },
      // Add inside children[], after payments:
      {
        path: 'maintenance',
        canActivate: [authGuard],
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/maintenance/maintenance-list/maintenance-list.component').then(
                (m) => m.MaintenanceListComponent,
              ),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./features/maintenance/maintenance-form/maintenance-form.component').then(
                (m) => m.MaintenanceFormComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/maintenance/maintenance-detail/maintenance-detail.component').then(
                (m) => m.MaintenanceDetailComponent,
              ),
          },
        ],
      },
      // Add inside children[], after maintenance:
      {
        path: 'reports',
        canActivate: [roleGuard],
        data: { roles: [AppRole.ADMIN, AppRole.OWNER] },
        loadComponent: () =>
          import('./features/reports/reports.component').then(
            (m) => m.ReportsComponent,
          ),
      },
      {
        path: 'settings',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/settings/settings.component').then(
            (m) => m.SettingsComponent,
          ),
      },
      { path: 'profile', redirectTo: 'settings', pathMatch: 'full' },
      // Add inside children[]:
      {
        path: 'notifications',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/notifications/notification-list.component').then(
            (m) => m.NotificationListComponent,
          ),
      },
      {
        path: 'admin/users',
        loadComponent: () =>
          import('./features/admin/user-management/user-management.component').then(
            (m) => m.UserManagementComponent,
          ),
        canActivate: [roleGuard],
        data: { roles: [AppRole.ADMIN] },
      },
    ],
  },

  {
    path: '**',
    loadComponent: () =>
      import('./features/not-found/not-found.component').then(
        (m) => m.NotFoundComponent,
      ),
  },
];
