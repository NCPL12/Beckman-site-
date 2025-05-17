import { Component, OnInit  } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SideBarComponent } from './components/side-bar/side-bar.component';
import { HeaderComponent } from './components/header/header.component';
import { Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterModule,  
    SideBarComponent,
    HeaderComponent,
    CommonModule
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent implements OnInit {
  showHeaderAndSidebar = false;

  constructor(private router: Router) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.updateHeaderAndSidebarVisibility(event.url);
      }
    });
  }

  ngOnInit() {
    this.updateHeaderAndSidebarVisibility(this.router.url);
  }

  updateHeaderAndSidebarVisibility(url: string) {
    const loggedIn = !!localStorage.getItem('role');
    this.showHeaderAndSidebar = loggedIn && url !== '/login';
  }
}
