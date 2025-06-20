import { Routes } from '@angular/router';
import { CreateTemplateComponent } from './components/create-template/create-template.component';
import { ListTemplateComponent } from './components/list-template/list-template.component';
import { ExportReportComponent } from './components/export-report/export-report.component';
import { LoginComponent } from './components/login/login.component';
import { ReportsComponent } from './components/reports/reports.component';
import { AuthGuard } from './components/auth.guard';
// import { DashboardComponent } from './components/dashboard/dashboard.component';
// import { LogReportComponent } from './components/log-report/log-report.component';
import { AboutComponent } from './components/about/about.component';
import { AuditReportComponent } from './components/audit-report/audit-report.component';

import { AlarmReportComponent } from './components/alaram-report/alarm-report.component';
import { BeckmanAuditComponent } from './components/beckman-audit/beckman-audit.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'list', component: ListTemplateComponent, canActivate: [AuthGuard]  },
  { path: 'create', component: CreateTemplateComponent, canActivate: [AuthGuard]  },
  // { path: 'list', component: ListTemplateComponent, canActivate: [AuthGuard]  },
  { path: 'export', component: ExportReportComponent, canActivate: [AuthGuard]  },
  { path: 'reports', component: ReportsComponent, canActivate: [AuthGuard]  },
  // { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard]  },
  // { path: 'log-report', component: LogReportComponent, canActivate: [AuthGuard]  },
  { path: 'audit-report', component: AuditReportComponent, canActivate: [AuthGuard]  },
  { path: 'alarm-report', component: AlarmReportComponent, canActivate: [AuthGuard]  },
    { path: 'beckman-audit', component: BeckmanAuditComponent, canActivate: [AuthGuard]  },
  { path: 'about', component: AboutComponent, canActivate: [AuthGuard]  },
  { path: '', redirectTo: '/login', pathMatch: 'full' }, 
  { path: '**', redirectTo: '/login' }  
];
