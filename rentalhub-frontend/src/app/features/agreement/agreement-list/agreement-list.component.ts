import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { AgreementService } from '../agreement.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AgreementResponse } from '../../../core/models/agreement.models';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-agreement-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatTooltipModule,
    MatDialogModule,
  ],
  templateUrl: './agreement-list.component.html',
  styleUrls: ['./agreement-list.component.scss'],
})
export class AgreementListComponent implements OnInit {
  displayedColumns = [
    'agreementNumber',
    'property',
    'tenant',
    'dates',
    'rent',
    'status',
    'actions',
  ];

  agreements: AgreementResponse[] = [];
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  isLoading = true;

  constructor(
    private agreementService: AgreementService,
    private notification: NotificationService,
    private dialog: MatDialog,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.agreementService.list(this.pageIndex, this.pageSize).subscribe({
      next: (page) => {
        this.agreements = page.content;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.notification.error('Failed to load agreements');
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  download(agreement: AgreementResponse, event: Event): void {
    event.stopPropagation();
    this.notification.info('Preparing download...');
    this.agreementService.download(
      agreement.id,
      agreement.fileName || `${agreement.agreementNumber}.pdf`,
    );
  }

  confirmDelete(agreement: AgreementResponse, event: Event): void {
    event.stopPropagation();

    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete agreement?',
        message: `Agreement ${agreement.agreementNumber} will be permanently deleted. This cannot be undone.`,
        confirmLabel: 'Delete',
        confirmColor: 'warn',
      },
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.agreementService.delete(agreement.id).subscribe({
          next: () => {
            this.notification.success('Agreement deleted');
            this.load();
          },
          error: (err) =>
            this.notification.error(err.error?.message || 'Failed to delete'),
        });
      }
    });
  }

  goToUpload(): void {
    this.router.navigate(['/agreements/new']);
  }

  goToDetail(id: number): void {
    this.router.navigate(['/agreements', id]);
  }

  getExpiryClass(status: string): string {
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

  getExpiryLabel(agreement: AgreementResponse): string {
    if (agreement.expiryStatus === 'EXPIRED') return 'Expired';
    if (agreement.expiryStatus === 'EXPIRING_SOON') {
      return `Expires in ${agreement.daysUntilExpiry}d`;
    }
    return 'Active';
  }
}
