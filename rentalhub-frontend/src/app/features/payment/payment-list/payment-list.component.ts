import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSelectModule } from '@angular/material/select';
import { PaymentService } from '../payment.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  PaymentResponse,
  PaymentSummary,
} from '../../../core/models/payment.models';
import { RecordPaymentDialogComponent } from '../record-payment-dialog/record-payment-dialog.component';

@Component({
  selector: 'app-payment-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatTooltipModule,
    MatDialogModule,
    MatSelectModule,
  ],
  templateUrl: './payment-list.component.html',
  styleUrls: ['./payment-list.component.scss'],
})
export class PaymentListComponent implements OnInit {
  displayedColumns: string[] = [];

  payments: PaymentResponse[] = [];
  summary: PaymentSummary | null = null;

  totalElements = 0;
  pageSize = 12;
  pageIndex = 0;
  isLoading = true;

  // true for ADMIN and OWNER, false for TENANT
  isOwnerOrAdmin = false;

  constructor(
    private paymentService: PaymentService,
    private notification: NotificationService,
    private authService: AuthService,
    private dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.isOwnerOrAdmin = this.authService.hasAnyRole([
      'ROLE_ADMIN',
      'ROLE_OWNER',
    ]);

    // Tenants don't see the Tenant column or the Actions column
    this.displayedColumns = this.isOwnerOrAdmin
      ? [
          'month',
          'property',
          'tenant',
          'amount',
          'dueDate',
          'status',
          'actions',
        ]
      : ['month', 'property', 'amount', 'dueDate', 'status'];

    // Summary endpoint is owner/admin only. Calling it as a tenant gives 403.
    if (this.isOwnerOrAdmin) {
      this.loadSummary();
    }

    this.loadPayments();
  }

  loadSummary(): void {
    this.paymentService.getSummary().subscribe({
      next: (s) => (this.summary = s),
      error: () => {},
    });
  }

  loadPayments(): void {
    this.isLoading = true;
    this.paymentService.list(this.pageIndex, this.pageSize).subscribe({
      next: (page) => {
        this.payments = page.content;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.notification.error('Failed to load payments');
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadPayments();
  }

  openRecordDialog(payment: PaymentResponse, event: Event): void {
    event.stopPropagation();

    const ref = this.dialog.open(RecordPaymentDialogComponent, {
      width: '440px',
      data: {
        paymentId: payment.id,
        monthLabel: payment.monthLabel,
        amount: payment.amount,
        tenantName: payment.tenantName,
        propertyName: payment.propertyName,
      },
    });

    ref.afterClosed().subscribe((result) => {
      if (!result) return;

      this.paymentService.markPaid(payment.id, result).subscribe({
        next: (updated) => {
          const idx = this.payments.findIndex((p) => p.id === payment.id);
          if (idx !== -1) this.payments[idx] = updated;
          this.payments = [...this.payments];

          this.notification.success(
            `${updated.monthLabel} rent marked as PAID`,
          );
          this.loadSummary();
        },
        error: (err) =>
          this.notification.error(
            err.error?.message || 'Failed to record payment',
          ),
      });
    });
  }

  waive(payment: PaymentResponse, event: Event): void {
    event.stopPropagation();

    this.paymentService.waive(payment.id, 'Waived by owner').subscribe({
      next: (updated) => {
        const idx = this.payments.findIndex((p) => p.id === payment.id);
        if (idx !== -1) this.payments[idx] = updated;
        this.payments = [...this.payments];
        this.notification.success('Payment waived');
        this.loadSummary();
      },
      error: (err) =>
        this.notification.error(
          err.error?.message || 'Failed to waive payment',
        ),
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PAID':
        return 'status-badge--success';
      case 'PENDING':
        return 'status-badge--info';
      case 'OVERDUE':
        return 'status-badge--danger';
      case 'WAIVED':
        return 'status-badge--warning';
      default:
        return '';
    }
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(value);
  }

  isActionable(payment: PaymentResponse): boolean {
    return payment.status === 'PENDING' || payment.status === 'OVERDUE';
  }
}
