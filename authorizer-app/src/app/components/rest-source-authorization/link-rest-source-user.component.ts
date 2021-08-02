import {Component, OnInit} from '@angular/core';
import {FormBuilder, Validators} from '@angular/forms';
import {RestSourceUserMockService} from '../../services/rest-source-user-mock.service';
import {SourceClientAuthorizationMockService} from '../../services/source-client-authorization-mock.service';
import {MatSelectChange} from '@angular/material';
import {RestSourceUserService} from '../../services/rest-source-user.service';
import {SourceClientAuthorizationService} from '../../services/source-client-authorization.service';
import {Router} from '@angular/router';
// import {NgbDateAdapter} from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-link-rest-source-user',
  templateUrl: './link-rest-source-user.component.html',
  styleUrls: ['./link-rest-source-user.component.scss'],
  // providers: [{ provide: NgbDateAdapter, useClass: NgbDateNativeAdapter }]
})
export class LinkRestSourceUserComponent implements OnInit {
  error?: string;
  linkGeneratingLoading = false;
  authorizationLoading = false;

  sourceTypes$ = this.sourceTypeService.getDeviceTypes();
  projects$ = this.service.getAllProjects();
  subjects$;
  linkForUser?: string;

  form = this.fb.group({
    sourceType: [null, Validators.required],
    projectId: [null, Validators.required],
    userId: [null, Validators.required],
    startDate: [null, Validators.required],
    endDate: [null],
  });

  constructor(
    private fb: FormBuilder,
    private mockService: RestSourceUserMockService,
    private service: RestSourceUserService,
    // private sourceTypeService: SourceClientAuthorizationMockService,
    private sourceTypeService: SourceClientAuthorizationService,
    private router: Router
  ) {}

  ngOnInit(): void {}

  onSubmit(e: any): void {
    // alert('Thanks!');
    this.error = null;

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

  onProjectSelectionChange(e: MatSelectChange) {
    // on project change users should be changed
    console.log(e);
    // this.service.getAllSubjectsOfProjects(e.value).subscribe(next => console.log(next));
    this.subjects$ = this.service.getAllSubjectsOfProjects(e.value);
  }

  onBack() {
    this.router.navigateByUrl('/');
  }

  copyToClipboard() {
    console.log('copy to clipboard');
  }
}
