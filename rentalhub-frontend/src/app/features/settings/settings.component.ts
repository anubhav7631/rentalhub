import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { CurrentUser } from '../../core/models/auth.models';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss'],
})
export class SettingsComponent implements OnInit {
  user: CurrentUser | null = null;
  form!: FormGroup;
  isSaving = false;
  hideCurrent = true;
  hideNew = true;

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private authService: AuthService,
    private notification: NotificationService,
  ) {}

  ngOnInit(): void {
    this.user = this.authService.getCurrentUser();

    this.form = this.fb.group(
      {
        currentPassword: ['', Validators.required],
        newPassword: [
          '',
          [
            Validators.required,
            Validators.pattern(
              /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/,
            ),
          ],
        ],
        confirmPassword: ['', Validators.required],
      },
      { validators: this.matchValidator },
    );
  }

  private matchValidator(g: AbstractControl): ValidationErrors | null {
    const a = g.get('newPassword')?.value;
    const b = g.get('confirmPassword')?.value;
    return a && b && a !== b ? { passwordMismatch: true } : null;
  }

  getRole(): string {
    return (this.user?.roles?.[0] ?? '').replace('ROLE_', '');
  }

  getInitials(): string {
    return (this.user?.fullName ?? '?')
      .split(' ')
      .map((n) => n[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    const { currentPassword, newPassword } = this.form.value;

    this.http
      .post(`${environment.apiUrl}/account/change-password`, {
        currentPassword,
        newPassword,
      })
      .subscribe({
        next: () => {
          this.isSaving = false;
          this.form.reset();
          this.notification.success('Password changed successfully');
        },
        error: (err) => {
          this.isSaving = false;
          this.notification.error(
            err.error?.message || 'Failed to change password',
          );
        },
      });
  }

  get newPassword() {
    return this.form.get('newPassword');
  }
  get confirmPassword() {
    return this.form.get('confirmPassword');
  }
}
