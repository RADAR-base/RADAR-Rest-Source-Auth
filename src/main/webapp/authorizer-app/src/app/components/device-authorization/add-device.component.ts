import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Params, Router} from '@angular/router';
import {AlertService} from '../alert/alert.service';
import {DevicesService} from "../../services/devices.service";
import {DeviceUser} from "../../models/device.model";
import {DeviceAuthorizationService} from "../../services/device-authorization.service";

@Component({
  selector: 'add-device',
  templateUrl: './add-device.component.html',
})
export class AddDeviceComponent implements OnInit {

  deviceUser: DeviceUser;
  startDate;
  endDate;

  constructor(private devicesService: DevicesService,
              private deviceAuthorizationService: DeviceAuthorizationService,
              private router: Router,
              private alertService: AlertService,
              private activatedRoute: ActivatedRoute) {
  }

  ngOnInit() {
    this.activatedRoute.queryParams.subscribe((params: Params) => {
      this.addDeviceUser(params['code'], params['state']);
    });
  }

  private updateDeviceUser() {
    console.log('user', this.deviceUser);
    this.deviceUser.startDate = new Date(this.startDate.year, this.startDate.month, this.startDate.day)
    console.log('user', this.deviceUser);
    this.devicesService.updateDeviceUser(this.deviceUser).subscribe(() => {
        return this.router.navigate(['/devices']);
      },
      err => {
        this.alertService.error(err.json._body);

      });
  }

  private addDeviceUser(code: string, state: string) {
    this.devicesService.addAuthorizedUser(code, state).subscribe(data => {
        this.deviceUser = data;
      },
      (err: Response) => {
        this.alertService.error('Cannot retrieve current user details');
        window.setTimeout(() => this.router.navigate(['']), 5000);
      });
  }
}
