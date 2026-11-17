import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { NotificationApiService } from '../../core/services/notification-api.service';
import { NotificationPollService } from '../../core/services/notification-poll.service';
import { NotificationItem } from '../../core/models/notification.models';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatPaginatorModule],
  templateUrl: './notification-list.component.html',
  styleUrls: ['./notification-list.component.scss'],
})
export class NotificationListComponent implements OnInit {
  notifications: NotificationItem[] = [];
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  isLoading = true;

  constructor(
    private apiService: NotificationApiService,
    private pollService: NotificationPollService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.apiService.list(this.pageIndex, this.pageSize).subscribe({
      next: (page) => {
        this.notifications = page.content;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  markAsRead(notification: NotificationItem, event: Event): void {
    event.stopPropagation();
    if (notification.isRead) return;

    this.apiService.markAsRead(notification.id).subscribe({
      next: () => {
        notification.isRead = true;
        this.pollService.refresh(); // update topbar badge
      },
    });
  }

  markAllAsRead(): void {
    this.apiService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.forEach((n) => (n.isRead = true));
        this.pollService.resetCount();
      },
    });
  }

  getTypeIcon(type: string): string {
    switch (type) {
      case 'RENT_PAID':
        return 'check_circle';
      case 'RENT_DUE':
        return 'schedule';
      case 'RENT_OVERDUE':
        return 'warning';
      case 'AGREEMENT_EXPIRING':
        return 'description';
      case 'AGREEMENT_EXPIRED':
        return 'error';
      case 'MAINTENANCE_UPDATED':
        return 'construction';
      case 'MAINTENANCE_COMPLETED':
        return 'build_circle';
      default:
        return 'notifications';
    }
  }

  getTypeColor(type: string): string {
    switch (type) {
      case 'RENT_PAID':
        return 'icon-success';
      case 'RENT_DUE':
        return 'icon-warning';
      case 'RENT_OVERDUE':
        return 'icon-danger';
      case 'AGREEMENT_EXPIRING':
        return 'icon-warning';
      case 'AGREEMENT_EXPIRED':
        return 'icon-danger';
      case 'MAINTENANCE_UPDATED':
        return 'icon-info';
      case 'MAINTENANCE_COMPLETED':
        return 'icon-success';
      default:
        return 'icon-default';
    }
  }

  get unreadCount(): number {
    return this.notifications.filter((n) => !n.isRead).length;
  }
}
