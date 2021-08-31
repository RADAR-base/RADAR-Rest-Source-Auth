import {Component, OnInit} from '@angular/core';
import {FormBuilder, Validators} from '@angular/forms';
import {RestSourceUserMockService} from '../../services/rest-source-user-mock.service';
import {SourceClientAuthorizationMockService} from '../../services/source-client-authorization-mock.service';
import {MatSelectChange} from '@angular/material';
import {RestSourceUserService} from '../../services/rest-source-user.service';
import {SourceClientAuthorizationService} from '../../services/source-client-authorization.service';
import {ActivatedRoute, Router} from '@angular/router';
// import {NgbDateAdapter} from '@ng-bootstrap/ng-bootstrap';
import { environment } from '../../../environments/environment';

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
  isEditing = false;

  form = this.fb.group({
    sourceType: [null, Validators.required],
    projectId: [null, Validators.required],
    userId: [null, Validators.required],
    startDate: [null, Validators.required],
    endDate: [null],
  });

  constructor(
    private fb: FormBuilder,
    // private mockService: RestSourceUserMockService,
    private service: RestSourceUserService,
    // private sourceTypeService: SourceClientAuthorizationMockService,
    private sourceTypeService: SourceClientAuthorizationService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {
    console.log(this.activatedRoute.snapshot.params);
    console.log(this.activatedRoute.snapshot.queryParams);
    this.isEditing = this.activatedRoute.snapshot.queryParams.link;
  }

  ngOnInit(): void {
    console.log('LinkRestSourceUserComponent');
  }

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
      startDate: this.form.value.startDate, // .valueOf(),
      endDate: this.form.value.endDate ? this.form.value.endDate : null, // .valueOf() : null,
      sourceType: this.form.value.sourceType,
    };
    console.log(user);
    //
    this.service.createUser(user).subscribe(
      resp => {
        console.log(resp);
        this.registerUser(resp.userId, persistent);
      },
      err => {
        console.log(err);
        if (err.status === 409 && err.error.error === 'user_exists') {
          this.registerUser(user.userId, persistent);
        } else {
          this.error = err.error.error_description; // message;
          this.linkGeneratingLoading = false;
          this.authorizationLoading = false;
        }
      }
    );
  }

  registerUser(userId, persistent): void {
    this.service.registerUser({userId, persistent}).subscribe(
      registrationResp => {
        console.log(registrationResp);
        if (registrationResp.authEndpointUrl) {
          // redirect to endpointUrl
          window.location.href = registrationResp.authEndpointUrl;
        } else if (registrationResp.secret) {
          // generate link // show // copy to clipboard
          this.linkForUser =
            `${environment.baseUrl}/users:auth?token=${registrationResp.token}&secret=${registrationResp.secret}`;
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

  // copyToClipboard() {
  //   console.log('copy to clipboard');
  // }
}
