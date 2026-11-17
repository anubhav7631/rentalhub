import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MaintenanceService } from '../maintenance.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  MaintenanceResponse,
  MaintenanceStatus,
} from '../../../core/models/maintenance.models';

// Allowed next states from a given current state
const TRANSITIONS: Record<MaintenanceStatus, MaintenanceStatus[]> = {
  OPEN: ['IN_PROGRESS', 'REJECTED'],
  IN_PROGRESS: ['COMPLETED', 'REJECTED'],
  COMPLETED: [],
  REJECTED: [],
};

@Component({
  selector: 'app-maintenance-detail',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './maintenance-detail.component.html',
  styleUrls: ['./maintenance-detail.component.scss'],
})
export class MaintenanceDetailComponent implements OnInit {
  request: MaintenanceResponse | null = null;
  isLoading = true;
  isUpdating = false;
  showUpdateForm = false;

  updateForm!: FormGroup;
  isOwnerOrAdmin = false;

  constructor(
    private maintenanceService: MaintenanceService,
    private notification: NotificationService,
    private authService: AuthService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.isOwnerOrAdmin = this.authService.hasAnyRole([
      'ROLE_ADMIN',
      'ROLE_OWNER',
    ]);

    this.updateForm = this.fb.group({
      status: ['', Validators.required],
      resolutionNotes: [''],
    });

    const id = +this.route.snapshot.paramMap.get('id')!;
    this.loadRequest(id);
  }

  loadRequest(id: number): void {
    this.isLoading = true;
    this.maintenanceService.getById(id).subscribe({
      next: (r) => {
        this.request = r;
        this.isLoading = false;
        // Pre-populate next status options in the form
        const allowed = this.getAllowedNextStatuses();
        if (allowed.length > 0) {
          this.updateForm.patchValue({ status: allowed[0] });
        }
      },
      error: () => {
        this.isLoading = false;
        this.notification.error('Request not found');
        this.router.navigate(['/maintenance']);
      },
    });
  }

  getAllowedNextStatuses(): MaintenanceStatus[] {
    if (!this.request) return [];
    return TRANSITIONS[this.request.status] ?? [];
  }

  submitStatusUpdate(): void {
    if (this.updateForm.invalid || !this.request) return;

    this.isUpdating = true;

    this.maintenanceService
      .updateStatus(this.request.id, this.updateForm.value)
      .subscribe({
        next: (updated) => {
          this.request = updated;
          this.isUpdating = false;
          this.showUpdateForm = false;
          this.notification.success(`Status updated to ${updated.status}`);
        },
        error: (err) => {
          this.isUpdating = false;
          this.notification.error(
            err.error?.message || 'Failed to update status',
          );
        },
      });
  }

  getImageUrl(path: string): string {
    return this.maintenanceService.getImageUrl(path);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'OPEN':
        return 'chip-open';
      case 'IN_PROGRESS':
        return 'chip-inprogress';
      case 'COMPLETED':
        return 'chip-completed';
      case 'REJECTED':
        return 'chip-rejected';
      default:
        return '';
    }
  }

  isTerminal(): boolean {
    return (
      this.request?.status === 'COMPLETED' ||
      this.request?.status === 'REJECTED'
    );
  }

  back(): void {
    this.router.navigate(['/maintenance']);
  }
}
