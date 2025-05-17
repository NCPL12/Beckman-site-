import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Subscription } from 'rxjs';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'header',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit, OnDestroy {
  private apiBaseUrl = environment.apiBaseUrl;
  username: string | null = null;
  role: string | null = null;
  showConfirmLogout = false;
  inactivityTimeout: any;
  inactivityTimeLimit = 1800000; 
  activityEvents: string[] = ['click', 'mousemove', 'keydown'];
  subscriptions: Subscription[] = [];
  isLoggingOut = false;  

  constructor(private router: Router, private http: HttpClient) {
    this.username = localStorage.getItem('username');
    this.role = localStorage.getItem('role');
  }

  ngOnInit() {
    this.username = localStorage.getItem('username'); 
    if (this.username) {
      this.resetInactivityTimer();
      this.trackUserActivity();
    } else {
      this.clearInactivityTimer(); 
    }
  }

  ngOnDestroy() {
    this.clearInactivityTimer();
    this.unsubscribeFromUserActivity();
  }

  isLoggedIn(): boolean {
    return !!this.username;
  }

  confirmLogout() {
    this.showConfirmLogout = true;
  }

  handleLogoutConfirm(confirmed: boolean) {
    this.showConfirmLogout = false;
    if (confirmed && this.username) {
      this.logLogout();
    }
  }

  logout() {
    this.clearInactivityTimer();  
    this.unsubscribeFromUserActivity(); 
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    this.username = null; 
    this.router.navigate(['/login']);
  }

  unsubscribeFromUserActivity() {
    this.activityEvents.forEach(event => {
      document.removeEventListener(event, this.resetInactivityTimer.bind(this));
    });
  }

  
  resetInactivityTimer() {
    if (!this.isLoggedIn() || this.isLoggingOut) {
      return; 
    }

    this.clearInactivityTimer(); 
    this.inactivityTimeout = setTimeout(() => this.handleAutoLogout(), this.inactivityTimeLimit);
  }

  trackUserActivity() {
    if (this.isLoggedIn()) { 
      this.activityEvents.forEach(event => {
        document.addEventListener(event, this.resetInactivityTimer.bind(this));
      });
    }
  }
  
  clearInactivityTimer() {
    if (this.inactivityTimeout) {
      clearTimeout(this.inactivityTimeout);
      this.inactivityTimeout = null; 
    }
  }

  handleAutoLogout() {
    if (!this.isLoggedIn() || this.isLoggingOut) {
      return; 
    }

    this.isLoggingOut = true; 
    this.clearInactivityTimer(); 

    const logReportUrl = `${this.apiBaseUrl}/log-auto-logout`;
    const auditReportUrl = `${this.apiBaseUrl}/audit-auto-logout`;
    const payload = { username: this.username };
  
    const logReport$ = this.http.post(logReportUrl, payload);
    const auditReport$ = this.http.post(auditReportUrl, payload);
  
    forkJoin([logReport$, auditReport$]).subscribe(
      () => {
        localStorage.removeItem('username');
        localStorage.removeItem('role');
        this.unsubscribeFromUserActivity();
        this.username = null;

        this.router.navigate(['/login']).then(() => {
          setTimeout(() => {
            alert('You have been automatically logged out due to inactivity.');
            this.isLoggingOut = false; 
          }, 100);
        });
      },
      (error) => {
        console.error('Error occurred during logout:', error);
      }
    );
  }

  logLogout() {
    const logReportUrl = `${this.apiBaseUrl}/log-logout`;
    const auditReportUrl = `${this.apiBaseUrl}/audit-logout`;
    const payload = { username: this.username };
    
    const logReport$ = this.http.post(logReportUrl, payload);
    const auditReport$ = this.http.post(auditReportUrl, payload);

    forkJoin([logReport$, auditReport$]).subscribe(
      () => {
        localStorage.removeItem('username');
        localStorage.removeItem('role');
        this.unsubscribeFromUserActivity(); 
        this.username = null; 
        this.router.navigate(['/login']);
      },
      (error) => {
        console.error('Error occurred during logout:', error);
      }
    );
  }
}