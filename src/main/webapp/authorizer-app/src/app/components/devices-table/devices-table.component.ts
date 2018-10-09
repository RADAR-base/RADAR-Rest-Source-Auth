import { Component, OnInit } from '@angular/core';
import {DevicesService} from "../../services/devices.service";
import {Observable} from "rxjs/internal/Observable";
import {Device} from "../../models/device.model";
import {DataSource} from "@angular/cdk/table";

@Component({
  selector: 'app-devices-table',
  templateUrl: './devices-table.component.html',
  styleUrls: ['./devices-table.component.css']
})
export class DevicesTableComponent implements OnInit {
  dataSource = new DeviceDataSource(this.userService);

  displayedColumns = ['id', 'projectId', 'userId', 'sourceId', 'externalUserId'];

  constructor(private userService: DevicesService) { }

  ngOnInit() {
  }
}

export class DeviceDataSource extends DataSource<any> {

  constructor(private userService: DevicesService) {
    super();
  }

  connect(): Observable<Device[]> {
    return this.userService.getDevices();
  }

  disconnect() {

  }
}
