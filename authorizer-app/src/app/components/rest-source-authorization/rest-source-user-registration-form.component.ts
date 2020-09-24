import {Component, OnInit} from '@angular/core';
import {RestSourceUserService} from '../../services/rest-source-user.service';
import {PlatformLocation} from '@angular/common';
import {SourceClientAuthorizationService} from '../../services/source-client-authorization.service';
import {FormBuilder} from '@angular/forms';
import {RestSourceClientDetails} from '../../models/source-client-details.model';

@Component({
  selector: 'rest-source-user-registration-form',
  templateUrl: './rest-source-user-registration-form.component.html'
})
export class RestSourceUserRegistrationFormComponent implements OnInit {

  sourceTypeForm: any;
  callbackUrl: String;

  sourceTypes: string[];
  sourceClientDetail: RestSourceClientDetails;
  showForm: boolean = false

  constructor(private restSourceUserService: RestSourceUserService,
              private sourceClientAuthorizationService: SourceClientAuthorizationService,
              private fb: FormBuilder,
              private platformLocation: PlatformLocation
  ) {


  }

  ngOnInit(): void {
    this.sourceClientAuthorizationService.getDeviceTypes().subscribe(
      data => {
        this.sourceTypes = data;
        this.createForm();
        this.intiSourceClientDetails()
      }
    );
  }

  createForm() {
    this.sourceTypeForm = this.fb.group({
      selectedDeviceType: this.sourceTypes[0],
    });
    this.showForm = true
  }

  onChange(sourceType: any) {
    this.sourceClientAuthorizationService.getSourceClientAuthDetails(sourceType).subscribe(
      data => {
        this.sourceClientDetail = data;
        this.callbackUrl = window.location.origin
          + this.platformLocation.getBaseHrefFromDOM()
          + 'users:new';
      }
    );
  }

  intiSourceClientDetails() {
    this.sourceClientAuthorizationService.getSourceClientAuthDetails(this.sourceTypes[0]).subscribe(
      data => {
        this.sourceClientDetail = data;
        this.callbackUrl = window.location.origin
          + this.platformLocation.getBaseHrefFromDOM()
          + 'users:new';
      }
    );
  }

}
