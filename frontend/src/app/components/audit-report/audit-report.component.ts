import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule, formatDate } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-audit-report',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-report.component.html',
  styleUrl: './audit-report.component.css',
  providers: [DatePipe]
})
export class AuditReportComponent implements OnInit {
  auditReports: any[] = [];
  fromDate: string = '';
  toDate: string = '';
  private apiBaseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient, private datePipe: DatePipe) { }

  ngOnInit() {
    this.fetchAuditReports();
  }

  fetchAuditReports() {
    this.http.get<any[]>(`${this.apiBaseUrl}/audit-reports`).subscribe(
      (data) => {
        this.auditReports = data;
      },
      (error) => {
        console.error('Error fetching audit reports', error);
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

    return this.datePipe.transform(new Date(timestamp), 'dd-MM-yyyy HH:mm:ss');
  }


  downloadAuditReport() {
    if (this.isValidDateRange()) {
      const logUrl = `${this.apiBaseUrl}/log-download-audit-report`;
      const downloadUrl = `${this.apiBaseUrl}/download-audit-report?fromDate=${this.fromDate}&toDate=${this.toDate}`;
      const payload = {
        username: localStorage.getItem('username'),
        fromDate: this.fromDate,
        toDate: this.toDate,
      };
      console.log("Log request payload:", payload);
      this.http.post(logUrl, payload, { responseType: 'text' }).subscribe(
        () => {
          window.open(downloadUrl, '_blank');
        },
        (error) => {
          console.error('Error logging download action:', error);
          alert(`Failed to log the download action. Please check your connection or try again.`);
        }
      );
    } else {
      alert('Both From Date and To Date are required.');
    }
  }



  isValidDateRange(): boolean {
    return this.fromDate !== '' && this.toDate !== '';
  }
   formatDateString(dateStr: string): string {
      if (!dateStr) return '';
      return formatDate(dateStr, 'dd-MM-yyyy HH:mm', 'en-IN');
    }
}