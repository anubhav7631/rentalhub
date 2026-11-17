import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { AgreementService } from '../agreement.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AgreementResponse } from '../../../core/models/agreement.models';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-agreement-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
  ],
  templateUrl: './agreement-detail.component.html',
  styleUrls: ['./agreement-detail.component.scss'],
})
export class AgreementDetailComponent implements OnInit {
  agreement: AgreementResponse | null = null;
  isLoading = true;

  constructor(
    private agreementService: AgreementService,
    private notification: NotificationService,
    private dialog: MatDialog,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    const id = +this.route.snapshot.paramMap.get('id')!;
    this.agreementService.getById(id).subscribe({
      next: (a) => {
        this.agreement = a;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.notification.error('Agreement not found');
        this.router.navigate(['/agreements']);
      },
    });
  }

  download(): void {
    if (!this.agreement) return;
    this.notification.info('Preparing download...');
    this.agreementService.download(
      this.agreement.id,
      this.agreement.fileName || `${this.agreement.agreementNumber}.pdf`,
    );
  }

  confirmDelete(): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete agreement?',
        message: `Agreement ${this.agreement!.agreementNumber} will be permanently deleted.`,
        confirmLabel: 'Delete',
        confirmColor: 'warn',
      },
    });

    ref.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.agreementService.delete(this.agreement!.id).subscribe({
          next: () => {
            this.notification.success('Agreement deleted');
            this.router.navigate(['/agreements']);
          },
          error: (err) =>
            this.notification.error(err.error?.message || 'Failed to delete'),
        });
      }
    });
  }

  getExpiryClass(): string {
    switch (this.agreement?.expiryStatus) {
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

  back(): void {
    this.router.navigate(['/agreements']);
  }
}
