import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface RecordPaymentDialogData {
  paymentId: number;
  monthLabel: string;
  amount: number;
  tenantName: string;
  propertyName: string;
}

@Component({
  selector: 'app-record-payment-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <div class="dialog-header">
      <h2 mat-dialog-title>Record Rent Payment</h2>
      <p>{{ data.monthLabel }} · {{ data.propertyName }}</p>
    </div>

    <mat-dialog-content>
      <div class="tenant-row">
        <mat-icon>person</mat-icon>
        <span>{{ data.tenantName }}</span>
      </div>

      <div class="amount-display">
        <span class="amount-display__label">Expected amount</span>
        <span class="amount-display__value"> ₹{{ data.amount | number }} </span>
      </div>

      <form [formGroup]="form">
        <div class="field-group">
          <label class="field-label">Date Received</label>
          <mat-form-field appearance="outline" class="full-width">
            <input
              matInput
              [matDatepicker]="picker"
              formControlName="paidDate"
            />
            <mat-datepicker-toggle matIconSuffix [for]="picker">
            </mat-datepicker-toggle>
            <mat-datepicker #picker></mat-datepicker>
            <mat-error *ngIf="form.get('paidDate')?.hasError('required')">
              Required
            </mat-error>
          </mat-form-field>
        </div>

        <div class="field-group">
          <label class="field-label">
            Amount Received
            <span class="optional">(leave blank to use expected amount)</span>
          </label>
          <mat-form-field appearance="outline" class="full-width">
            <span matTextPrefix>₹&nbsp;</span>
            <input
              matInput
              type="number"
              formControlName="amount"
              [placeholder]="data.amount.toString()"
            />
          </mat-form-field>
        </div>

        <div class="field-group">
          <label class="field-label">
            Payment Reference
            <span class="optional">(UPI ID, cheque no., etc.)</span>
          </label>
          <mat-form-field appearance="outline" class="full-width">
            <input
              matInput
              formControlName="paymentReference"
              placeholder="e.g. UPI/123456789012"
            />
            <mat-icon matSuffix>receipt</mat-icon>
          </mat-form-field>
        </div>

        <div class="field-group">
          <label class="field-label"
            >Notes <span class="optional">(optional)</span></label
          >
          <mat-form-field appearance="outline" class="full-width">
            <textarea matInput formControlName="notes" rows="2"></textarea>
          </mat-form-field>
        </div>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      <button
        mat-raised-button
        color="primary"
        [disabled]="form.invalid"
        (click)="confirm()"
      >
        <mat-icon>check</mat-icon>
        Confirm Payment
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dialog-header {
        padding: 20px 24px 0;
        h2 {
          font-size: 1.1rem;
          font-weight: 700;
          margin: 0 0 4px;
        }
        p {
          font-size: 0.875rem;
          color: var(--rh-text-secondary);
          margin: 0 0 16px;
        }
      }
      mat-dialog-content {
        padding: 0 24px;
      }
      .tenant-row {
        display: flex;
        align-items: center;
        gap: 8px;
        background: var(--rh-surface-2);
        border-radius: 8px;
        padding: 10px 14px;
        font-size: 0.875rem;
        color: var(--rh-text-secondary);
        margin-bottom: 16px;
        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
          color: var(--rh-primary);
        }
      }
      .amount-display {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 0;
        border-bottom: 1px solid var(--rh-border);
        margin-bottom: 16px;
        &__label {
          font-size: 0.875rem;
          color: var(--rh-text-secondary);
        }
        &__value {
          font-size: 1.25rem;
          font-weight: 700;
          color: var(--rh-text-primary);
        }
      }
      .field-group {
        margin-bottom: 4px;
      }
      .field-label {
        display: block;
        font-size: 0.8rem;
        font-weight: 600;
        color: #374151;
        margin-bottom: 6px;
      }
      .optional {
        font-weight: 400;
        color: var(--rh-text-muted);
      }
      .full-width {
        width: 100%;
      }
      mat-dialog-actions {
        padding: 12px 24px 20px;
        gap: 8px;
      }
    `,
  ],
})
export class RecordPaymentDialogComponent implements OnInit {
  form!: FormGroup;

  constructor(
    public dialogRef: MatDialogRef<RecordPaymentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RecordPaymentDialogData,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      paidDate: [new Date(), Validators.required],
      amount: [null],
      paymentReference: [''],
      notes: [''],
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    if (this.form.invalid) return;

    const v = this.form.value;
    const d = v.paidDate as Date;
    const formatted = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

    this.dialogRef.close({
      paidDate: formatted,
      amount: v.amount || undefined,
      paymentReference: v.paymentReference || undefined,
      notes: v.notes || undefined,
    });
  }
}
