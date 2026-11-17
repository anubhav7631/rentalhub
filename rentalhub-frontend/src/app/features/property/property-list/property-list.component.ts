import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { PropertyService } from '../property.service';
import { NotificationService } from '../../../core/services/notification.service';
import { PropertyResponse } from '../../../core/models/property.models';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-property-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatPaginatorModule,
    MatMenuModule,
    MatDialogModule,
  ],
  templateUrl: './property-list.component.html',
  styleUrls: ['./property-list.component.scss'],
})
export class PropertyListComponent implements OnInit {
  properties: PropertyResponse[] = [];
  totalElements = 0;
  pageSize = 12;
  pageIndex = 0;

  searchTerm = '';
  isLoading = true;

  /*
   * Subject + debounceTime: search-as-you-type without firing an
   * API call on every keystroke.
   *
   * searchSubject.next(value) is called on every (input) event.
   * .pipe(debounceTime(400)) waits 400ms of silence before letting
   * the value through. distinctUntilChanged() skips if the value
   * didn't actually change (e.g., user typed then immediately
   * deleted back to the same string).
   */
  private searchSubject = new Subject<string>();

  constructor(
    private propertyService: PropertyService,
    private notification: NotificationService,
    private dialog: MatDialog,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(400), distinctUntilChanged())
      .subscribe(() => {
        this.pageIndex = 0; // reset to first page on new search
        this.loadProperties();
      });

    this.loadProperties();
  }

  loadProperties(): void {
    this.isLoading = true;

    this.propertyService
      .list(this.pageIndex, this.pageSize, this.searchTerm)
      .subscribe({
        next: (response) => {
          this.properties = response.content;
          this.totalElements = response.totalElements;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
          this.notification.error('Failed to load properties');
        },
      });
  }

  onSearchInput(): void {
    this.searchSubject.next(this.searchTerm);
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProperties();
  }

  getPrimaryImage(property: PropertyResponse): string | null {
    const primary =
      property.images.find((img) => img.isPrimary) ?? property.images[0];
    return primary ? this.propertyService.getImageUrl(primary.filePath) : null;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'AVAILABLE':
        return 'status-badge--success';
      case 'OCCUPIED':
        return 'status-badge--info';
      case 'MAINTENANCE':
        return 'status-badge--warning';
      case 'INACTIVE':
        return 'status-badge--danger';
      default:
        return '';
    }
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(value);
  }

  /**
   * confirmDelete — opens a confirmation dialog before deleting.
   * Destructive actions should never fire immediately on click.
   */
  confirmDelete(property: PropertyResponse, event: Event): void {
    event.stopPropagation(); // prevent the card click (navigate) from also firing

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete property?',
        message: `"${property.propertyName}" will be marked inactive. This action can be reviewed by an administrator.`,
        confirmLabel: 'Delete',
        confirmColor: 'warn',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.propertyService.delete(property.id).subscribe({
          next: () => {
            this.notification.success('Property deleted');
            this.loadProperties();
          },
          error: (err) => {
            this.notification.error(
              err.error?.message || 'Failed to delete property',
            );
          },
        });
      }
    });
  }

  goToProperty(id: number): void {
    this.router.navigate(['/properties', id]);
  }

  goToCreate(): void {
    this.router.navigate(['/properties/new']);
  }

  goToEdit(id: number, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/properties', id, 'edit']);
  }
}
