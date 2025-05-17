import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UserDTO {
  id: number;
  username: string;
  password: string; 
  role: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  private apiBaseUrl = environment.apiBaseUrl;
  username = '';
  password = '';
  errorMessage = '';

  constructor(private router: Router, private http: HttpClient) {}

  login() {
    const loginUrl = `${this.apiBaseUrl}/login`;
    const payload = { username: this.username, password: this.password };
  
    this.http.post<UserDTO>(loginUrl, payload).subscribe(
      (userDTO) => {
        localStorage.setItem('username', userDTO.username);
        localStorage.setItem('role', userDTO.role);
        const logReportUrl = `${this.apiBaseUrl}/log-login`;
        const auditReportUrl = `${this.apiBaseUrl}/audit-login`;
        const logPayload = { username: userDTO.username };
        const logReport$ = this.http.post(logReportUrl, logPayload);
        const auditReport$ = this.http.post(auditReportUrl, logPayload);
  
        forkJoin([logReport$, auditReport$]).subscribe(
          () => {
            if (userDTO.role === 'admin') {
              // this.router.navigate(['/create']);
              this.router.navigate(['/list']);
            } else if (userDTO.role === 'supervisor') {
              this.router.navigate(['/reports']);
            } else if (userDTO.role === 'operator') {
              this.router.navigate(['/export']);
            }
          },
          (error) => {
            console.error('Error occurred during logging:', error);
          }
        );
      },
      (error) => {
        this.errorMessage = 'Invalid username or password';
        console.error('Error occurred during login:', error);
      }
    );
  }
  
}