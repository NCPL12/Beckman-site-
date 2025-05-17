import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
//import { CanvasJSAngularChartsModule } from '@canvasjs/angular-charts';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  private apiBaseUrl = environment.apiBaseUrl;
  approvedCount: number = 0;
  pendingCount: number = 0;
  approvedPercentage: number = 0;
  selectedReportType: string = 'manual';

  constructor(private http: HttpClient) {}

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
      default:
        console.error('Invalid report type selected');
        return;
    }

    this.http.get<any[]>(apiUrl)
      .subscribe(data => {
        this.calculateCounts(data);
      }, error => {
        console.error('Error fetching reports:', error);
      });
  }

  private calculateCounts(reports: any[]) {
    this.approvedCount = reports.filter(report => report.isApproved).length;
    this.pendingCount = reports.length - this.approvedCount;
    this.approvedPercentage = Math.round(
      (this.approvedCount / (this.approvedCount + this.pendingCount)) * 100
    );
  }

  onReportTypeChange() {
    this.loadReports();
  }
}

