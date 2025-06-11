import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { environment } from '../../../environments/environment';

interface TemplateData {
  id: number;
  name: string;
  report_group: string;
  additionalInfo: string;
  parameters: string[];
  selected?: boolean;
}

@Component({
  selector: 'list-template',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './list-template.component.html',
  styleUrls: ['./list-template.component.css']
})

export class ListTemplateComponent implements OnInit {
  private apiBaseUrl = environment.apiBaseUrl;
  data: TemplateData[] = [];
  filteredData: TemplateData[] = [];
  parameterDetailsVisibility: boolean[] = [];
  searchQuery: string = '';
  additionalInfoList = ['MAX', 'AVG', 'MIN'];
  groupNameList: string[] = [];
  parameters: string[] = [];
  selectedParameters: any[] = [];
  unselectedParameters: string[] = [];
  selectedAdditionalInfo: string[] = [];
  isDeletePopupVisible: boolean = false;
  currentUsername = localStorage.getItem('username');
  selectedTemplate: TemplateData = {
    id: 0,
    name: '',
    report_group: '',
    additionalInfo: '',
    parameters: [],
    selected: false
  };
  selectedGroup: string = '';
  isInfoDropdownOpen = false;
  isGroupDropdownOpen = false;
  isEditVisible = false;
  isAscendingSort = true;

  isAscendingSortByName: boolean = false;
  isAscendingSortByGroup: boolean = false;

  constructor(private http: HttpClient, private router: Router) { }

  navigateTo(route: string): void {
    this.router.navigate([route]);
  }

  ngOnInit(): void {
    this.loadTemplates();
    this.getParametersList();
    this.fetchGroupNames();
  }

  fetchGroupNames(): void {
    this.http.get<any[]>(`${this.apiBaseUrl}/groups`)
      .subscribe(
        (data: any[]) => {
          this.groupNameList = data.map(item => item.name);
        },
        (error) => {
          console.error('Error fetching group names', error);
        }
      );
  }

  loadTemplates(): void {
    this.http.get<TemplateData[]>(`${this.apiBaseUrl}/templates`, { responseType: 'json' }).subscribe(
      (response: TemplateData[]) => {
        this.data = response;
        this.filteredData = response;
      },
      (error) => {
        console.error('Error fetching templates', error);
      }
    );
  }


getParametersList(): void {
  this.http.get<string[]>(`${this.apiBaseUrl}/parameters`, { responseType: 'json' }).subscribe(
    (response: string[]) => {
      // Remove EMS_NEW_ prefix from all available parameters
      this.parameters = response.map(param =>
        param.startsWith("EMS_NEW_") ? param.replace("EMS_NEW_", "") : param
      );
      this.updateUnselectedParameters();
    },
    (error) => {
      console.error('Error fetching parameters', error);
    }
  );
}


  updateUnselectedParameters(): void {
    const selectedBaseNames = this.selectedParameters.map(param => param.baseName);
    this.unselectedParameters = this.parameters.filter(param => !selectedBaseNames.includes(param));
  }

  parseParameters(parameters: string[]): any[] {
    return parameters.map(param => {
      const rangeMatch = param.match(/(.*)_From_(\d+)_To_(\d+)_?(.*)?/);
      const unitMatch = param.match(/(.*)_Unit_(.*)/);
      if (rangeMatch) {
        const baseName = rangeMatch[1];
        const min = +rangeMatch[2];
        const max = +rangeMatch[3];
        const unit = rangeMatch[4] || '';
        return { baseName, range: { min, max, unit }, displayName: `${baseName} (${min}-${max}${unit ? ' ' + unit : ''})` };
      } else if (unitMatch) {
        const baseName = unitMatch[1];
        const unit = unitMatch[2];
        return { baseName, range: { min: null, max: null, unit }, displayName: `${baseName} (${unit})` };
      } else {
        return { baseName: param, range: { min: null, max: null, unit: '' }, displayName: param };
      }
    });
  }
  filterTemplates(): void {
    if (!this.searchQuery) {
      this.filteredData = this.data;
    } else {
      const lowerCaseQuery = this.searchQuery.toLowerCase();
      this.filteredData = this.data.filter(item =>
        item.name.toLowerCase().includes(lowerCaseQuery)
      );
    }
  }


  deleteSelected(): void {
    if (this.filteredData.some(item => item.selected)) {
      this.isDeletePopupVisible = true;
    } else {
      alert('Please select at least one template to delete.');
    }
  }

  // confirmDeletion(): void {
  //   const selectedItems = this.filteredData.filter(item => item.selected);
  //   if (selectedItems.length > 0) {
  //     const idsToDelete = selectedItems.map(item => item.id);

  //     this.http.post(`${this.apiBaseUrl}/deleteTemplates`, idsToDelete, { responseType: 'json' })
  //       .subscribe(
  //         () => {
  //           console.log('Templates deleted successfully:', idsToDelete);

