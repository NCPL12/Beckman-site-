import { Component, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { CommonModule } from '@angular/common'; // <-- Add this import

@Component({
  selector: 'about',
  standalone: true,
  imports: [CommonModule], // <-- Add CommonModule here
  templateUrl: './about.component.html',
  styleUrl: './about.component.css'
})
export class AboutComponent implements OnInit {
  aboutHeading: string = 'About Us';
  aboutDescription1: string = `Neptune Controls Pvt Ltd is proud to present Version 1.0 of our state-of-the-art 
  application. This system is specifically designed to help industrial units monitor and manage key factors 
  like temperature and humidity, providing detailed reports and graphical insights.`;

  aboutDescription2: string = `By offering real-time data and analytics, our application empowers businesses to 
  optimize energy usage, reduce costs, and work towards sustainable industrial processes. With this solution, 
  Neptune Controls aims to make factories more efficient and eco-friendly.`;

  userGuideLinkText: string = 'View User Guide';
  userGuideLinkUrl!: SafeResourceUrl;
  showPdf: boolean = false;

  constructor(private sanitizer: DomSanitizer) {}

  ngOnInit() {
    this.userGuideLinkUrl = this.sanitizer.bypassSecurityTrustResourceUrl('assets/images/PRWS_UserGuide.pdf');
  }

  showUserGuide() {
    this.showPdf = true;
  }
}
