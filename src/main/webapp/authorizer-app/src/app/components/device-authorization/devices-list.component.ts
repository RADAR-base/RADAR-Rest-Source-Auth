import {Component, OnInit} from '@angular/core';
import {AlertService} from '../alert/alert.service';
import {DeviceUser} from "../../models/device.model";
import {DevicesService} from "../../services/devices.service";

@Component({
  selector: 'devices-list',
  templateUrl: './devices-list.component.html',
  styleUrls: ['./devices-list.component.css']
})
export class DevicesListComponent implements OnInit {

  devices: DeviceUser[];

  constructor(private deviceService: DevicesService,
              private alertService: AlertService,
  ) {}

  ngOnInit() {
    console.log("on ingit")
    this.loadAllUsers();
  }

  private loadAllUsers() {
    this.deviceService.getDevices().subscribe(devicesList => {
        this.devices = devicesList;
      },
      () => {
        this.alertService.error('Cannot load registered devices!');
      });
  }

  removeDevice(device: DeviceUser) {
    this.deviceService.delete(device.id).subscribe(() => {
      this.loadAllUsers();
    });
  }
}
