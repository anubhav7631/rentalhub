import {
  Component,
  OnInit,
  OnDestroy,
  AfterViewInit,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Subject, takeUntil } from 'rxjs';
import {
  Chart,
  BarController,
  BarElement,
  LineController,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  DoughnutController,
  ArcElement,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';

import { DashboardService } from './dashboard.service';
import { AuthService } from '../../core/services/auth.service';
import { HasRoleDirective } from '../../core/directives/has-role.directive';
import { AppRole } from '../../core/models/user.models';
import {
  AdminDashboard,
  OwnerDashboard,
  TenantDashboard,
} from '../../core/models/dashboard.models';

/*
 * Register only the Chart.js components we actually use.
 * Chart.js v4 uses tree-shaking — importing Chart alone does nothing.
 * You must register each controller, element, scale, and plugin.
 * This keeps the bundle smaller than importing everything.
 */
Chart.register(
  BarController,
  BarElement,
  LineController,
  LineElement,
  PointElement,
  DoughnutController,
  ArcElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend,
  Filler,
);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    HasRoleDirective,
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly AppRole = AppRole;

  // Role detection
  isAdmin = false;
  isOwner = false;
  isTenant = false;
  userName = '';

  // Data
  adminData: AdminDashboard | null = null;
  ownerData: OwnerDashboard | null = null;
  tenantData: TenantDashboard | null = null;

  isLoading = true;
  loadError = false;

  // Chart references
  @ViewChild('revenueChart') revenueChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('occupancyChart')
  occupancyChartRef!: ElementRef<HTMLCanvasElement>;

  private revenueChart: Chart | null = null;
  private occupancyChart: Chart | null = null;

  private destroy$ = new Subject<void>();

  constructor(
    private dashboardService: DashboardService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    this.userName = user?.fullName ?? '';
    this.isAdmin = this.authService.hasRole(AppRole.ADMIN);
    this.isOwner = this.authService.hasRole(AppRole.OWNER);
    this.isTenant = this.authService.hasRole(AppRole.TENANT);

    this.loadDashboard();
  }

  ngAfterViewInit(): void {
    // Charts are built after data loads — see buildCharts()
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    // Destroy Chart.js instances to free canvas memory
    this.revenueChart?.destroy();
    this.occupancyChart?.destroy();
  }

  loadDashboard(): void {
    this.isLoading = true;
    this.loadError = false;

    if (this.isAdmin) {
      this.dashboardService
        .getAdminDashboard()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (d) => {
            this.adminData = d;
            this.isLoading = false;
            this.buildAdminCharts();
          },
          error: () => {
            this.isLoading = false;
            this.loadError = true;
          },
        });
    } else if (this.isOwner) {
      this.dashboardService
        .getOwnerDashboard()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (d) => {
            this.ownerData = d;
            this.isLoading = false;
            this.buildOwnerCharts();
          },
          error: () => {
            this.isLoading = false;
            this.loadError = true;
          },
        });
    } else if (this.isTenant) {
      this.dashboardService
        .getTenantDashboard()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (d) => {
            this.tenantData = d;
            this.isLoading = false;
          },
          error: () => {
            this.isLoading = false;
            this.loadError = true;
          },
        });
    }
  }

  // ── Chart builders ────────────────────────────────────────────

  private buildAdminCharts(): void {
    // Give Angular one tick to render the canvas elements
    setTimeout(() => {
      if (this.revenueChartRef && this.adminData) {
        this.revenueChart?.destroy();
        this.revenueChart = new Chart(this.revenueChartRef.nativeElement, {
          type: 'bar',
          data: {
            labels: this.adminData.monthlyRevenue.map((p) => p.month),
            datasets: [
              {
                label: 'Revenue (₹)',
                data: this.adminData.monthlyRevenue.map((p) => p.amount),
                backgroundColor: 'rgba(55, 48, 163, 0.75)',
                borderColor: '#3730a3',
                borderWidth: 1,
                borderRadius: 4,
              },
            ],
          },
          options: this.barChartOptions('Monthly Platform Revenue'),
        });
      }
    });
  }

  private buildOwnerCharts(): void {
    setTimeout(() => {
      if (this.revenueChartRef && this.ownerData) {
        this.revenueChart?.destroy();
        this.revenueChart = new Chart(this.revenueChartRef.nativeElement, {
          type: 'bar',
          data: {
            labels: this.ownerData.monthlyRevenue.map((p) => p.month),
            datasets: [
              {
                label: 'Revenue Collected (₹)',
                data: this.ownerData.monthlyRevenue.map((p) => p.amount),
                backgroundColor: 'rgba(55, 48, 163, 0.75)',
                borderColor: '#3730a3',
                borderWidth: 1,
                borderRadius: 4,
              },
            ],
          },
          options: this.barChartOptions('Monthly Revenue'),
        });
      }

      if (this.occupancyChartRef && this.ownerData) {
        this.occupancyChart?.destroy();
        this.occupancyChart = new Chart(this.occupancyChartRef.nativeElement, {
          type: 'doughnut',
          data: {
            labels: ['Occupied', 'Vacant'],
            datasets: [
              {
                data: [
                  this.ownerData.occupiedProperties,
                  this.ownerData.vacantProperties,
                ],
                backgroundColor: ['#4f46e5', '#e5e7eb'],
                borderWidth: 0,
              },
            ],
          },
          options: {
            responsive: true,
            cutout: '72%',
            plugins: {
              legend: { position: 'bottom' },
              tooltip: {
                callbacks: {
                  label: (ctx) => ` ${ctx.label}: ${ctx.raw} properties`,
                },
              },
            },
          },
        });
      }
    });
  }

  private barChartOptions(title: string): any {
    return {
      responsive: true,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx: any) => ` ₹${Number(ctx.raw).toLocaleString('en-IN')}`,
          },
        },
      },
      scales: {
        x: { grid: { display: false } },
        y: {
          grid: { color: '#f3f4f6' },
          ticks: {
            callback: (v: any) =>
              '₹' +
              (Number(v) >= 1000 ? (Number(v) / 1000).toFixed(0) + 'K' : v),
          },
        },
      },
    };
  }

  // ── Helpers ───────────────────────────────────────────────────

  formatCurrency(value: number): string {
    if (value >= 100000) return '₹' + (value / 100000).toFixed(1) + 'L';
    if (value >= 1000) return '₹' + (value / 1000).toFixed(0) + 'K';
    return '₹' + value;
  }

  getPaymentStatusClass(status: string): string {
    switch (status) {
      case 'PAID':
        return 'status-badge--success';
      case 'PENDING':
        return 'status-badge--info';
      case 'OVERDUE':
        return 'status-badge--danger';
      case 'NO_RECORD':
        return 'status-badge--warning';
      default:
        return '';
    }
  }

  getAgreementStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'status-badge--success';
      case 'EXPIRING_SOON':
        return 'status-badge--warning';
      case 'EXPIRED':
        return 'status-badge--danger';
      default:
        return '';
    }
  }
}
