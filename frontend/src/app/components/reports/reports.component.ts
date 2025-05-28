import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'reports',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.css'],
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule],
  providers: [DatePipe]
})
export class ReportsComponent implements OnInit {
  private apiBaseUrl = environment.apiBaseUrl;
  reports: any[] = [];
  showConfirmDialog = false;
  dialogMessage = '';
  currentReportId: number | null = null;
  actionType: 'approve' | 'delete' | 'review' | null = null;
  successMessage: string | null = null;
  showPdfModal = false;
  pdfUrl: SafeResourceUrl | null = null;
  tableShowChk: boolean = false;
  selectedReportType: 'manual' | 'daily' | 'weekly' | 'monthly' = 'manual';


  constructor(private http: HttpClient, private sanitizer: DomSanitizer, private datePipe: DatePipe) { }

  ngOnInit() {
    this.loadReports();
  }

  loadReports() {
    let apiUrl = '';
    switch (this.selectedReportType) {
      case 'manual':
        apiUrl = `${this.apiBaseUrl}/reports`;
        break;
      case 'daily':
        apiUrl = `${this.apiBaseUrl}/daily-reports`;
        break;
      case 'weekly':
        apiUrl = `${this.apiBaseUrl}/weekly-reports`;
        break;
      case 'monthly':
        apiUrl = `${this.apiBaseUrl}/monthly-reports`;
        break;
      // Add similar logic for weekly and monthly reports
    }

    this.http.get<any[]>(apiUrl).subscribe(
      data => {
        this.reports = data;
      },
      error => {
        console.error(`Error fetching ${this.selectedReportType} reports:`, error);
      }
    );
  }


  formatDate(timestampStr: string | null): string | null {
    if (timestampStr === null || timestampStr === '') {
      return null;
    }
    const timestamp = Number(timestampStr);
    if (isNaN(timestamp)) {
      return null;
    }
    return this.datePipe.transform(new Date(timestamp), 'yyyy-MM-dd HH:mm:ss');
  }

  viewReport(reportId: number) {
    this.currentReportId = reportId;

    let apiUrl = '';
    if (this.selectedReportType === 'manual') {
      apiUrl = `${this.apiBaseUrl}/reports/${reportId}`;
    } else if (this.selectedReportType === 'daily') {
      apiUrl = `${this.apiBaseUrl}/daily-reports/${reportId}`;
    } else if (this.selectedReportType === 'weekly') {
      apiUrl = `${this.apiBaseUrl}/weekly-reports/${reportId}`;
    }
    else if (this.selectedReportType === 'monthly') {
      apiUrl = `${this.apiBaseUrl}/monthly-reports/${reportId}`;
    }
    // Optionally add similar logic for weekly and monthly reports if necessary

    // this.http.get(apiUrl, { responseType: 'blob' }).subscribe(
    //   blob => {
    //     const url = window.URL.createObjectURL(blob);
    //     this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
    //     this.showPdfModal = true;
    //   },
    //   error => {
    //     console.error(`Error loading PDF for ${this.selectedReportType} report:`, error);
    //   }
    // );
    this.pdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(apiUrl);
this.showPdfModal = true;

  }

  closePdf() {
    this.showPdfModal = false;
    this.pdfUrl = null;
    this.currentReportId = null;
    this.reloadReports();
  }

  reloadReports() {
    // 
    this.loadReports();
  }

  confirmApprove(reportId: number) {
    const currentReport = this.reports.find(r => r.id === reportId);
    const username = localStorage.getItem('role');

    if (currentReport && currentReport.assignedApprover !== username) {
      alert('You are not assigned to approve this report.');
      return;
    }

    this.currentReportId = reportId;
    this.dialogMessage = 'Are you sure you want to approve this report?';
    this.actionType = 'approve';
    this.showConfirmDialog = true;
  }

  confirmDelete(reportId: number) {
    this.currentReportId = reportId;
    this.dialogMessage = 'Are you sure you want to delete this report?';
    this.actionType = 'delete';
    this.showConfirmDialog = true;
  }

  // handleConfirm(confirmed: boolean) {
  //   this.showConfirmDialog = false;
  //   if (confirmed && this.currentReportId) {
  //     if (this.actionType === 'approve') {
  //       this.approveReport(this.currentReportId);
  //     } else if (this.actionType === 'review') {
  //       this.reviewReport(this.currentReportId);
  //     } else if (this.actionType === 'delete') {
  //       // Implement delete logic here
  //     }
  //   }
  //   this.currentReportId = null;
  //   this.actionType = null;
  // }

