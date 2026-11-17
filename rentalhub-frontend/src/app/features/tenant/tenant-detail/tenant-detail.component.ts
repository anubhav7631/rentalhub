import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TenantService } from '../tenant.service';
import { NotificationService } from '../../../core/services/notification.service';
import { TenantResponse } from '../../../core/models/tenant.models';
import { MoveOutDialogComponent } from '../move-out-dialog/move-out-dialog.component';

@Component({
  selector: 'app-tenant-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDialogModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './tenant-detail.component.html',
  styleUrls: ['./tenant-detail.component.scss'],
})
export class TenantDetailComponent implements OnInit {
  tenant: TenantResponse | null = null;
  isLoading = true;
  isMovingOut = false;
  loadError = false;

  constructor(
    private tenantService: TenantService,
    private notification: NotificationService,
    private dialog: MatDialog,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      this.router.navigate(['/tenants']);
      return;
    }
    this.loadTenant(+idParam);
  }

  loadTenant(id: number): void {
    this.isLoading = true;
    this.loadError = false;

    this.tenantService.getById(id).subscribe({
      next: (tenant) => {
        this.tenant = tenant;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.loadError = true;

        if (err.status === 403) {
          this.notification.error(
            'Access denied — you can only view tenants on your own properties.',
          );
        } else if (err.status === 404) {
          this.notification.error('Tenant not found.');
        } else {
          this.notification.error('Failed to load tenant details.');
        }
      },
    });
  }

  /**
   * openMoveOutDialog — opens the date-picker dialog.
   *
   * On confirm, calls moveOut() which:
   *   1. Sets tenant.moveOutDate on the backend
   *   2. Flips property.status → AVAILABLE
   *   3. Updates the local `tenant` object so the template
   *      immediately shows "Moved Out" badge WITHOUT a page refresh
   */
  openMoveOutDialog(): void {
    if (!this.tenant) return;

    const dialogRef = this.dialog.open(MoveOutDialogComponent, {
      width: '440px',
      disableClose: true,
      data: {
        tenantName: this.tenant.fullName,
        moveInDate: this.tenant.moveInDate,
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      // result is null when user clicked Cancel
      if (!result) return;

      this.isMovingOut = true;

      this.tenantService.moveOut(this.tenant!.id, result).subscribe({
        next: (updated) => {
          // Replace local tenant object with server response —
          // this is what changes isCurrentTenant from true → false,
          // causing the template to show "Moved Out" and hide the button
          this.tenant = updated;
          this.isMovingOut = false;
          this.notification.success(
            `${updated.fullName} has moved out. Property is now available.`,
          );
        },
        error: (err) => {
          this.isMovingOut = false;
          this.notification.error(
            err.error?.message ||
              'Failed to process move-out. Please try again.',
          );
        },
      });
    });
  }

  viewProperty(): void {
    if (this.tenant) {
      this.router.navigate(['/properties', this.tenant.propertyId]);
    }
  }

  back(): void {
    this.router.navigate(['/tenants']);
  }

  calculateDuration(): string {
    if (!this.tenant) return '';

    const start = new Date(this.tenant.moveInDate);
    const end = this.tenant.moveOutDate
      ? new Date(this.tenant.moveOutDate)
      : new Date();

    const months =
      (end.getFullYear() - start.getFullYear()) * 12 +
      (end.getMonth() - start.getMonth());

    if (months < 1) return 'Less than a month';
    if (months < 12) return `${months} month${months > 1 ? 's' : ''}`;

    const years = Math.floor(months / 12);
    const remMonths = months % 12;
    return `${years} year${years > 1 ? 's' : ''}${remMonths ? `, ${remMonths} mo` : ''}`;
  }
}
