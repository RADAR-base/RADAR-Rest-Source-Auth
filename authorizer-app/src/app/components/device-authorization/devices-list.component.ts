import {Component, OnInit} from '@angular/core';
import {DeviceUser} from "../../models/device.model";
import {DevicesService} from "../../services/devices.service";

@Component({
  selector: 'devices-list',
  templateUrl: './devices-list.component.html',
})
export class DevicesListComponent implements OnInit {
  errorMessage: string;
  devices: DeviceUser[];
  public isCollapsed = true;

  constructor(private deviceService: DevicesService) {}

  ngOnInit() {
    this.loadAllUsers();
  }

  private loadAllUsers() {
    this.deviceService.getDevices().subscribe((devicesList: any) => {
        this.devices = devicesList.users;
      },
      () => {
        this.errorMessage = 'Cannot load registered users!';
      });
  }

  removeDevice(device: DeviceUser) {
    this.deviceService.deleteUser(device.id).subscribe(() => {
      this.loadAllUsers();
    });
  }
}
