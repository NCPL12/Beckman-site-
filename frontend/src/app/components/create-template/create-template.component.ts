import { Component, HostListener, OnInit } from '@angular/core';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'create-template',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule],
  templateUrl: './create-template.component.html',
  styleUrls: ['./create-template.component.css']
})
export class CreateTemplateComponent implements OnInit {
  private apiBaseUrl = environment.apiBaseUrl;
  additionalInfo = ['MAX', 'AVG', 'MIN'];
  groupNameList: string[] = [];
  parameters: string[] = [];
  reportName: string = '';
  groupName: string = '';
  selectedParameters: string[] = [];
  currentUsername = localStorage.getItem('username');
  parameterRanges: {
    [param: string]: {
      min: number,
      max: number,
      addRange: boolean,
      unit: string,
      rangeError?: string,
      unitError?: string
    }
  } = {};

  searchTerm: string = '';
  selectedAdditionalInfo: string[] = [];
  isDropdownOpen: boolean = false;
  isAdditionalInfoDropdownOpen: boolean = false;

  reportNameError: string = '';
  groupNameError: string = '';
  parametersError: string = '';
  additionalInfoError: string = '';

  isPopupVisible: boolean = false;

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    this.getParametersList();
    this.fetchGroupNames();
  }

  get filteredParameters(): string[] {
    if (!this.searchTerm) return this.parameters;
    return this.parameters.filter(param =>
      param.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }

  fetchGroupNames(): void {
    this.http.get<any[]>(`${this.apiBaseUrl}/groups`).subscribe(
      (data: any[]) => {
        this.groupNameList = data.map(item => item.name);
      },
      (error) => {
        console.error('Error fetching group names', error);
      }
    );
  }

  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  toggleAdditionalInfoDropdown(): void {
    this.isAdditionalInfoDropdownOpen = !this.isAdditionalInfoDropdownOpen;
  }
  getDisplayName(param: string): string {
  return param.startsWith('EMS_NEW_') ? param.replace('EMS_NEW_', '') : param;
}


getParametersList(): void {
  this.http.get<string[]>(`${this.apiBaseUrl}/parameters`).subscribe(
    (data) => {
      this.parameters = data; // DO NOT filter EMS_NEW_
    },
    (error) => {
      console.error('Error fetching parameters', error);
    }
  );
}



  isSelected(param: string): boolean {
    return this.selectedParameters.includes(param);
  }

  isAdditionalInfoSelected(info: string): boolean {
    return this.selectedAdditionalInfo.includes(info);
  }

  onParameterChange(event: any): void {
    const value = event.target.value;
    if (event.target.checked) {
      if (this.selectedParameters.length >= 12) {
        alert("You can select a maximum of 12 parameters.");
        event.target.checked = false;
        return;
      }
      this.selectedParameters.push(value);
      this.parameterRanges[value] = {
        min: 18,
        max: 25,
        addRange: false,
        unit: ''
      };
    } else {
      const index = this.selectedParameters.indexOf(value);
      if (index > -1) {
        this.selectedParameters.splice(index, 1);
        delete this.parameterRanges[value];
      }
    }
  }

 updateParameterRange(param: string): void {
  const range = this.parameterRanges[param];
  range.rangeError = '';

  if (range.addRange) {
    if (range.min === null || range.max === null || range.min >= range.max) {
      range.rangeError = 'Start range must be less than end range';
    }
  }
}


  onAdditionalInfoChange(event: any): void {
    const value = event.target.value;
    if (event.target.checked) {
      this.selectedAdditionalInfo.push(value);
    } else {
      const index = this.selectedAdditionalInfo.indexOf(value);
      if (index > -1) {
        this.selectedAdditionalInfo.splice(index, 1);
      }
    }
  }

  validateReportName(): void {
    if (!this.reportName) {
      this.reportNameError = 'Report Name is required';
    } else if (this.reportName.length > 20) {
      this.reportNameError = 'Report Name cannot exceed 20 characters';
    } else {
      this.reportNameError = '';
    }
  }

  validateGroupName(): void {
    if (!this.groupName) {
      this.groupNameError = 'Group Name is required';
    } else {
      this.groupNameError = '';
    }
  }

  validateParameters(): void {
    if (this.selectedParameters.length === 0) {
      this.parametersError = 'At least one parameter must be selected';
    } else {
      this.parametersError = '';
    }
  }

  validateAdditionalInfo(): void {
    if (this.selectedAdditionalInfo.length === 0) {
      this.additionalInfoError = 'At least one Additional Info must be selected';
    } else {
      this.additionalInfoError = '';
    }
  }

  validateForm(): boolean {
    this.validateReportName();
    this.validateGroupName();
    this.validateParameters();
    this.validateAdditionalInfo();
  
    let hasRangeUnitError = false;
  
    for (const param of this.selectedParameters) {
      const range = this.parameterRanges[param];
      if (range?.addRange) {
      if (range.min === null || range.max === null || range.min >= range.max) {
  range.rangeError = 'Start range must be less than end range';
  hasRangeUnitError = true;
} else {
  range.rangeError = '';
}

  
        if (range.min === null || range.max === null || range.min >= range.max) {
          range.rangeError = 'Start range must be less than end range';
          hasRangeUnitError = true;
        } else {
          range.rangeError = '';
        }
      }
    }
  
    if (hasRangeUnitError) {
    alert('Please enter a valid range for selected parameters with "Add Range & Units" checked.');

    }
  
    return !(
      this.reportNameError ||
      this.groupNameError ||
      this.parametersError ||
      this.additionalInfoError ||
      hasRangeUnitError
    );
  }
  

  showConfirmationPopup(): void {
    if (this.validateForm()) {
      this.isPopupVisible = true;
    } else {
      console.log('Validation failed');
    }
  }

  confirmSubmission(): void {
    if (this.validateForm()) {
      this.isPopupVisible = false;
      this.postTemplate();
    }
  }

  cancelSubmission(): void {
    this.isPopupVisible = false;
  }

  resetForm(): void {
    this.reportName = '';
    this.groupName = '';
    this.selectedParameters = [];
    this.selectedAdditionalInfo = [];
    this.parameterRanges = {};
    this.reportNameError = '';
    this.groupNameError = '';
    this.parametersError = '';
    this.additionalInfoError = '';
    this.isDropdownOpen = false;
    this.isAdditionalInfoDropdownOpen = false;
  }

  postTemplate(): void {
    if (this.validateForm()) {
      const formattedParameters = this.selectedParameters.map(param => {
        const range = this.parameterRanges[param];
        if (range && range.addRange) {
          return `${param}_From_${range.min}_To_${range.max}_Unit_${range.unit}`;
        } else {
          return param;
        }
      });

      const templateObj = {
        name: this.reportName,
        report_group: this.groupName,
        parameters: formattedParameters,
        additionalInfo: this.selectedAdditionalInfo.join(',')
      };

      console.log('Data to be sent:', templateObj);

      this.http.post(`${this.apiBaseUrl}/createTemplate`, templateObj, { responseType: 'json' })
        .subscribe(
          (successResponse: any) => {
            console.log("Response: ", successResponse);
            alert("Template added successfully");
            this.resetForm();
          },
          (error) => {
            console.error('Error posting template', error);
          }
        );
    } else {
      console.log('Validation failed');
    }
  }

  @HostListener('document:click', ['$event'])
  handleClickOutside(event: Event): void {
    const targetElement = event.target as HTMLElement;
    if (!targetElement.closest('.dropdown-container')) {
      this.isDropdownOpen = false;
      this.isAdditionalInfoDropdownOpen = false;
    }
  }

  goBack(): void {
    this.router.navigate(['list']);
  }
}
