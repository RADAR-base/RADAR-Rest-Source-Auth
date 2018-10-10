import { Component, OnInit } from '@angular/core';
import {MatDialog, MatDialogConfig} from '@angular/material';
import {AddDeviceDialogComponent} from "../add-device-dialog/add-device-dialog.component";

@Component({
  selector: 'app-devices-header',
  templateUrl: './devices-header.component.html',
  styleUrls: ['./devices-header.component.css']
})
export class DevicesHeaderComponent implements OnInit {

  constructor(private dialog: MatDialog) {}

  openDialog() {

    const dialogConfig = new MatDialogConfig();

    dialogConfig.disableClose = true;
    dialogConfig.autoFocus = true;

    this.dialog.open(AddDeviceDialogComponent, dialogConfig);
  }

  ngOnInit(): void {
  }
}
