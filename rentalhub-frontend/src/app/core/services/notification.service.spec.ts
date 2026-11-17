import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  constructor(private snackBar: MatSnackBar) {}

  success(message: string, duration = 4000): void {
    this.snackBar.open(message, '✕', {
      duration,
      panelClass: ['snack-success'],
      horizontalPosition: 'right',
      verticalPosition: 'top',
    });
  }

  error(message: string, duration = 6000): void {
    this.snackBar.open(message, '✕', {
      duration,
      panelClass: ['snack-error'],
      horizontalPosition: 'right',
      verticalPosition: 'top',
    });
  }

  warning(message: string, duration = 4000): void {
    this.snackBar.open(message, '✕', {
      duration,
      panelClass: ['snack-warn'],
      horizontalPosition: 'right',
      verticalPosition: 'top',
    });
  }

  info(message: string, duration = 4000): void {
    this.snackBar.open(message, '✕', {
      duration,
      horizontalPosition: 'right',
      verticalPosition: 'top',
    });
  }
}
