import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../environments/environment';

interface TemplateData {
  id: number;
  name: string;
  report_group: string;
  additionalInfo: string;
  parameters: string[];
  selected?: boolean;
}

interface User {
  id: number;
  username: string;
  role: string;
}

@Component({
  selector: 'export-report',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './export-report.component.html',
  styleUrls: ['./export-report.component.css'],
})
export class ExportReportComponent implements OnInit {
  private apiBaseUrl = environment.apiBaseUrl;
  reports: TemplateData[] = [];
  scheduledReportIds: number[] = [];
  weeklyScheduledReportIds: number[] = [];
  monthlyScheduledReportIds: number[] = [];
  users: User[] = [];
  selectedTemplate: any = null;
  fromDate: string = '';
  toDate: string = '';
  exportType: string = 'manual';
  scheduleFrequency: string = '';
  predefinedReport: string = '';
  assignedTo: string = '';
  assignedApprover: string = '';
  isApproverRequired: boolean = false;
  scheduledBy: string = '';
  dailyTime: number | null = null;
  hoursList: number[] = [];
  daysList: number[] = [];
  dateError: string = '';
  weeklyDay: string = '';
  weeklyTime: number | null = null;
  monthlyDay: number | null = null;
  monthlyTime: number | null = null;

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    this.loadTemplates();
    this.loadUsers();
    this.loadScheduledReports();
    this.loadWeeklyScheduledReports();
    this.loadMonthlyScheduledReports();
    this.hoursList = Array.from({ length: 24 }, (_, i) => i);
    this.daysList = Array.from({ length: 31 }, (_, i) => i + 1);
  }

  loadTemplates(): void {
    this.http
      .get<TemplateData[]>(`${this.apiBaseUrl}/templates`, {
        responseType: 'json',
      })
      .subscribe(
        (response: TemplateData[]) => {
          this.reports = response;
        },
        (error) => {
          console.error('Error fetching templates', error);
        }
      );
  }
  validateDateRange(): boolean {
    this.dateError = '';
  
    if (this.exportType === 'manual' && this.fromDate && this.toDate) {
      const from = new Date(this.fromDate);
      const to = new Date(this.toDate);
  
      if (to <= from) {
        this.dateError = 'To Date must be after From Date.';
        return false;
      }
    }
  
    return true;
  }
    

  loadUsers(): void {
    this.http.get<User[]>(`${this.apiBaseUrl}/users`)
      .subscribe(users => {
        this.users = users;
      }, error => {
        console.error('Error fetching users:', error);
      });
  }

  // Load all scheduled reports once
  loadScheduledReports(): void {
    this.http.get<number[]>(`${this.apiBaseUrl}/get-all-daily-scheduled-reports`)
      .subscribe((scheduledReportIds: number[]) => {
        this.scheduledReportIds = scheduledReportIds;
      }, error => {
        console.error('Error fetching scheduled reports', error);
      });
  }

  loadWeeklyScheduledReports(): void {
    this.http.get<number[]>(`${this.apiBaseUrl}/get-all-weekly-scheduled-reports`)
      .subscribe((weeklyScheduledReportIds: number[]) => {
        this.weeklyScheduledReportIds = weeklyScheduledReportIds;
      }, error => {
        console.error('Error fetching weekly scheduled reports', error);
      });
  }

  loadMonthlyScheduledReports(): void {
    this.http.get<number[]>(`${this.apiBaseUrl}/get-all-monthly-scheduled-reports`)
      .subscribe((monthlyScheduledReportIds: number[]) => {
        this.monthlyScheduledReportIds = monthlyScheduledReportIds;
      }, error => {
        console.error('Error fetching monthly scheduled reports', error);
      });
  }


  onExportTypeChange(): void {
    this.fromDate = '';
    this.toDate = '';
    this.scheduleFrequency = '';
    this.predefinedReport = '';
  }

  onPredefinedReportChange(): void {
    const currentDate = new Date();

    if (this.predefinedReport === 'yesterday') {
      const yesterday = new Date();
      yesterday.setDate(currentDate.getDate() - 1);
      this.fromDate = `${yesterday.toISOString().split('T')[0]}T00:00`;
      this.toDate = `${yesterday.toISOString().split('T')[0]}T23:59`;
    } else if (this.predefinedReport === 'oneWeek') {
      const lastWeek = new Date();
      lastWeek.setDate(currentDate.getDate() - 7);
      this.fromDate = `${lastWeek.toISOString().split('T')[0]}T00:00`;
      this.toDate = `${currentDate.toISOString().split('T')[0]}T23:59`;
    } else if (this.predefinedReport === 'oneMonth') {
      const lastMonth = new Date();
      lastMonth.setMonth(currentDate.getMonth() - 1);
      this.fromDate = `${lastMonth.toISOString().split('T')[0]}T00:00`;
      this.toDate = `${currentDate.toISOString().split('T')[0]}T23:59`;
    }
  }

