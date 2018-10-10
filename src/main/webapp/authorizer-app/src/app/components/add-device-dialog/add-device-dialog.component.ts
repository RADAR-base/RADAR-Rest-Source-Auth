import {Component, Inject, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material";
import {DevicesService} from "../../services/devices.service";
import {Device} from "../../models/device.model";

@Component({
  selector: 'app-add-device-dialog',
  templateUrl: './add-device-dialog.component.html',
  styleUrls: ['./add-device-dialog.component.css']
})
export class AddDeviceDialogComponent implements OnInit {

  deviceId:number;

  form: FormGroup;
  description:string;

  constructor(
    private fb: FormBuilder,
    private deviceService: DevicesService,
    private dialogRef: MatDialogRef<AddDeviceDialogComponent>,
    @Inject(MAT_DIALOG_DATA) device:Device) {

    this.description = "Add a new device";
    this.deviceId = device.id;

    this.form = fb.group({

    });
  }

  ngOnInit() {
    this.form = this.fb.group({
      description: [this.description, []]
    });
  }

  save() {
    this.dialogRef.close(this.form.value);
  }

  close() {
    this.dialogRef.close();
  }
}
