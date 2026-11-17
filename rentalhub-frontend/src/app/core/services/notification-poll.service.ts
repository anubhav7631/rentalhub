// ─────────────────────────────────────────────────────────────────
// notification-poll.service.ts — polls unread count every 30 seconds
// ─────────────────────────────────────────────────────────────────
import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, interval, Subscription, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class NotificationPollService implements OnDestroy {
  private readonly apiUrl = `${environment.apiUrl}/notifications`;

  /*
   * BehaviorSubject<number> — the live unread count.
   * Starts at 0. Updated every 30 seconds by the poll.
   * Components subscribe to unreadCount$ to get live updates.
   */
  private unreadSubject = new BehaviorSubject<number>(0);
  unreadCount$ = this.unreadSubject.asObservable();

  private pollSubscription: Subscription | null = null;

  constructor(
    private http: HttpClient,
    private authService: AuthService,
  ) {
    // Start polling when a user is logged in
    this.authService.currentUser$.subscribe((user) => {
      if (user) {
        this.startPolling();
      } else {
        this.stopPolling();
        this.unreadSubject.next(0);
      }
    });
  }

  private startPolling(): void {
    if (this.pollSubscription) return; // already running

    // Fetch immediately, then every 30 seconds
    this.fetchUnreadCount();

    this.pollSubscription = interval(30_000)
      .pipe(
        /*
         * switchMap: when the interval emits, cancel any in-flight
         * HTTP request and start a new one. Prevents request pileup
         * if the server is slow.
         */
        switchMap(() =>
          this.http.get<{ count: number }>(`${this.apiUrl}/unread-count`),
        ),
      )
      .subscribe({
        next: (res) => this.unreadSubject.next(res.count),
        error: () => {}, // silently ignore poll errors
      });
  }

  private stopPolling(): void {
    this.pollSubscription?.unsubscribe();
    this.pollSubscription = null;
  }

  private fetchUnreadCount(): void {
    this.http.get<{ count: number }>(`${this.apiUrl}/unread-count`).subscribe({
      next: (res) => this.unreadSubject.next(res.count),
      error: () => {},
    });
  }

  // Called when a user marks notifications as read
  resetCount(): void {
    this.unreadSubject.next(0);
  }

  // Force an immediate refresh (called after marking as read)
  refresh(): void {
    this.fetchUnreadCount();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }
}