public submit(): void {
  if (this.selectedTemplate) {
    // ✅ Validate Date Range
    if (!this.validateDateRange()) {
      return;
    }

    // ❌ Don't block if `assignedTo` is empty
    if (this.isApproverRequired) {
      if (!this.assignedApprover) {
        alert('Please assign an approver.');
        return;
      }

      if (this.assignedTo === this.assignedApprover) {
        alert('Both approver and reviewer cannot be the same.');
        return;
      }
    }

    const approverName = this.isApproverRequired ? this.assignedApprover : null;

    if (this.exportType === 'manual') {
      // Pass null for assignedTo if not set
      this.exportReport(approverName);
    } else if (this.exportType === 'schedule') {
      if (!this.assignedTo) {
        alert('Please assign the report to a reviewer for scheduling.');
        return;
      }

      if (this.scheduleFrequency === 'daily') {
        this.scheduleDailyReport();
      } else if (this.scheduleFrequency === 'weekly') {
        this.scheduleWeeklyReport();
      } else if (this.scheduleFrequency === 'monthly') {
        this.scheduleMonthlyReport();
      }
    }
  } else {
    alert('Please select a template.');
  }
}

  
  // private exportReport(approverName: string | null): void {
  //   const username = localStorage.getItem('username');
  //   if (this.fromDate && this.toDate && this.assignedTo && username) {
  //     const formattedFromDate = this.formatDate(this.fromDate);
  //     const formattedToDate = this.formatDate(this.toDate);

  //     const logPayload = {
  //       username: username,
  //       reportId: this.selectedTemplate.id // Include report ID here
  //     };

  //     console.log('Logging report generation:', logPayload);

  //     // Log the report generation
  //     this.http.post(`${this.apiBaseUrl}/log-generated-report`, logPayload)
  //       .subscribe(() => {
  //         const url = `${this.apiBaseUrl}/exportReport?id=${this.selectedTemplate.id}&fromDate=${formattedFromDate}&toDate=${formattedToDate}&username=${username}&assignedTo=${this.assignedTo}&assigned_approver=${approverName}`;
  //         window.open(url, '_blank');
  //       }, error => {
  //         console.error('Error logging report generation', error);
  //         alert('Failed to log the report generation. Please try again.');
  //       });
  //   } else {
  //     alert('Please select a date range and assign the report.');
  //   }
  // }

  // private scheduleDailyReport(): void {
  //   const username = localStorage.getItem('username');
  //   if (this.dailyTime && this.selectedTemplate) {
  //     if (this.scheduledReportIds.includes(this.selectedTemplate.id)) {
  //       alert('This report is already scheduled for daily execution.');
  //       return;
  //     }

  //     const scheduleDetails = {
  //       id: this.selectedTemplate.id,
  //       name: this.selectedTemplate.name,
  //       assignedApprover: this.assignedApprover,
  //       assignedReview: this.assignedTo,
  //       isApproverRequired: this.isApproverRequired,
  //       scheduledBy: username,
  //       dailyTime: this.dailyTime
  //     };

  //     // Log the daily report scheduling
  //     this.http.post(`${this.apiBaseUrl}/log-scheduled-daily-report`, {
  //       username: username,
  //       reportType: 'daily',
  //       templateId: this.selectedTemplate.id
  //     }).subscribe(() => {
  //       console.log('Daily report scheduling log created successfully.');
  //     }, error => {
  //       console.error('Error logging daily report scheduling', error);
  //     });

  //     // Schedule the daily report
  //     this.http.post(`${this.apiBaseUrl}/schedule-report-daily`, scheduleDetails)
  //       .subscribe(() => {
  //         alert('Daily report scheduled successfully.');
  //         this.scheduledReportIds.push(this.selectedTemplate.id);
  //       }, error => {
  //         console.error('Error scheduling daily report', error);
  //       });
  //   } else {
  //     alert('Please select a time for the daily report and ensure all fields are filled.');
  //   }
  // }

  // private scheduleWeeklyReport(): void {
  //   const username = localStorage.getItem('username');
  //   if (this.weeklyTime && this.weeklyDay && this.selectedTemplate) {
  //     if (this.weeklyScheduledReportIds.includes(this.selectedTemplate.id)) {
  //       alert('This report is already scheduled for weekly execution.');
  //       return;
  //     }

  //     const scheduleDetails = {
  //       id: this.selectedTemplate.id,
  //       name: this.selectedTemplate.name,
  //       assignedApprover: this.assignedApprover,
  //       assignedReview: this.assignedTo,
  //       isApproverRequired: this.isApproverRequired,
  //       scheduledBy: username,
  //       weeklyTime: this.weeklyTime,
  //       weeklyDay: this.weeklyDay
  //     };

  //     // Log the weekly report scheduling
  //     this.http.post(`${this.apiBaseUrl}/log-scheduled-weekly-report`, {
  //       username: username,
  //       reportType: 'weekly',
  //       templateId: this.selectedTemplate.id
  //     }).subscribe(() => {
  //       console.log('Weekly report scheduling log created successfully.');
  //     }, error => {
  //       console.error('Error logging weekly report scheduling', error);
  //     });

  //     // Schedule the weekly report
  //     this.http.post(`${this.apiBaseUrl}/schedule-report-weekly`, scheduleDetails)
  //       .subscribe(() => {
  //         alert('Weekly report scheduled successfully.');
  //         this.weeklyScheduledReportIds.push(this.selectedTemplate.id);
  //       }, error => {
  //         console.error('Error scheduling weekly report', error);
  //       });
  //   } else {
  //     alert('Please select a time and day for the weekly report and ensure all fields are filled.');
  //   }
  // }

  // private scheduleMonthlyReport(): void {
  //   const username = localStorage.getItem('username');
  //   if (this.monthlyTime && this.monthlyDay && this.selectedTemplate) {
  //     if (this.monthlyScheduledReportIds.includes(this.selectedTemplate.id)) {
  //       alert('This report is already scheduled for monthly execution.');
  //       return;
  //     }

  //     const scheduleDetails = {
  //       id: this.selectedTemplate.id,
  //       name: this.selectedTemplate.name,
  //       assignedApprover: this.assignedApprover,
  //       assignedReview: this.assignedTo,
  //       isApproverRequired: this.isApproverRequired,
  //       scheduledBy: username,
  //       monthlyTime: this.monthlyTime,
  //       monthlyDay: this.monthlyDay
  //     };

  //     // Log the monthly report scheduling
  //     this.http.post('${this.apiBaseUrl}/log-scheduled-monthly-report', {
  //       username: username,
  //       reportType: 'monthly',
  //       templateId: this.selectedTemplate.id
  //     }).subscribe(() => {
  //       console.log('Monthly report scheduling log created successfully.');
  //     }, error => {
  //       console.error('Error logging monthly report scheduling', error);
  //     });

  //     // Schedule the monthly report
  //     this.http.post('${this.apiBaseUrl}/schedule-report-monthly', scheduleDetails)
  //       .subscribe(() => {
  //         alert('Monthly report scheduled successfully.');
  //         this.monthlyScheduledReportIds.push(this.selectedTemplate.id);
  //       }, error => {
  //         console.error('Error scheduling monthly report', error);
  //       });
  //   } else {
  //     alert('Please select a time and day for the monthly report and ensure all fields are filled.');
  //   }
  // }

