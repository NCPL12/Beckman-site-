import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-log-report',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './log-report.component.html',
  styleUrls: ['./log-report.component.css'],
  providers: [DatePipe]
})
export class LogReportComponent implements OnInit {
  private apiBaseUrl = environment.apiBaseUrl;
  logReports: any[] = [];
  fromDate: string = '';
  toDate: string = '';
  username = localStorage.getItem('username');

  constructor(private http: HttpClient, private datePipe: DatePipe) { }

  ngOnInit() {
    this.fetchLogReports();
  }

  fetchLogReports() {
    this.http.get<any[]>(`${this.apiBaseUrl}/log-reports`).subscribe(
      (data) => {
        this.logReports = data;
      },
      (error) => {
        console.error('Error fetching log reports', error);
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


  downloadLogReport() {
    if (this.isValidDateRange()) {
      const logPayload = {
        username: this.username,
        fromDate: this.fromDate,
        toDate: this.toDate,
      };

      // Log the download operation
      this.http.post(`${this.apiBaseUrl}/log-download-log-report`, logPayload, { responseType: 'text' })
        .subscribe(() => {
          // Proceed with downloading the report
          const url = `${this.apiBaseUrl}/download-log-report?fromDate=${this.fromDate}&toDate=${this.toDate}`;
          window.open(url, '_blank');
        }, error => {
          console.error('Error logging download operation', error);
          alert('Failed to log the download operation. Please try again.');
        });
    } else {
      alert('Both From Date and To Date are required.');
    }
  }

  isValidDateRange(): boolean {
    return this.fromDate !== '' && this.toDate !== '';
  }
}
