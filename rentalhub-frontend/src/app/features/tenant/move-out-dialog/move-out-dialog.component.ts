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

export interface MoveOutDialogData {
  tenantName: string;
  moveInDate: string;
}

@Component({
  selector: 'app-move-out-dialog',
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
      <h2 mat-dialog-title>End Tenancy</h2>
      <p>{{ data.tenantName }}</p>
    </div>

    <mat-dialog-content>
      <form [formGroup]="form">
        <div class="field-group">
          <label class="field-label">Move-out Date</label>
          <mat-form-field appearance="outline" class="full-width">
            <input
              matInput
              [matDatepicker]="picker"
              formControlName="moveOutDate"
              [min]="minDate"
              placeholder="Select date"
            />
            <mat-datepicker-toggle matIconSuffix [for]="picker">
            </mat-datepicker-toggle>
            <mat-datepicker #picker></mat-datepicker>
            <mat-error *ngIf="form.get('moveOutDate')?.hasError('required')">
              Move-out date is required
            </mat-error>
            <mat-error
              *ngIf="form.get('moveOutDate')?.hasError('matDatepickerMin')"
            >
              Date cannot be before move-in date
            </mat-error>
          </mat-form-field>
        </div>

        <div class="field-group">
          <label class="field-label"
            >Notes <span class="optional">(optional)</span></label
          >
          <mat-form-field appearance="outline" class="full-width">
            <textarea
              matInput
              formControlName="notes"
              rows="3"
              placeholder="Reason for leaving, deposit settlement, condition notes..."
            >
            </textarea>
          </mat-form-field>
        </div>
      </form>

      <div class="warning-box">
        <mat-icon>info_outline</mat-icon>
        <span>
          The property will be marked as <strong>Available</strong>
          once you confirm, allowing a new tenant to be assigned.
        </span>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="cancel()">Cancel</button>
      <button
        mat-raised-button
        color="warn"
        type="button"
        [disabled]="form.invalid"
        (click)="confirm()"
      >
        <mat-icon>exit_to_app</mat-icon>
        Confirm Move-out
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

      .warning-box {
        display: flex;
        align-items: flex-start;
        gap: 10px;
        background: #fef3c7;
        border: 1px solid #fcd34d;
        border-radius: 8px;
        padding: 12px 14px;
        margin: 4px 0 8px;
        font-size: 0.8rem;
        color: #92400e;
        line-height: 1.5;

        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
          flex-shrink: 0;
          margin-top: 1px;
        }
      }

      mat-dialog-actions {
        padding: 12px 24px 20px;
        gap: 8px;
      }
    `,
  ],
})
export class MoveOutDialogComponent implements OnInit {
  form!: FormGroup;
  minDate!: Date;

  constructor(
    public dialogRef: MatDialogRef<MoveOutDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MoveOutDialogData,
    private fb: FormBuilder,
  ) {}

  ngOnInit(): void {
    // minDate = move-in date (can't move out before moving in)
    this.minDate = new Date(this.data.moveInDate);

    this.form = this.fb.group({
      moveOutDate: [new Date(), Validators.required],
      notes: [''],
    });
  }

  cancel(): void {
    // Returns null — caller checks for null before calling moveOut()
    this.dialogRef.close(null);
  }

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.value.moveOutDate as Date;
    const yyyy = raw.getFullYear();
    const mm = String(raw.getMonth() + 1).padStart(2, '0');
    const dd = String(raw.getDate()).padStart(2, '0');

    this.dialogRef.close({
      moveOutDate: `${yyyy}-${mm}-${dd}`, // Java LocalDate format
      notes: this.form.value.notes || undefined,
    });
  }
}
