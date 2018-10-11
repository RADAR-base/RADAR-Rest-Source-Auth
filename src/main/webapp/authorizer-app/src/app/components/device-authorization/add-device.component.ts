import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Params, Router} from '@angular/router';
import {AlertService} from '../alert/alert.service';
import {DevicesService} from "../../services/devices.service";
import {Device} from "../../models/device.model";
import {DeviceAuthorizationService} from "../../services/device-authorization.service";

@Component({
  selector: 'add-device',
  templateUrl: './add-device.component.html',
  styleUrls: ['./add-device.component.css']
})
export class AddDeviceComponent implements OnInit {

  device: Device;

  constructor(private devicesService: DevicesService,
              private deviceAuthorizationService: DeviceAuthorizationService,
              private router: Router,
              private alertService: AlertService,
              private activatedRoute: ActivatedRoute) {
  }

  ngOnInit() {
    this.activatedRoute.queryParams.subscribe((params: Params) => {
      console.log("params" , params)
      this.getExternalUserId(params['code'], params['state']);
    });
  }

  private addDevice() {
    this.devicesService.addDevice(this.device).subscribe(() => {
        return this.router.navigate(['/devices']);
      },
      err => {
        this.alertService.error(err.json._body);

      });
  }

  private getExternalUserId(code: string, state: string) {
    this.deviceAuthorizationService.authorize(code, state).subscribe(data => {
        this.device = data;
        console.log("created deevice" , this.device)
      },
      (err: Response) => {
        this.alertService.error('Cannot retrieve current user details');
        window.setTimeout(() => this.router.navigate(['']), 5000);
      });
  }
}
