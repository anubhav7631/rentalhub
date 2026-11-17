import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  confirmColor?: 'primary' | 'accent' | 'warn';
}

/**
 * ConfirmDialogComponent — reusable confirmation dialog.
 *
 * USAGE:
 *   const ref = this.dialog.open(ConfirmDialogComponent, {
 *     width: '420px',
 *     data: { title: 'Delete?', message: '...', confirmColor: 'warn' }
 *   });
 *   ref.afterClosed().subscribe(confirmed => { if (confirmed) { ... } });
 *
 * MAT_DIALOG_DATA: injection token Angular Material uses to pass
 * the `data` object from .open() into this component.
 *
 * MatDialogRef<ConfirmDialogComponent>: typed reference to THIS
 * dialog instance — used to call .close(true) / .close(false).
 */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p>{{ data.message }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close(false)">
        {{ data.cancelLabel || 'Cancel' }}
      </button>
      <button
        mat-raised-button
        [color]="data.confirmColor || 'primary'"
        (click)="dialogRef.close(true)"
      >
        {{ data.confirmLabel || 'Confirm' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      mat-dialog-content p {
        color: var(--rh-text-secondary);
        line-height: 1.6;
        font-size: 0.9rem;
      }
      h2 {
        font-size: 1.1rem;
        font-weight: 700;
      }
    `,
  ],
})
export class ConfirmDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData,
  ) {}
}
