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
import { RouterModule, Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-register',
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
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;
  isLoading = false;
  hidePassword = true;
  hideConfirm = true;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private notification: NotificationService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    /*
     * FormBuilder.group() with a second argument accepts group-level validators.
     * passwordMatchValidator runs on the entire FormGroup — it can compare
     * any two fields. Individual field validators (Validators.required, etc.)
     * run on each FormControl independently.
     */
    this.registerForm = this.fb.group(
      {
        fullName: [
          '',
          [
            Validators.required,
            Validators.minLength(2),
            Validators.maxLength(150),
          ],
        ],
        email: ['', [Validators.required, Validators.email]],
        phone: ['', [Validators.pattern(/^[+]?[0-9]{10,15}$/)]],
        role: ['ROLE_OWNER', Validators.required],
        password: [
          '',
          [
            Validators.required,
            Validators.minLength(8),
            // Must contain: uppercase, lowercase, digit, special character
            Validators.pattern(
              /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/,
            ),
          ],
        ],
        confirmPassword: ['', Validators.required],
      },
      { validators: this.passwordMatchValidator },
    );
  }

  /*
   * Custom cross-field validator.
   * Receives the whole FormGroup as AbstractControl.
   * Returns { passwordMismatch: true } if passwords don't match, null if they do.
   * Null = valid, any object = invalid.
   */
  private passwordMatchValidator(
    group: AbstractControl,
  ): ValidationErrors | null {
    const pw = group.get('password')?.value;
    const cp = group.get('confirmPassword')?.value;
    if (!pw || !cp) return null;
    return pw === cp ? null : { passwordMismatch: true };
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;

    // Destructure to remove confirmPassword before sending to API
    // The backend UserRequestDTO has no confirmPassword field
    const { confirmPassword, ...request } = this.registerForm.value;

    this.authService.register(request).subscribe({
      next: () => {
        this.isLoading = false;
        this.notification.success('Account created! Welcome to RentalHub.');
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        this.notification.error(
          err.error?.message || 'Registration failed. Please try again.',
        );
      },
    });
  }

  // Convenience getters — used in the template to read validation errors
  get fullName() {
    return this.registerForm.get('fullName');
  }
  get email() {
    return this.registerForm.get('email');
  }
  get phone() {
    return this.registerForm.get('phone');
  }
  get password() {
    return this.registerForm.get('password');
  }
  get confirmPassword() {
    return this.registerForm.get('confirmPassword');
  }
  get role() {
    return this.registerForm.get('role');
  }
}
