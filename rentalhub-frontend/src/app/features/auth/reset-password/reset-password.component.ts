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
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NotificationService } from '../../../core/services/notification.service';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.scss'],
})
export class ResetPasswordComponent implements OnInit {
  form!: FormGroup;
  isLoading = false;
  isValidating = true; // checking token on page load
  isTokenValid = false;
  isSuccess = false;
  hidePassword = true;
  hideConfirm = true;

  private token = '';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private notification: NotificationService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    // Read token from URL: /reset-password?token=abc123
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';

    if (!this.token) {
      this.isValidating = false;
      this.isTokenValid = false;
      return;
    }

    this.form = this.fb.group(
      {
        newPassword: [
          '',
          [
            Validators.required,
            Validators.minLength(8),
            Validators.pattern(
              /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/,
            ),
          ],
        ],
        confirmPassword: ['', Validators.required],
      },
      { validators: this.passwordMatchValidator },
    );

    // Validate token with backend before showing the form
    this.http
      .get<{
        valid: boolean;
      }>(`${environment.apiUrl}/auth/validate-reset-token?token=${this.token}`)
      .subscribe({
        next: (res) => {
          this.isValidating = false;
          this.isTokenValid = res.valid;
          if (!res.valid) {
            this.notification.error(
              'This reset link has expired or is invalid.',
            );
          }
        },
        error: () => {
          this.isValidating = false;
          this.isTokenValid = false;
        },
      });
  }

  private passwordMatchValidator(
    group: AbstractControl,
  ): ValidationErrors | null {
    const pw = group.get('newPassword')?.value;
    const cp = group.get('confirmPassword')?.value;
    if (!pw || !cp) return null;
    return pw === cp ? null : { passwordMismatch: true };
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading = true;

    this.http
      .post(`${environment.apiUrl}/auth/reset-password`, {
        token: this.token,
        newPassword: this.form.value.newPassword,
      })
      .subscribe({
        next: () => {
          this.isLoading = false;
          this.isSuccess = true;
          // Redirect to login after 3 seconds
          setTimeout(() => this.router.navigate(['/login']), 3000);
        },
        error: (err) => {
          this.isLoading = false;
          this.notification.error(
            err.error?.message || 'Failed to reset password. Please try again.',
          );
        },
      });
  }

  get newPassword() {
    return this.form?.get('newPassword');
  }
  get confirmPassword() {
    return this.form?.get('confirmPassword');
  }
}