private exportReport(approverName: string | null): void {
  const username = localStorage.getItem('role');
  if (this.fromDate && this.toDate && username && this.selectedTemplate) {
    const formattedFromDate = this.formatDate(this.fromDate);
    const formattedToDate = this.formatDate(this.toDate);

    const logPayload = {
      username: username,
      reportId: this.selectedTemplate.id,
      reportName: this.selectedTemplate.name
    };

    // ✅ Step 1: Open blank tab
    const newTab = window.open('', '_blank');
    if (newTab) {
      newTab.document.write(`
        <!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>Generating Report</title>
  <style>
    body {
      font-family: Arial, sans-serif;
      background-color: #f9f9f9;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100vh;
      margin: 0;
      color: #444;
    }

    .loader {
      border: 8px solid #eee;
      border-top: 8px solid #3f51b5;
      border-radius: 50%;
      width: 60px;
      height: 60px;
      animation: spin 1s linear infinite;
      margin-bottom: 20px;
    }

    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }

    h2 {
      font-weight: normal;
    }

    p {
      color: #666;
      font-size: 14px;
    }
  </style>
</head>
<body>
  <div class="loader"></div>
  <h2>Generating Report...</h2>
  <p>Please wait while your PDF is being prepared.</p>
</body>
</html>
      `);
    }

    // ✅ Step 2: Log the generation and fetch PDF
    this.http.post(`${this.apiBaseUrl}/log-generated-report`, logPayload).subscribe(() => {
      const url = `${this.apiBaseUrl}/exportReport?id=${this.selectedTemplate.id}&fromDate=${formattedFromDate}&toDate=${formattedToDate}&username=${username}&assignedTo=${this.assignedTo || ''}&assigned_approver=${approverName || ''}`;

      this.http.get(url, { responseType: 'blob' }).subscribe(blob => {
        const blobUrl = URL.createObjectURL(blob);

        if (newTab) {
          // ✅ Use iframe with onload event to ensure PDF is rendered before closing
          newTab.document.body.innerHTML = `
            <iframe id="pdfFrame" src="${blobUrl}" style="width:100vw;height:100vh;border:none;" onload="setTimeout(() => window.close(), 5000)"></iframe>
          `;
        }
      }, error => {
        if (newTab) {
          newTab.document.body.innerHTML = '<p style="color: red;">❌ Failed to generate PDF. Please try again.</p>';
        }
      });

    }, error => {
      alert('Failed to log report generation');
      if (newTab) {
        newTab.close();
      }
    });
  } else {
    alert('Please select a valid date range and template.');
  }
}



  private scheduleDailyReport(): void {
    const username = localStorage.getItem('username');
    if (this.dailyTime && this.selectedTemplate) {
      if (this.scheduledReportIds.includes(this.selectedTemplate.id)) {
        alert('This report is already scheduled for daily execution.');
        return;
      }

      const scheduleDetails = {
        id: this.selectedTemplate.id,
        name: this.selectedTemplate.name,
        assignedApprover: this.assignedApprover,
        assignedReview: this.assignedTo,
        isApproverRequired: this.isApproverRequired,
        scheduledBy: username,
        dailyTime: this.dailyTime
      };

      // Log the daily report scheduling
      this.http.post(`${this.apiBaseUrl}/log-scheduled-daily-report`, {
        username: username,
        reportType: 'daily',
        templateId: this.selectedTemplate.id,
        templateName: this.selectedTemplate.name
      }).subscribe(() => {
        console.log('Daily report scheduling log created successfully.');
      }, error => {
        console.error('Error logging daily report scheduling', error);
      });

      // Schedule the daily report
      this.http.post(`${this.apiBaseUrl}/schedule-report-daily`, scheduleDetails)
        .subscribe(() => {
          alert('Daily report scheduled successfully.');
          this.scheduledReportIds.push(this.selectedTemplate.id);
        }, error => {
          console.error('Error scheduling daily report', error);
        });
    } else {
      alert('Please select a time for the daily report and ensure all fields are filled.');
    }
  }

  private scheduleWeeklyReport(): void {
    const username = localStorage.getItem('username');
    if (this.weeklyTime && this.weeklyDay && this.selectedTemplate) {
      if (this.weeklyScheduledReportIds.includes(this.selectedTemplate.id)) {
        alert('This report is already scheduled for weekly execution.');
        return;
      }

      const scheduleDetails = {
        id: this.selectedTemplate.id,
        name: this.selectedTemplate.name,
        assignedApprover: this.assignedApprover,
        assignedReview: this.assignedTo,
        isApproverRequired: this.isApproverRequired,
        scheduledBy: username,
        weeklyTime: this.weeklyTime,
        weeklyDay: this.weeklyDay
      };

      // Log the weekly report scheduling
      this.http.post(`${this.apiBaseUrl}/log-scheduled-weekly-report`, {
        username: username,
        reportType: 'weekly',
        templateId: this.selectedTemplate.id,
        templateName: this.selectedTemplate.name
      }).subscribe(() => {
        console.log('Weekly report scheduling log created successfully.');
      }, error => {
        console.error('Error logging weekly report scheduling', error);
      });

      // Schedule the weekly report
      this.http.post(`${this.apiBaseUrl}/schedule-report-weekly`, scheduleDetails)
        .subscribe(() => {
          alert('Weekly report scheduled successfully.');
          this.weeklyScheduledReportIds.push(this.selectedTemplate.id);
        }, error => {
          console.error('Error scheduling weekly report', error);
        });
    } else {
      alert('Please select a time and day for the weekly report and ensure all fields are filled.');
    }
  }

  private scheduleMonthlyReport(): void {
    const username = localStorage.getItem('username');
    if (this.monthlyTime && this.monthlyDay && this.selectedTemplate) {
      if (this.monthlyScheduledReportIds.includes(this.selectedTemplate.id)) {
        alert('This report is already scheduled for monthly execution.');
        return;
      }

      const scheduleDetails = {
        id: this.selectedTemplate.id,
        name: this.selectedTemplate.name,
        assignedApprover: this.assignedApprover,
        assignedReview: this.assignedTo,
        isApproverRequired: this.isApproverRequired,
        scheduledBy: username,
        monthlyTime: this.monthlyTime,
        monthlyDay: this.monthlyDay
      };

      // Log the monthly report scheduling
      this.http.post(`${this.apiBaseUrl}/log-scheduled-monthly-report`, {
        username: username,
        reportType: 'monthly',
        templateId: this.selectedTemplate.id,
        templateName: this.selectedTemplate.name
      }).subscribe(() => {
        console.log('Monthly report scheduling log created successfully.');
      }, error => {
        console.error('Error logging monthly report scheduling', error);
      });

      // Schedule the monthly report
      this.http.post(`${this.apiBaseUrl}/schedule-report-monthly`, scheduleDetails)
        .subscribe(() => {
          alert('Monthly report scheduled successfully.');
          this.monthlyScheduledReportIds.push(this.selectedTemplate.id);
        }, error => {
          console.error('Error scheduling monthly report', error);
        });
    } else {
      alert('Please select a time and day for the monthly report and ensure all fields are filled.');
    }
  }

  private formatDate(dateString: string): string {
    const date = new Date(dateString);
    const month = ('0' + (date.getMonth() + 1)).slice(-2);
    const day = ('0' + date.getDate()).slice(-2);
    const year = date.getFullYear();
    const hours = ('0' + date.getHours()).slice(-2);
    const minutes = ('0' + date.getMinutes()).slice(-2);
    return `${month}/${day}/${year} ${hours}:${minutes}`;
  }
}