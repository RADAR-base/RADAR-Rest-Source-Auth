import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AddDeviceDialogComponent } from './add-device-dialog.component';

describe('AddDeviceDialogComponent', () => {
  let component: AddDeviceDialogComponent;
  let fixture: ComponentFixture<AddDeviceDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AddDeviceDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AddDeviceDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
