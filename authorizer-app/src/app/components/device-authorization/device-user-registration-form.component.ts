import {Component, OnInit} from "@angular/core";
import {DevicesService} from "../../services/devices.service";
import {PlatformLocation} from "@angular/common";
import {DeviceAuthorizationService} from "../../services/device-authorization.service";
import {FormBuilder} from "@angular/forms";
import {DeviceAuthDetails} from "../../models/device-auth-details.model";

@Component({
  selector: 'user-registration-form',
  templateUrl: './device-user-registration-form.component.html'
})
export class DeviceUserRegistrationFormComponent implements OnInit {

  deviceTypeForm: any;
  callbackUrl: String;
  clientId: String;

  deviceTypes: string[];
  deviceAuthDetail: DeviceAuthDetails;
  // deviceTypeControl


  constructor(private deviceService: DevicesService,
              private deviceAuthorizationService: DeviceAuthorizationService,
              private fb: FormBuilder,
              private platformLocation: PlatformLocation
  ) {
    this.createForm();

  }

  ngOnInit(): void {
    this.deviceAuthorizationService.getDeviceTypes().subscribe(
      data => {
        this.deviceTypes = data;
      }
    )
  }

  createForm() {
    this.deviceTypeForm = this.fb.group({
      selectedDeviceType: '',
    });
  }

  onChange(deviceType: any) {
    this.deviceAuthorizationService.getDeviceClientAuthDetails(deviceType).subscribe(
      data => {
        this.deviceAuthDetail = data;
        this.callbackUrl = window.location.origin
          + this.platformLocation.getBaseHrefFromDOM()
          + 'users:new';
      }
    )
  }

}
