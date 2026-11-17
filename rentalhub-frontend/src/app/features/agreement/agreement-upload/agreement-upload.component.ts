import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AgreementService } from '../agreement.service';
import { NotificationService } from '../../../core/services/notification.service';
import { environment } from '../../../../environments/environment';

interface PropertyOption {
  id: number;
  propertyName: string;
  city: string;
}
interface TenantOption {
  id: number;
  fullName: string;
  email: string;
}

@Component({
  selector: 'app-agreement-upload',
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
  templateUrl: './agreement-upload.component.html',
  styleUrls: ['./agreement-upload.component.scss'],
})
export class AgreementUploadComponent implements OnInit {
  form!: FormGroup;
  isSaving = false;

  properties: PropertyOption[] = [];
  tenants: TenantOption[] = [];
  selectedFile: File | null = null;
  fileError: string | null = null;

  isLoadingProperties = true;
  isLoadingTenants = false;

  constructor(
    private fb: FormBuilder,
    private agreementService: AgreementService,
    private http: HttpClient,
    private notification: NotificationService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      propertyId: [null, Validators.required],
      tenantId: [null, Validators.required],
      startDate: [null, Validators.required],
      endDate: [null, Validators.required],
      monthlyRent: [null, [Validators.required, Validators.min(1)]],
      depositAmount: [null, [Validators.required, Validators.min(0)]],
      notes: [''],
    });

    this.loadProperties();

    // When property changes, load the current tenant for that property
    this.form.get('propertyId')!.valueChanges.subscribe((propertyId) => {
      if (propertyId) {
        this.form.patchValue({ tenantId: null });
        this.loadTenantsForProperty(propertyId);
      }
    });
  }

  private loadProperties(): void {
    this.http
      .get<{
        content: PropertyOption[];
      }>(`${environment.apiUrl}/properties?size=100`)
      .subscribe({
        next: (res) => {
          this.properties = res.content;
          this.isLoadingProperties = false;
        },
        error: () => {
          this.isLoadingProperties = false;
        },
      });
  }

  private loadTenantsForProperty(propertyId: number): void {
    this.isLoadingTenants = true;
    this.tenants = [];

    // Get current tenant for this property
    this.http
      .get<TenantOption | null>(
        `${environment.apiUrl}/tenants/property/${propertyId}/current`,
      )
      .subscribe({
        next: (tenant) => {
          this.tenants = tenant ? [tenant] : [];
          this.isLoadingTenants = false;

          // Auto-select if there's only one tenant
          if (this.tenants.length === 1) {
            this.form.patchValue({ tenantId: this.tenants[0].id });
          }
        },
        error: () => {
          this.isLoadingTenants = false;
          this.notification.warning(
            'No current tenant found for this property. Assign a tenant first.',
          );
        },
      });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    const file = input.files[0];
    this.fileError = null;

    // Validate PDF only
    if (file.type !== 'application/pdf') {
      this.fileError = 'Only PDF files are accepted';
      this.selectedFile = null;
      input.value = '';
      return;
    }

    // Validate size (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
      this.fileError = 'File size must be under 10MB';
      this.selectedFile = null;
      input.value = '';
      return;
    }

    this.selectedFile = file;
  }

  removeFile(): void {
    this.selectedFile = null;
    this.fileError = null;
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    if (!this.selectedFile) {
      this.fileError = 'Please select a PDF agreement file';
      return;
    }

    this.isSaving = true;

    const value = this.form.value;

    const request = {
      ...value,
      startDate: this.formatDate(value.startDate),
      endDate: this.formatDate(value.endDate),
    };

    this.agreementService.upload(request, this.selectedFile).subscribe({
      next: (agreement) => {
        this.isSaving = false;
        this.notification.success(
          `Agreement ${agreement.agreementNumber} uploaded successfully`,
        );
        this.router.navigate(['/agreements', agreement.id]);
      },
      error: (err) => {
        this.isSaving = false;
        this.notification.error(
          err.error?.message || 'Failed to upload agreement',
        );
      },
    });
  }

  private formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  cancel(): void {
    this.router.navigate(['/agreements']);
  }

  get propertyId() {
    return this.form.get('propertyId');
  }
  get tenantId() {
    return this.form.get('tenantId');
  }
  get startDate() {
    return this.form.get('startDate');
  }
  get endDate() {
    return this.form.get('endDate');
  }
  get monthlyRent() {
    return this.form.get('monthlyRent');
  }
  get depositAmount() {
    return this.form.get('depositAmount');
  }
}
