import {Component, OnInit} from '@angular/core';
import {AlertService} from '../alert/alert.service';
import {DeviceUser} from "../../models/device.model";
import {DevicesService} from "../../services/devices.service";

@Component({
  selector: 'devices-list',
  templateUrl: './devices-list.component.html',
})
export class DevicesListComponent implements OnInit {

  devices: DeviceUser[];
  public isCollapsed = true;

  constructor(private deviceService: DevicesService,
              private alertService: AlertService) {}

  ngOnInit() {
    this.loadAllUsers();
  }

  private loadAllUsers() {
    this.deviceService.getDevices().subscribe((devicesList: any) => {
        this.devices = devicesList.users;
      },
      () => {
        this.alertService.error('Cannot load registered users!');
      });
  }

  removeDevice(device: DeviceUser) {
    this.deviceService.deleteUser(device.id).subscribe(() => {
      this.loadAllUsers();
    });
  }
}