  //           // Log the deletion with IDs
  //           this.http.post(`${this.apiBaseUrl}/log-deleted-template`, {
  //             username: this.currentUsername,
  //             templateIds: idsToDelete // Pass the list of IDs here
  //           }, { responseType: 'text' }) // Specify responseType as 'text'
  //             .subscribe(
  //               (response) => {
  //                 console.log('Deletion logged successfully:', response);
  //                 this.data = this.data.filter(item => !item.selected);
  //                 this.filteredData = this.filteredData.filter(item => !item.selected);
  //                 this.isDeletePopupVisible = false;
  //               },
  //               (error) => {
  //                 console.error('Error logging template deletion', error);
  //               }
  //             );
  //         },
  //         (error) => {
  //           console.error('Error deleting templates', error);
  //         }
  //       );
  //   } else {
  //     alert("None of the templates have been selected for deletion.");
  //   }
  // }

  confirmDeletion(): void {
    const selectedItems = this.filteredData.filter(item => item.selected);
    if (selectedItems.length > 0) {
        const idsToDelete = selectedItems.map(item => item.id);
        const namesToDelete = selectedItems.map(item => item.name); // Retrieve names

        this.http.post(`${this.apiBaseUrl}/deleteTemplates`, idsToDelete, { responseType: 'json' })
            .subscribe(
                () => {
                    console.log('Templates deleted successfully:', idsToDelete);

                    // Log the deletion with IDs and names
                    this.http.post(`${this.apiBaseUrl}/log-deleted-template`, {
                        username: this.currentUsername,
                        templateIds: idsToDelete,
                        templateNames: namesToDelete // Pass names along with IDs
                    }, { responseType: 'text' })
                        .subscribe(
                            (response) => {
                                console.log('Deletion logged successfully:', response);
                                this.data = this.data.filter(item => !item.selected);
                                this.filteredData = this.filteredData.filter(item => !item.selected);
                                this.isDeletePopupVisible = false;
                            },
                            (error) => {
                                console.error('Error logging template deletion', error);
                            }
                        );
                },
                (error) => {
                    console.error('Error deleting templates', error);
                }
            );
    } else {
        alert("None of the templates have been selected for deletion.");
    }
  }


  cancelDeletion(): void {
    this.isDeletePopupVisible = false;
    this.clearSelections();
  }

  toggleParameterDetails(index: number): void {
    this.parameterDetailsVisibility[index] = !this.parameterDetailsVisibility[index];
  }

  getShortParameters(parameters: string[]): string {
    if (parameters.length === 0) {
      return '';
    }
    return parameters.slice(0, 2).join(', ') + (parameters.length > 2 ? ', ...' : '');
  }

  isParameterDetailsVisible(index: number): boolean {
    return this.parameterDetailsVisibility[index];
  }

  editSelected(): void {
    const selectedItems = this.filteredData.filter(item => item.selected);
    if (selectedItems.length !== 1) {
      alert('Please select exactly one template to edit.');
      return;
    }
  
    this.selectedTemplate = { ...selectedItems[0] };
    this.selectedGroup = this.selectedTemplate.report_group;
    this.selectedAdditionalInfo = this.selectedTemplate.additionalInfo
      ? this.selectedTemplate.additionalInfo.split(',')
      : [];
    this.selectedParameters = this.parseParameters(this.selectedTemplate.parameters);
    this.updateUnselectedParameters();
    this.isEditVisible = true;
  }
  
  updateParameterRange(index: number): void {
    const param = this.selectedParameters[index];
    const { min, max, unit } = param.range;
    const baseName = param.baseName;

    let updatedParam;
    if (min !== null && max !== null && unit) {
  
      updatedParam = `${baseName}_From_${min}_To_${max}_Unit_${unit}`;
    } else if (min !== null && max !== null) {

      updatedParam = `${baseName}_From_${min}_To_${max}`;
    } else if (unit) {

      updatedParam = `${baseName}_Unit_${unit}`;
    } else {


      updatedParam = baseName;
    }
  
    this.selectedParameters[index] = { baseName, range: { min, max, unit } };
    this.selectedTemplate.parameters[index] = updatedParam;
  }
  

  validateRange(param: any): boolean {
    if (param.range.min !== null && param.range.max !== null) {
      return param.range.min < param.range.max;
    }
    return true;
  }

