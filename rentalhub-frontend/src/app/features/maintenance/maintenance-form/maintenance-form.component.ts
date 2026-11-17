import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MaintenanceService } from '../maintenance.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { MAINTENANCE_CATEGORIES } from '../../../core/models/maintenance.models';
import { environment } from '../../../../environments/environment';

interface PropertyOption {
  id: number;
  propertyName: string;
  city: string;
}

@Component({
  selector: 'app-maintenance-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './maintenance-form.component.html',
  styleUrls: ['./maintenance-form.component.scss'],
})
export class MaintenanceFormComponent implements OnInit {
  form!: FormGroup;
  isSaving = false;

  properties: PropertyOption[] = [];
  isLoadingProperties = true;

  selectedImages: File[] = [];
  imagePreviews: string[] = [];

  readonly categories = MAINTENANCE_CATEGORIES;

  isTenant = false;

  constructor(
    private fb: FormBuilder,
    private maintenanceService: MaintenanceService,
    private http: HttpClient,
    private authService: AuthService,
    private notification: NotificationService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.isTenant = this.authService.hasRole('ROLE_TENANT');

    this.form = this.fb.group({
      propertyId: [null, Validators.required],
      title: ['', [Validators.required, Validators.maxLength(200)]],
      description: ['', Validators.required],
      category: [''],
    });

    this.loadProperties();

    // Pre-select from query param
    const pid = this.route.snapshot.queryParamMap.get('propertyId');
    if (pid) this.form.patchValue({ propertyId: +pid });
  }

  private loadProperties(): void {
    const endpoint = this.isTenant
      ? `${environment.apiUrl}/tenants/property/current-for-me` // simplified
      : `${environment.apiUrl}/properties?size=100&status=OCCUPIED`;

    // For tenants we get their one property; for owners all occupied ones
    this.http.get<any>(endpoint).subscribe({
      next: (res) => {
        this.properties = res.content
          ? res.content.filter((p: any) => p.status === 'OCCUPIED')
          : [];
        this.isLoadingProperties = false;

        if (this.properties.length === 1) {
          this.form.patchValue({ propertyId: this.properties[0].id });
        }
      },
      error: () => {
        this.isLoadingProperties = false;

        // Fallback for tenants: try to load their active tenancy
        if (this.isTenant) {
          this.loadTenantProperty();
        }
      },
    });
  }

  /**
   * For tenants: load their active tenancy to get the property.
   * A tenant only has ONE current property.
   */
  private loadTenantProperty(): void {
    const userId = this.authService.getCurrentUser()?.userId;
    if (!userId) return;

    this.http.get<any>(`${environment.apiUrl}/properties?size=100`).subscribe({
      next: (res) => {
        this.properties = (res.content || []).filter(
          (p: any) => p.status === 'OCCUPIED',
        );
        this.isLoadingProperties = false;
        if (this.properties.length === 1) {
          this.form.patchValue({ propertyId: this.properties[0].id });
        }
      },
      error: () => {
        this.isLoadingProperties = false;
      },
    });
  }

  onImagesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;

    Array.from(input.files).forEach((file) => {
      if (!file.type.startsWith('image/')) {
        this.notification.warning(`${file.name} is not an image — skipped`);
        return;
      }
      this.selectedImages.push(file);
      this.imagePreviews.push(URL.createObjectURL(file));
    });

    input.value = '';
  }

  removeImage(index: number): void {
    URL.revokeObjectURL(this.imagePreviews[index]);
    this.selectedImages.splice(index, 1);
    this.imagePreviews.splice(index, 1);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving = true;

    this.maintenanceService
      .create(this.form.value, this.selectedImages)
      .subscribe({
        next: (req) => {
          this.isSaving = false;
          this.notification.success('Maintenance request submitted');
          this.router.navigate(['/maintenance', req.id]);
        },
        error: (err) => {
          this.isSaving = false;
          this.notification.error(
            err.error?.message || 'Failed to submit request',
          );
        },
      });
  }

  cancel(): void {
    this.router.navigate(['/maintenance']);
  }

  get titleField() {
    return this.form.get('title');
  }
  get description() {
    return this.form.get('description');
  }
  get propertyId() {
    return this.form.get('propertyId');
  }
}
