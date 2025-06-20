import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule, formatDate } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-alarm-report',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './alarm-report.component.html',
  styleUrl: './alarm-report.component.css'
})
export class AlarmReportComponent {
  fromDate: string = '';
  toDate: string = '';
  errorMessage: string = '';
  storedReports: any[] = []; // List of stored alarm reports
  showPdfModal: boolean = false;
  selectedReportId: number | null = null;
  pdfUrl: SafeResourceUrl | null = null;

  private apiBaseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient, private sanitizer: DomSanitizer) {}

  ngOnInit() {
    this.fetchStoredReports(); // Load reports initially
  }
  
downloadAlarmReport() {
  this.errorMessage = '';
  const username = localStorage.getItem('username');
  if (!username) {
    alert('Username not found in localStorage. Please log in again.');
    return;
  }

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
  const downloadUrl = `${this.apiBaseUrl}/alarm-report/download?startDate=${encodeURIComponent(formattedFrom)}&endDate=${encodeURIComponent(formattedTo)}&username=${encodeURIComponent(username)}`;

  this.http.get(downloadUrl, { responseType: 'blob' }).subscribe({
    next: (blob) => {
      const fileURL = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = fileURL;
      a.download = `Alarm_Report_${new Date().toISOString().split('T')[0]}.pdf`;
      a.click();
      setTimeout(() => this.fetchStoredReports(), 2000);
    },
    error: (err) => {
      if (err.status === 500 || err.status === 400) {
        // Backend sends a string error message
        const reader = new FileReader();
        reader.onload = () => {
          this.errorMessage = reader.result?.toString() || "Failed to generate report. No data found for the selected time period";
        };
        reader.readAsText(err.error);
      } else {
        this.errorMessage = 'Something went wrong while generating the report.';
      }
    }
  });
}

  fetchStoredReports() {
    const listUrl = `${this.apiBaseUrl}/stored-alarm-report/list`;
    this.http.get<any[]>(listUrl).subscribe({
      next: (data) => {
        this.storedReports = data;
      },
      error: (err) => {
        console.error('Failed to fetch stored alarm reports', err);
      }
    });
  }
//   canReview(reportId: number | null): boolean {
//     if (reportId === null) return false;
  
//     const report = this.storedReports.find(r => r.id === reportId);
//     const username = localStorage.getItem('username');
  
//     // Only allow review if not already reviewed and user is not the generator
//     return report && !report.reviewedBy && report.generatedBy !== username;
//   }
  
  shouldShowReviewButton(reportId: number | null): boolean {
    if (reportId === null) return false;
  
    const report = this.storedReports.find(r => r.id === reportId);
    const username = localStorage.getItem('username');
  
    // Show the button only if report is not yet reviewed and user is not the generator
    return report && !report.reviewedBy && report.generatedBy !== username;
  }
  
  viewStoredReport(id: number) {
    const viewUrl = `${this.apiBaseUrl}/alarm-report/view/${id}`;
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
    this.selectedReportId = id; 
  }
//   reviewReport(reportId: number | null) {
//     if (reportId === null) {
//       alert('No report selected for review.');
//       return;
//     }
  
//     const username = localStorage.getItem('username');
//     if (!username) {
//       alert('Username not found in localStorage.');
//       return;
//     }
  
//     const url = `${this.apiBaseUrl}/alarm-report/review/${reportId}`;
//     this.http.put(url, { username: username }).subscribe({
//       next: () => {
//         alert('Report reviewed successfully.');
//         this.closePdf();
//         this.fetchStoredReports(); // refresh list
//       },
//       error: (err) => {
//         console.error('Failed to review report', err);
//         alert('Failed to review the report.');
//       }
//     });
//   }
  

  closePdf() {
    this.showPdfModal = false;
    this.pdfUrl = null;
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
    formatDateString(dateStr: string): string {
        if (!dateStr) return '';
        return formatDate(dateStr, 'dd-MM-yyyy HH:mm', 'en-IN');
      }
}