  saveEdit(): void {
    if (!this.selectedTemplate || !this.selectedTemplate.id) {
        console.error('Invalid template selected for editing');
        alert('No template selected or invalid template data');
        return;
    }

    // Add range validation before saving
    const invalidRanges = this.selectedParameters.filter(param => !this.validateRange(param));
    if (invalidRanges.length > 0) {
        alert('Start range must be less than end range for all parameters');
        return;
    }

    const templateObj = {
        name: this.selectedTemplate.name,
        report_group: this.selectedGroup,
        parameters: this.selectedParameters.map(p => {
          const { baseName, range } = p;
          const { min, max, unit } = range;
        
          if (min !== null && max !== null && unit) {
            return `${baseName}_From_${min}_To_${max}_Unit_${unit}`;
          } else if (min !== null && max !== null) {
            return `${baseName}_From_${min}_To_${max}`;
          } else if (unit) {
            return `${baseName}_Unit_${unit}`;
          } else {
            return baseName;
          }
        })
        
        
        
        // additionalInfo: this.selectedAdditionalInfo.join(',')
    };

    console.log('Template object for update:', templateObj);

    this.http.put(`${this.apiBaseUrl}/editTemplate/${this.selectedTemplate.id}`, templateObj, { responseType: 'json' })
        .subscribe(
            (response: any) => {
                console.log('Response after edit:', response);

                const index = this.filteredData.findIndex(item => item.id === this.selectedTemplate.id);
                if (index > -1) {
                    this.filteredData[index] = { ...response };
                    this.filterTemplates();
                    this.isEditVisible = false;

                    // Log the edit with ID and Name
                    this.http.post(`${this.apiBaseUrl}/log-edited-template`, {
                        username: this.currentUsername,
                        templateId: this.selectedTemplate.id, // Pass ID
                        templateName: this.selectedTemplate.name // Pass Name
                    }).subscribe(
                        () => {
                            console.log('Edit logged successfully');
                        },
                        (error) => {
                            console.error('Error logging template editing', error);
                        }
                    );

                    this.clearSelections();
                }
            },
            (error) => {
                console.error('Error updating template:', error);
            }
        );
}



  cancelEdit(): void {
    this.isEditVisible = false;
    this.clearSelections();
  }

  toggleDropdown(dropdownType: string): void {
    if (dropdownType === 'param') {
      this.isInfoDropdownOpen = false;
      this.isGroupDropdownOpen = false;
    } else if (dropdownType === 'unselected') {
      this.isInfoDropdownOpen = false;
      this.isGroupDropdownOpen = false;
    } else if (dropdownType === 'info') {
      this.isInfoDropdownOpen = !this.isInfoDropdownOpen;
      this.isGroupDropdownOpen = false;
    } else if (dropdownType === 'group') {
      this.isGroupDropdownOpen = !this.isGroupDropdownOpen;
      this.isInfoDropdownOpen = false;
    }
  }


  isSelected(param: string): boolean {
    return this.selectedParameters.some(p => p.baseName === param);
  }

  onParameterChange(event: any): void {
    const value = event.target.value;
    if (event.target.checked) {
      this.selectedParameters.push({ 
        baseName: value, 
        range: { min: null, max: null, unit: '' } 
      });
      this.unselectedParameters = this.unselectedParameters.filter(param => param !== value);
    } else {
      this.selectedParameters = this.selectedParameters.filter(p => p.baseName !== value);
      this.unselectedParameters.push(value);
    }
  }
  
  isInfoSelected(info: string): boolean {
    return this.selectedAdditionalInfo.includes(info);
  }

  toggleGroupDropdown(): void {
    this.isGroupDropdownOpen = !this.isGroupDropdownOpen;
  }

  onAdditionalInfoChange(event: any): void {
    const value = event.target.value;
    if (event.target.checked) {
      this.selectedAdditionalInfo.push(value);
    } else {
      this.selectedAdditionalInfo = this.selectedAdditionalInfo.filter(info => info !== value);
    }
  }


  clearSelections(): void {
    this.selectedTemplate = {
      id: 0,
      name: '',
      report_group: '',
      additionalInfo: '',
      parameters: [],
      selected: false
    };
    this.selectedParameters = [];
    this.unselectedParameters = [...this.parameters];
    this.filteredData.forEach(item => item.selected = false);
  }


  @HostListener('document:click', ['$event'])
  handleClickOutside(event: Event): void {
    const targetElement = event.target as HTMLElement;
    if (!targetElement.closest('.dropdown-container')) {
      this.isInfoDropdownOpen = false;
      this.isGroupDropdownOpen = false;
    }
  }

  sortDataById(): void {
    this.isAscendingSort = !this.isAscendingSort;

    this.filteredData.sort((a, b) => {
      return this.isAscendingSort ? a.id - b.id : b.id - a.id;
    });
  }

  sortByName(): void {
    this.isAscendingSortByName = !this.isAscendingSortByName;

    this.filteredData.sort((a, b) => {
      const nameA = a.name.toLowerCase();
      const nameB = b.name.toLowerCase();

      if (nameA < nameB) {
        return this.isAscendingSortByName ? -1 : 1;
      }
      if (nameA > nameB) {
        return this.isAscendingSortByName ? 1 : -1;
      }
      return 0;
    });
  }

  sortByGroup(): void {
    this.isAscendingSortByGroup = !this.isAscendingSortByGroup;

    this.filteredData.sort((a, b) => {
      const groupA = a.report_group.toLowerCase();
      const groupB = b.report_group.toLowerCase();

      if (groupA < groupB) {
        return this.isAscendingSortByGroup ? -1 : 1;
      }
      if (groupA > groupB) {
        return this.isAscendingSortByGroup ? 1 : -1;
      }
      return 0;
    });
  }
}
