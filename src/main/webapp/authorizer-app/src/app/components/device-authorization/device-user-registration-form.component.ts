import {Component, OnInit} from "@angular/core";
import {AlertService} from "../alert/alert.service";
import {DevicesService} from "../../services/devices.service";
import {environment} from "../../../environments/environment";
import {PlatformLocation} from "@angular/common";
import {DeviceAuthorizationService} from "../../services/device-authorization.service";
import {FormBuilder, FormControl, FormGroup} from "@angular/forms";
import {DeviceAuthDetails} from "../../models/device-auth-details.model";

@Component({
  selector: 'user-registration-form',
  templateUrl: './device-user-registration-form.component.html'
})
export class DeviceUserRegistrationFormComponent implements OnInit {

  deviceTypeForm: any;
  registrationServiceUrl: String;
  callbackUrl: String;
  clientId: String;
  authState: String;

  deviceTypes: string[];
  deviceAuthDetail: DeviceAuthDetails;
  // deviceTypeControl


  constructor(private deviceService: DevicesService,
              private deviceAuthorizationService: DeviceAuthorizationService,
              private alertService: AlertService,
              private fb: FormBuilder,
              private platformLocation: PlatformLocation
  ) {
    this.createForm();

  }

  ngOnInit(): void {
    console.log("on init")
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
    console.log('evt',deviceType)
    this.deviceAuthorizationService.getDeviceClientAuthDetails(deviceType).subscribe(
      data => {
        this.deviceAuthDetail = data;
        console.log("receive detail ", data)
        this.callbackUrl = window.location.origin
          + this.platformLocation.getBaseHrefFromDOM()
          + 'devices:new';
      }
    )
  }

}