  handleConfirm(confirmed: boolean) {
    this.showConfirmDialog = false;

    if (confirmed && this.currentReportId) {
        const currentReport = this.reports.find(report => report.id === this.currentReportId);

        if (!currentReport) {
            console.error('Report not found for the given ID');
            alert('Unable to find the selected report. Please try again.');
            return;
        }

        const reportName = currentReport.name;

        if (this.actionType === 'approve') {
            this.approveReport(this.currentReportId, reportName); // Pass both ID and name
        } else if (this.actionType === 'review') {
            this.reviewReport(this.currentReportId, reportName); // Pass both ID and name
        } else if (this.actionType === 'delete') {
             // Implement delete logic
        }
    }

    this.currentReportId = null;
    this.actionType = null;
}


  

  // reviewReport(reportId: number) {
  //   const username = localStorage.getItem('username');

  //   let apiUrl = '';
  //   let reportType = ''; // Add a variable to determine the report type
  //   if (this.selectedReportType === 'manual') {
  //     apiUrl = `${this.apiBaseUrl}/reports/review/${reportId}`;
  //     reportType = 'manual';
  //   } else if (this.selectedReportType === 'daily') {
  //     apiUrl = `${this.apiBaseUrl}/daily-reports/review/${reportId}`;
  //     reportType = 'daily';
  //   } else if (this.selectedReportType === 'weekly') {
  //     apiUrl = `${this.apiBaseUrl}/weekly-reports/review/${reportId}`;
  //     reportType = 'weekly';
  //   } else if (this.selectedReportType === 'monthly') {
  //     apiUrl = `${this.apiBaseUrl}/monthly-reports/review/${reportId}`;
  //     reportType = 'monthly';
  //   }

  //   if (!apiUrl || !reportType) {
  //     console.error('Invalid report type selected');
  //     alert('Failed to determine the report type. Please try again.');
  //     return;
  //   }

  //   // Log the review action
  //   this.http.post('${this.apiBaseUrl}/log-review-report', {
  //     username: username,
  //     reportType: reportType,
  //     reportId: reportId
  //   }).subscribe(() => {
  //     // Proceed to approve the report
  //     this.http.put(apiUrl, { username }).subscribe(() => {
  //       this.successMessage = 'Report reviewed successfully';
  //       setTimeout(() => this.successMessage = null, 3000);
  //       this.closePdf();
  //     }, error => {
  //       console.error('Error reviewing report:', error);
  //     });
  //   }, error => {
  //     console.error('Error logging report review:', error);
  //     alert('Failed to log the report review. Please try again.');
  //   });
  // }

  reviewReport(reportId: number, reportName: string) {
    const username = localStorage.getItem('username');

    let apiUrl = '';
    let reportType = ''; // Variable to determine the report type
    if (this.selectedReportType === 'manual') {
        apiUrl = `${this.apiBaseUrl}/reports/review/${reportId}`;
        reportType = 'manual';
    } else if (this.selectedReportType === 'daily') {
        apiUrl = `${this.apiBaseUrl}/daily-reports/review/${reportId}`;
        reportType = 'daily';
    } else if (this.selectedReportType === 'weekly') {
        apiUrl = `${this.apiBaseUrl}/weekly-reports/review/${reportId}`;
        reportType = 'weekly';
    } else if (this.selectedReportType === 'monthly') {
        apiUrl = `${this.apiBaseUrl}/monthly-reports/review/${reportId}`;
        reportType = 'monthly';
    }

    if (!apiUrl || !reportType) {
        console.error('Invalid report type selected');
        alert('Failed to determine the report type. Please try again.');
        return;
    }

    // Log the review action
    this.http.post(`${this.apiBaseUrl}/log-review-report`, {
        username: username,
        reportType: reportType,
        reportId: reportId,
        reportName: reportName // Include report name
    }).subscribe(() => {
        // Proceed to approve the report
        this.http.put(apiUrl, { username }).subscribe(() => {
            this.successMessage = 'Report reviewed successfully';
            setTimeout(() => this.successMessage = null, 3000);
            this.closePdf();
        }, error => {
            console.error('Error reviewing report:', error);
        });
    }, error => {
        console.error('Error logging report review:', error);
        alert('Failed to log the report review. Please try again.');
    });
}


