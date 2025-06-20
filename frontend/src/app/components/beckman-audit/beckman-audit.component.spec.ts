import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BeckmanAuditComponent } from './beckman-audit.component';

describe('BeckmanAuditComponent', () => {
  let component: BeckmanAuditComponent;
  let fixture: ComponentFixture<BeckmanAuditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BeckmanAuditComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(BeckmanAuditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
