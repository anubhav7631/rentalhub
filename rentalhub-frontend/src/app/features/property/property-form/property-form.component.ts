import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PropertyService } from '../property.service';
import { NotificationService } from '../../../core/services/notification.service';
import {
  PropertyType,
  PropertyResponse,
} from '../../../core/models/property.models';

/** Local type for tracking images during the form session */
interface ImagePreview {
  file?: File; // present for newly-added images (not yet uploaded)
  url: string; // object URL (new) or server URL (existing)
  existingId?: number; // present for images already on the server
  isPrimary: boolean;
}

@Component({
  selector: 'app-property-form',
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
  templateUrl: './property-form.component.html',
  styleUrls: ['./property-form.component.scss'],
})
export class PropertyFormComponent implements OnInit {
  form!: FormGroup;
  propertyTypes = Object.values(PropertyType);

  isEditMode = false;
  propertyId: number | null = null;
  isLoading = false;
  isSaving = false;

  images: ImagePreview[] = [];

  constructor(
    private fb: FormBuilder,
    private propertyService: PropertyService,
    private notification: NotificationService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      propertyName: ['', [Validators.required, Validators.maxLength(200)]],
      propertyType: ['APARTMENT', Validators.required],
      addressLine1: ['', Validators.required],
      addressLine2: [''],
      city: ['', Validators.required],
      state: ['', Validators.required],
      pincode: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]],
      rentAmount: [null, [Validators.required, Validators.min(1)]],
      depositAmount: [null, [Validators.required, Validators.min(0)]],
      bedrooms: [1, [Validators.required, Validators.min(1)]],
      bathrooms: [1, [Validators.required, Validators.min(1)]],
      areaSqft: [null],
      description: [''],
    });

    // Edit mode: route has an :id param
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.propertyId = +idParam;
      this.loadProperty(this.propertyId);
    }
  }

  loadProperty(id: number): void {
    this.isLoading = true;
    this.propertyService.getById(id).subscribe({
      next: (property) => {
        this.form.patchValue(property);
        this.images = property.images.map((img) => ({
          url: this.propertyService.getImageUrl(img.filePath),
          existingId: img.id,
          isPrimary: img.isPrimary,
        }));
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.notification.error('Failed to load property');
        this.router.navigate(['/properties']);
      },
    });
  }

  /**
   * onFilesSelected — handles the <input type="file" multiple> change event.
   *
   * For each selected file:
   *   1. Create an object URL for instant preview (URL.createObjectURL)
   *   2. Push to images[] with the raw File attached
   *
   * If this is the very first image added (no existing images), mark
   * it primary by default — matches the backend's auto-primary logic
   * for brand-new properties.
   */
  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;

    const hasExistingPrimary = this.images.some((img) => img.isPrimary);

    Array.from(input.files).forEach((file, index) => {
      this.images.push({
        file,
        url: URL.createObjectURL(file),
        isPrimary:
          !hasExistingPrimary && this.images.length === 0 && index === 0,
      });
    });

    input.value = ''; // allow re-selecting the same file later
  }

  /**
   * removeImage — for existing (server-side) images, calls the delete
   * API immediately. For newly-added (not yet uploaded) images, just
   * removes from the local array.
   */
  removeImage(index: number): void {
    const image = this.images[index];

    if (image.existingId && this.propertyId) {
      this.propertyService
        .deleteImage(this.propertyId, image.existingId)
        .subscribe({
          next: () => {
            this.images.splice(index, 1);
            this.notification.success('Image removed');
          },
          error: () => this.notification.error('Failed to remove image'),
        });
    } else {
      // Revoke the object URL to free memory
      if (image.file) URL.revokeObjectURL(image.url);
      this.images.splice(index, 1);
    }
  }

  setPrimary(index: number): void {
    const image = this.images[index];

    if (image.existingId && this.propertyId) {
      this.propertyService
        .setPrimaryImage(this.propertyId, image.existingId)
        .subscribe({
          next: () => {
            this.images.forEach((img, i) => (img.isPrimary = i === index));
          },
          error: () => this.notification.error('Failed to set primary image'),
        });
    } else {
      // Local-only update — applies once the property is saved
      this.images.forEach((img, i) => (img.isPrimary = i === index));
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    const request = this.form.value;

    // Only NEW images (those with a `file`) are sent to the backend
    const newFiles = this.images
      .filter((img) => img.file)
      .map((img) => img.file!) as File[];

    const onSuccess = (property: PropertyResponse) => {
      this.isSaving = false;
      this.notification.success(
        this.isEditMode ? 'Property updated' : 'Property created',
      );
      this.router.navigate(['/properties', property.id]);
    };

    const onError = (err: any) => {
      this.isSaving = false;
      this.notification.error(err.error?.message || 'Failed to save property');
    };

    if (this.isEditMode && this.propertyId) {
      this.propertyService
        .update(this.propertyId, request, newFiles)
        .subscribe({ next: onSuccess, error: onError });
    } else {
      this.propertyService
        .create(request, newFiles)
        .subscribe({ next: onSuccess, error: onError });
    }
  }

  cancel(): void {
    this.router.navigate(['/properties']);
  }

  // Convenience getters
  get propertyName() {
    return this.form.get('propertyName');
  }
  get addressLine1() {
    return this.form.get('addressLine1');
  }
  get city() {
    return this.form.get('city');
  }
  get state() {
    return this.form.get('state');
  }
  get pincode() {
    return this.form.get('pincode');
  }
  get rentAmount() {
    return this.form.get('rentAmount');
  }
  get depositAmount() {
    return this.form.get('depositAmount');
  }
  get bedrooms() {
    return this.form.get('bedrooms');
  }
  get bathrooms() {
    return this.form.get('bathrooms');
  }
}
