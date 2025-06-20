import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { environment } from '../../../environments/environment';


@Component({
  selector: 'app-beckman-audit',
  standalone: true,
  imports: [
    CommonModule,  // For *ngIf, *ngFor, | date pipe
    FormsModule    // For [(ngModel)]
  ],
  templateUrl: './beckman-audit.component.html',
  styleUrl: './beckman-audit.component.css'
})

export class BeckmanAuditComponent {
  fromDate: string = '';
  toDate: string = '';
  errorMessage: string = '';
  storedReports: any[] = [];
  showPdfModal: boolean = false;
  selectedReportId: number | null = null;
  pdfUrl: SafeResourceUrl | null = null;

  private apiBaseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient, private sanitizer: DomSanitizer) {}

  ngOnInit(): void {
    this.loadStoredReports(); 
  }

  downloadAuditReport() {
    this.errorMessage = '';

    if (!this.isValidDateRange()) {
      this.errorMessage = 'Please select both From Date and To Date.';
      return;
    }

    if (!this.isToDateAfterFromDate()) {
      this.errorMessage = 'To Date must be after From Date.';
      return;
    }

    const formattedFrom = this.formatDateForSQL(this.fromDate);
    const formattedTo = this.formatDateForSQL(this.toDate);
    const downloadUrl = `${this.apiBaseUrl}/audit-report/download?startDate=${encodeURIComponent(formattedFrom)}&endDate=${encodeURIComponent(formattedTo)}`;
    window.open(downloadUrl, '_blank');
  }

  formatDateForSQL(date: string): string {
    return date.replace('T', ' ') + ':00';
  }

  isValidDateRange(): boolean {
    return this.fromDate !== '' && this.toDate !== '';
  }

  isToDateAfterFromDate(): boolean {
    return new Date(this.toDate) > new Date(this.fromDate);
  }

  validateYear(dateString: string, field: 'from' | 'to') {
    const date = new Date(dateString);
    const year = date.getFullYear();

    if (year < 1000 || year > 9999) {
      alert(`${field === 'from' ? 'From Date' : 'To Date'} must have a 4-digit year.`);
      if (field === 'from') this.fromDate = '';
      if (field === 'to') this.toDate = '';
    }
  }
  // 👇 New: Load Stored Reports for table
  loadStoredReports() {
    this.http.get<any[]>(`${this.apiBaseUrl}/stored-audit-report/list`)
      .subscribe(
        (data) => {
          this.storedReports = data;
        },
        (error) => {
          console.error('Failed to fetch stored audit reports', error);
        }
      );
  }
  // 👇 New: View Report by ID
  canReview(reportId: number | null): boolean {
    if (reportId === null) return false;
  
    const report = this.storedReports.find(r => r.id === reportId);
    const username = localStorage.getItem('username');
  
    // Only allow review if not already reviewed and user is not the generator
    return report && !report.reviewedBy && report.generatedBy !== username;
  }
  
  shouldShowReviewButton(reportId: number | null): boolean {
    if (reportId === null) return false;
  
    const report = this.storedReports.find(r => r.id === reportId);
    const username = localStorage.getItem('username');
  
    // Show the button only if report is not yet reviewed and user is not the generator
    return report && !report.reviewedBy && report.generatedBy !== username;
  }
  
  viewReport(id: number) {
    const viewUrl = `${this.apiBaseUrl}/audit-report/view/${id}`;
    // this.http.get(viewUrl, { responseType: 'blob' }).subscribe({
    //   next: (blob) => {
    //     const url = window.URL.createObjectURL(blob);
    //     this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    //     this.selectedReportId = id;
    //     this.showPdfModal = true;
    //   },
    //   error: (err) => {
    //     console.error('Failed to load PDF', err);
    //     alert('Failed to load the report. Please try again.');
    //   }
    // });
    
    this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(viewUrl);
    this.showPdfModal = true;
      
  }

  closePdf() {
    if (this.pdfUrl) {
      window.URL.revokeObjectURL(this.pdfUrl.toString());
    }
    this.pdfUrl = null;
    this.selectedReportId = null;
    this.showPdfModal = false;
  }

  reviewReport(reportId: number) {
    if (reportId === null) return;

    const report = this.storedReports.find(r => r.id === reportId);
    if (!report) return;

    const reviewUrl = `${this.apiBaseUrl}/audit-report/review/${reportId}`;
    this.http.post(reviewUrl, { reviewedBy: localStorage.getItem('username') }).subscribe({
      next: () => {
        alert('Report reviewed successfully');
        this.closePdf();
        this.loadStoredReports();
      },
      error: (err) => {
        console.error('Failed to review report', err);
        alert('Failed to review the report. Please try again.');
      }
    });
  }
}