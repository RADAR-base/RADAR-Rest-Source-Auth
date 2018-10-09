import { Component, OnInit } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material';
import {AddDeviceDialogComponent} from "../add-device-dialog/add-device-dialog.component";

@Component({
  selector: 'app-devices-header',
  templateUrl: './devices-header.component.html',
  styleUrls: ['./devices-header.component.css']
})
export class DevicesHeaderComponent implements OnInit {

  addDeviceDialogRef: MatDialogRef<AddDeviceDialogComponent>;

  constructor(private dialog: MatDialog) { }

  ngOnInit() {
  }

  addDevice() {
    this.addDeviceDialogRef = this.dialog.open(AddDeviceDialogComponent);
  }
}
