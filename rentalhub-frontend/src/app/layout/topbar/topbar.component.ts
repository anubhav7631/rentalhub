import {
  Component,
  EventEmitter,
  OnInit,
  OnDestroy,
  Output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../core/services/auth.service';
import { NotificationPollService } from '../../core/services/notification-poll.service';
import { CurrentUser } from '../../core/models/auth.models';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatBadgeModule,
    MatTooltipModule,
    MatDividerModule,
  ],
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.scss'],
})
export class TopbarComponent implements OnInit, OnDestroy {
  @Output() toggleSidenav = new EventEmitter<void>();

  currentUser: CurrentUser | null = null;

  // Real unread count from the poll service
  notificationCount = 0;

  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private pollService: NotificationPollService,
  ) {}

  ngOnInit(): void {
    // Subscribe to auth state
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe((user) => (this.currentUser = user));

    // Subscribe to live notification count from polling service
    this.pollService.unreadCount$
      .pipe(takeUntil(this.destroy$))
      .subscribe((count) => (this.notificationCount = count));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  getInitials(): string {
    if (!this.currentUser?.fullName) return '?';
    return this.currentUser.fullName
      .split(' ')
      .map((n) => n[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }

  getPrimaryRole(): string {
    return (this.currentUser?.roles?.[0] ?? '').replace('ROLE_', '');
  }

  onToggleSidenav(): void {
    this.toggleSidenav.emit();
  }
  onLogout(): void {
    this.authService.logout();
  }
}
