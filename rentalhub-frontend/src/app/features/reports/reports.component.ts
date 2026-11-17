import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { NotificationService } from '../../core/services/notification.service';
import { environment } from '../../../environments/environment';

interface ReportCard {
  title: string;
  description: string;
  icon: string;
  color: string;
  excelPath: string;
  pdfPath: string;
}

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatProgressBarModule,
  ],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss'],
})
export class ReportsComponent {
  selectedYear = new Date().getFullYear();
  downloadingKey: string | null = null;

  readonly years = Array.from(
    { length: 5 },
    (_, i) => new Date().getFullYear() - i,
  );

  readonly reports: ReportCard[] = [
    {
      title: 'Revenue Report',
      description:
        'Monthly rent collection breakdown — paid, pending, and overdue payments',
      icon: 'payments',
      color: 'primary',
      excelPath: '/reports/revenue/excel',
      pdfPath: '/reports/revenue/pdf',
    },
    {
      title: 'Tenant Report',
      description: 'All tenants with move-in/move-out dates and current status',
      icon: 'people',
      color: 'success',
      excelPath: '/reports/tenants/excel',
      pdfPath: '/reports/tenants/pdf',
    },
    {
      title: 'Occupancy Report',
      description: 'Property-by-property occupancy status and rent amounts',
      icon: 'apartment',
      color: 'accent',
      excelPath: '/reports/occupancy/excel',
      pdfPath: '/reports/occupancy/pdf',
    },
    {
      title: 'Maintenance Report',
      description: 'All maintenance requests with status and resolution notes',
      icon: 'construction',
      color: 'warning',
      excelPath: '/reports/maintenance/excel',
      pdfPath: '/reports/maintenance/pdf',
    },
  ];

  constructor(
    private http: HttpClient,
    private notification: NotificationService,
  ) {}

  /**
   * download — fetches the report as a Blob and triggers
   * browser download using a temporary anchor element.
   *
   * Same pattern as agreement download in Phase 11.
   * The AuthInterceptor attaches the JWT automatically.
   */
  download(report: ReportCard, format: 'excel' | 'pdf'): void {
    const key = report.title + format;
    const path = format === 'excel' ? report.excelPath : report.pdfPath;
    const ext = format === 'excel' ? 'xlsx' : 'pdf';

    // Revenue report needs the year parameter
    const isRevenue = path.includes('revenue');
    const url =
      `${environment.apiUrl}${path}` +
      (isRevenue ? `?year=${this.selectedYear}` : '');

    this.downloadingKey = key;
    this.notification.info(`Generating ${report.title}...`);

    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const filename =
          report.title.toLowerCase().replace(/ /g, '-') +
          '-' +
          new Date().toISOString().slice(0, 10) +
          '.' +
          ext;

        const objectUrl = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = objectUrl;
        anchor.download = filename;
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        URL.revokeObjectURL(objectUrl);

        this.downloadingKey = null;
        this.notification.success(`${report.title} downloaded`);
      },
      error: () => {
        this.downloadingKey = null;
        this.notification.error(`Failed to generate ${report.title}`);
      },
    });
  }

  isDownloading(report: ReportCard, format: 'excel' | 'pdf'): boolean {
    return this.downloadingKey === report.title + format;
  }
}
