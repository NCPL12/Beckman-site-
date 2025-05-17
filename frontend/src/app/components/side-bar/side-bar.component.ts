import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'side-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './side-bar.component.html',
  styleUrls: ['./side-bar.component.css']
})
export class SideBarComponent implements OnInit {
  userRole: string | null = '';

  constructor(private router: Router, private activatedRoute: ActivatedRoute) {}

  ngOnInit() {
    this.userRole = localStorage.getItem('role');
  }

  navigateTo(route: string) {
    this.router.navigateByUrl(`/${route}`);
  }
  
  isActiveRoute(route: string): boolean {
    return this.router.url === `/${route}`;
  }
}

