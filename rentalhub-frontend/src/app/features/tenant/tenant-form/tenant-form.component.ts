import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TenantService } from '../tenant.service';
import { NotificationService } from '../../../core/services/notification.service';
import { environment } from '../../../../environments/environment';

interface PropertyOption {
  id: number;
  propertyName: string;
  city: string;
  status: string;
}

@Component({
  selector: 'app-tenant-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './tenant-form.component.html',
  styleUrls: ['./tenant-form.component.scss'],
})
export class TenantFormComponent implements OnInit {
  form!: FormGroup;
  isSaving = false;

  // Only AVAILABLE properties can receive a new tenant
  availableProperties: PropertyOption[] = [];
  isLoadingProperties = true;

  constructor(
    private fb: FormBuilder,
    private tenantService: TenantService,
    private http: HttpClient,
    private notification: NotificationService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      propertyId: [null, Validators.required],
      fullName: ['', [Validators.required, Validators.maxLength(150)]],
      email: ['', [Validators.required, Validators.email]],
      phone: [
        '',
        [Validators.required, Validators.pattern(/^[+]?[0-9]{10,15}$/)],
      ],
      idProofType: [''],
      idProofNumber: [''],
      moveInDate: [new Date(), Validators.required],
      emergencyContactName: [''],
      emergencyContactPhone: [''],
      notes: [''],
    });

    this.loadAvailableProperties();

    // Pre-select property if navigated from a property's detail page
    // e.g. /tenants/new?propertyId=5
    const propertyIdParam = this.route.snapshot.queryParamMap.get('propertyId');
    if (propertyIdParam) {
      this.form.patchValue({ propertyId: +propertyIdParam });
    }
  }

  /**
   * Loads properties with status AVAILABLE for the dropdown.
   * Reuses the existing /api/properties endpoint with a large page
   * size — for a true production app with hundreds of properties,
   * a dedicated lightweight "available properties" endpoint would be
   * preferable, but this is sufficient for RentalHub's scale.
   */
  private loadAvailableProperties(): void {
    this.http
      .get<{
        content: PropertyOption[];
      }>(`${environment.apiUrl}/properties?size=100`)
      .subscribe({
        next: (response) => {
          this.availableProperties = response.content.filter(
            (p) => p.status === 'AVAILABLE',
          );
          this.isLoadingProperties = false;
        },
        error: () => {
          this.isLoadingProperties = false;
          this.notification.error('Failed to load properties');
        },
      });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving = true;

    const value = this.form.value;
    const request = {
      ...value,
      moveInDate: this.formatDate(value.moveInDate),
    };

    this.tenantService.assignTenant(request).subscribe({
      next: (tenant) => {
        this.isSaving = false;
        this.notification.success(
          `${tenant.fullName} assigned to ${tenant.propertyName}`,
        );
        this.router.navigate(['/tenants', tenant.id]);
      },
      error: (err) => {
        this.isSaving = false;
        this.notification.error(
          err.error?.message || 'Failed to assign tenant',
        );
      },
    });
  }

  private formatDate(date: Date): string {
    // Converts JS Date to "YYYY-MM-DD" — matches Java's LocalDate JSON format
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  cancel(): void {
    this.router.navigate(['/tenants']);
  }

  get propertyId() {
    return this.form.get('propertyId');
  }
  get fullName() {
    return this.form.get('fullName');
  }
  get email() {
    return this.form.get('email');
  }
  get phone() {
    return this.form.get('phone');
  }
  get moveInDate() {
    return this.form.get('moveInDate');
  }
}