  // approveReport(reportId: number) {
  //   const username = localStorage.getItem('username');

  //   let apiUrl = '';
  //   let reportType = ''; // Add a variable to determine the report type
  //   if (this.selectedReportType === 'manual') {
  //     apiUrl = `${this.apiBaseUrl}/reports/approve/${reportId}`;
  //     reportType = 'manual';
  //   } else if (this.selectedReportType === 'daily') {
  //     apiUrl = `${this.apiBaseUrl}/daily-reports/approve/${reportId}`;
  //     reportType = 'daily';
  //   } else if (this.selectedReportType === 'weekly') {
  //     apiUrl = `${this.apiBaseUrl}/weekly-reports/approve/${reportId}`;
  //     reportType = 'weekly';
  //   } else if (this.selectedReportType === 'monthly') {
  //     apiUrl = `${this.apiBaseUrl}/monthly-reports/approve/${reportId}`;
  //     reportType = 'monthly';
  //   }

  //   if (!apiUrl || !reportType) {
  //     console.error('Invalid report type selected');
  //     alert('Failed to determine the report type. Please try again.');
  //     return;
  //   }

  //   // Log the approval action
  //   this.http.post('${this.apiBaseUrl}/log-approved-report', {
  //     username: username,
  //     reportType: reportType,
  //     reportId: reportId
  //   }).subscribe(() => {
  //     // Proceed to approve the report
  //     this.http.put(apiUrl, { username }).subscribe(() => {
  //       this.successMessage = 'Report approved successfully';
  //       setTimeout(() => this.successMessage = null, 3000);
  //       this.closePdf();
  //     }, error => {
  //       console.error('Error approving report:', error);
  //     });
  //   }, error => {
  //     console.error('Error logging report approval:', error);
  //     alert('Failed to log the report approval. Please try again.');
  //   });
  // }

  approveReport(reportId: number, reportName: string) {
    const username = localStorage.getItem('username');

    let apiUrl = '';
    let reportType = ''; // Variable to determine the report type
    if (this.selectedReportType === 'manual') {
        apiUrl = `${this.apiBaseUrl}/reports/approve/${reportId}`;
        reportType = 'manual';
    } else if (this.selectedReportType === 'daily') {
        apiUrl = `${this.apiBaseUrl}/daily-reports/approve/${reportId}`;
        reportType = 'daily';
    } else if (this.selectedReportType === 'weekly') {
        apiUrl = `${this.apiBaseUrl}/weekly-reports/approve/${reportId}`;
        reportType = 'weekly';
    } else if (this.selectedReportType === 'monthly') {
        apiUrl = `${this.apiBaseUrl}/monthly-reports/approve/${reportId}`;
        reportType = 'monthly';
    }

    if (!apiUrl || !reportType) {
        console.error('Invalid report type selected');
        alert('Failed to determine the report type. Please try again.');
        return;
    }

    // Log the approval action with ID and Name
    this.http.post(`${this.apiBaseUrl}/log-approved-report`, {
        username: username,
        reportType: reportType,
        reportId: reportId,
        reportName: reportName // Pass the name of the report
    }).subscribe(() => {
        // Proceed to approve the report
        this.http.put(apiUrl, { username }).subscribe(() => {
            this.successMessage = `Report "${reportName}" approved successfully`; // Include the name in the success message
            setTimeout(() => this.successMessage = null, 3000);
            this.closePdf();
        }, error => {
            console.error('Error approving report:', error);
        });
    }, error => {
        console.error('Error logging report approval:', error);
        alert('Failed to log the report approval. Please try again.');
    });
}


  isCurrentReportApproved(): boolean {
    const currentReport = this.reports.find(r => r.id === this.currentReportId);
    return currentReport ? (currentReport.isApproved === true || currentReport.reviewedBy === null) : false;
  }

  isCurrentReportReviewed(): boolean {
    const currentReport = this.reports.find(r => r.id === this.currentReportId);
    return currentReport ? currentReport.reviewedBy !== null : false;
  }

  confirmReview(reportId: number) {
    const currentReport = this.reports.find(r => r.id === reportId);
    const username = localStorage.getItem('username');

    if (currentReport && currentReport.assignedReview !== username) {
      alert('You are not assigned to review this report.');
      return;
    }

    this.currentReportId = reportId;
    this.dialogMessage = 'Are you sure you want to review this report?';
    this.actionType = 'review';
    this.showConfirmDialog = true;
  }
}