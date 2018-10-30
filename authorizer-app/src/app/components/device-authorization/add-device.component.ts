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
  errorMessage: string;
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
      if(params.hasOwnProperty('error')) {
        this.errorMessage = params['error_description'];
      } else {
        this.errorMessage = null;
        this.addDeviceUser(params['code'], params['state']);
      }
    });
  }

  private updateDeviceUser() {

    this.deviceUser.startDate = new Date(Date.UTC(this.startDate.year, this.startDate.month-1, this.startDate.day)).toISOString();
    this.deviceUser.endDate = new Date(Date.UTC(this.endDate.year, this.endDate.month-1, this.endDate.day)).toISOString();
    this.devicesService.updateDeviceUser(this.deviceUser).subscribe(() => {
        return this.router.navigate(['/users']);
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
