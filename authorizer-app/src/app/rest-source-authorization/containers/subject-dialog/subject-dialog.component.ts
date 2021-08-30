import {
  Component,
  Inject, Input,
} from '@angular/core';
import { RestSourceUser } from '../../models/rest-source-user.model';
import {FormBuilder, FormControl, Validators} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {RestSourceUserMockService} from '../../services/rest-source-user-mock.service';
import {RestSourceUserService} from '../../services/rest-source-user.service';
import {SourceClientAuthorizationService} from '../../services/source-client-authorization.service';
import {Router} from '@angular/router';
import {SourceClientAuthorizationMockService} from '../../services/source-client-authorization-mock.service';

@Component({
  selector: 'app-subject-dialog',
  templateUrl: 'subject-dialog.component.html',
  styleUrls: ['subject-dialog.component.scss']
})
export class SubjectDialogComponent {
  mode = this.data.mode;
  subject = this.data.subject;

  error?: string;
  linkGeneratingLoading = false;
  authorizationLoading = false;

  sourceTypes$ = this.sourceTypeService.getDeviceTypes();
  // projects$ = this.service.getAllProjects();
  // subjects$;
  linkForUser?: string;

  form = this.fb.group({
    sourceType: [{value: this.subject.sourceType, disabled: this.mode != 'add'}, Validators.required],
    projectId: [{value: this.subject.projectId, disabled: true}],
    userId: [{value: this.subject.userId, disabled: true}],
    externalId: [{value: this.subject.externalId, disabled: true}],
    // range: [{value: null, disabled: this.mode == 'view'}],
    startDate: [{value: this.subject.startDate, disabled: this.mode == 'view'}, Validators.required],
    endDate: [{value: this.subject.endDate, disabled: this.mode == 'view'}],
    isAuthorized: [{value: this.subject.isAuthorized, disabled: true}],
    sourceId: [{value: this.subject.sourceId, disabled: true}],
    serviceUserId: [{value: this.subject.serviceUserId, disabled: true}],
    hasValidToken: [{value: this.subject.hasValidToken, disabled: true}],
    timesReset: [{value: this.subject.timesReset, disabled: true}],
  });



  // startDateFormControl: FormControl;
  // endDateFormControl: FormControl;
  //
  // // Stores a copy of the data so as to not modify the original content
  // dataCopy: RestSourceUser;

  constructor(
    public dialogRef: MatDialogRef<SubjectDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {subject: RestSourceUser; mode: string},
    private fb: FormBuilder,
    private mockService: RestSourceUserMockService,
    private service: RestSourceUserService,
    private sourceTypeService: SourceClientAuthorizationMockService,
    // private sourceTypeService: SourceClientAuthorizationService,
    private router: Router
  ) {
    // this.startDateFormControl = new FormControl(moment(this.data.startDate));
    // this.endDateFormControl = new FormControl(moment(this.data.endDate));
    // this.dataCopy = deepcopy(this.data);
  }

  ngOnInit(): void {}

  onSubmit(e: any): void {
    // alert('Thanks!');
    this.error = undefined;

    console.log(e.submitter.name);
    console.log(this.form.value);
    console.log(this.form.value.startDate.unix());
    console.log(this.form.value.startDate.valueOf());
    const persistent = e.submitter.name === 'link';
    this.linkGeneratingLoading = persistent;
    this.authorizationLoading = !persistent;
    // send POST req to /users {RestSourceUserReq} = {}
    // subscribe
    // success => send POST to /registrations = {}
    // subscribe
    // success =>
    // 1) redirect to endpointUrl (researcher)
    // 2) ? (subject) generate link and show it / copy to clipboard or ...
    // error: show error
    // error: show error
    const user = {
      projectId: this.form.value.projectId,
      userId: this.form.value.userId,
      // sourceId: '789', // this.form.value.sourceId,
      startDate: this.form.value.startDate.valueOf(),
      endDate: this.form.value.endDate ? this.form.value.endDate.valueOf() : null,
      sourceType: this.form.value.sourceType,
    };
    console.log(user);
    //
    this.service.createUser(user).subscribe(
      resp => {
        console.log(resp);
        this.mockService.registerUser({userId: resp.id, persistent}).subscribe(
          registrationResp => {
            console.log(registrationResp);
            if (registrationResp.authEndpointUrl) {
              // redirect to endpointUrl
              window.location.href = registrationResp.authEndpointUrl;
            } else if (registrationResp.secret) {
              // generate link // show // copy to clipboard
              this.linkForUser =
                `https://rest-source-auth-frontend/users:auth?token=${registrationResp.token}&secret=${registrationResp.secret}`;
              this.linkGeneratingLoading = false;
            }
          },
          err => {
            console.log(err);
            this.error = err.error.error_description; // err.message;
            this.linkGeneratingLoading = false;
            this.authorizationLoading = false;
          }
        );
      },
      err => {
        console.log(err);
        if (err.status === 409 && err.error.error === 'user_exists') {
          this.mockService.registerUser({userId: '11', persistent}).subscribe(
            registrationResp => {
              console.log(registrationResp);
              if (registrationResp.authEndpointUrl) {
                // redirect to endpointUrl
                window.location.href = registrationResp.authEndpointUrl;
              } else if (registrationResp.secret) {
                // generate link // show // copy to clipboard
                this.linkForUser =
                  `https://rest-source-auth-frontend/users:auth?token=${registrationResp.token}&secret=${registrationResp.secret}`;
                this.linkGeneratingLoading = false;
              }
            },
            error => {
              console.log(error);
              this.error = err.error.error_description; // err.message;
              this.linkGeneratingLoading = false;
              this.authorizationLoading = false;
            }
          );
        } else {
          this.error = err.error.error_description; // message;
          this.linkGeneratingLoading = false;
          this.authorizationLoading = false;
        }
      }
    );
  }

  // onProjectSelectionChange(e: MatSelectChange) {
  //   // on project change users should be changed
  //   console.log(e);
  //   // this.service.getAllSubjectsOfProjects(e.value).subscribe(next => console.log(next));
  //   this.subjects$ = this.service.getAllSubjectsOfProjects(e.value);
  // }
  //
  // onBack() {
  //   this.router.navigateByUrl('/');
  // }

  copyToClipboard() {
    console.log('copy to clipboard');
  }

  closeResetDialog(): void {
    this.dialogRef.close();
  }

  // updateStartDateValue(event: MatDatepickerInputEvent<any>) {
  //   // this.dataCopy.startDate = event.value.toISOString();
  // }
  //
  // updateEndDateValue(event: MatDatepickerInputEvent<any>) {
  //   // this.dataCopy.endDate = event.value.toISOString();
  // }
}
