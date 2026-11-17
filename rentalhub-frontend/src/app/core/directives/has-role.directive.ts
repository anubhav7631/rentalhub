import {
  Directive,
  Input,
  OnInit,
  OnDestroy,
  TemplateRef,
  ViewContainerRef,
} from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Directive({
  selector: '[appHasRole]',
  standalone: true,
})
export class HasRoleDirective implements OnInit, OnDestroy {
  // The array of roles passed in: *appHasRole="['ROLE_ADMIN']"
  // Setter pattern: re-evaluates visibility whenever the input changes
  private requiredRoles: string[] = [];
  private hasView = false;
  private destroy$ = new Subject<void>();

  @Input() set appHasRole(roles: string[]) {
    this.requiredRoles = roles;
    this.updateView();
  }

  constructor(
    private templateRef: TemplateRef<unknown>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    // Re-check visibility whenever auth state changes
    // (e.g., user logs out — element should disappear immediately)
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.updateView());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateView(): void {
    const shouldShow = this.authService.hasAnyRole(this.requiredRoles);

    if (shouldShow && !this.hasView) {
      // Insert the element into the DOM
      this.viewContainer.createEmbeddedView(this.templateRef);
      this.hasView = true;
    } else if (!shouldShow && this.hasView) {
      // Remove the element from the DOM entirely
      this.viewContainer.clear();
      this.hasView = false;
    }
  }
}
