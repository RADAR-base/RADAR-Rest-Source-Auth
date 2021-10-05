import { ActivatedRoute, Params, Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import {
  NgbDateAdapter,
  NgbDateNativeAdapter
} from '@ng-bootstrap/ng-bootstrap';
import {
  RadarProject,
  RadarSubject
} from '../../models/rest-source-project.model';

import { HttpErrorResponse } from '@angular/common/http';
import {DatePipe, Location, PlatformLocation} from '@angular/common';
import { RequestTokenPayload } from 'src/app/models/auth.model';
import { RestSourceUser } from '../../models/rest-source-user.model';
import { RestSourceUserService } from '../../services/rest-source-user.service';
import {RestSourceUserMockService} from '../../services/rest-source-user-mock.service';
import {environment} from '../../../environments/environment';

@Component({
  selector: 'update-rest-source-user',
  templateUrl: './update-rest-source-user.component.html',
  providers: [{ provide: NgbDateAdapter, useClass: NgbDateNativeAdapter }]
})
export class UpdateRestSourceUserComponent implements OnInit {
  isEditing = false;
  errorMessage?: string;
  startDate: Date;
  endDate: Date;
  restSourceUser: RestSourceUser;
  subjects: RadarSubject[] = [];
  restSourceProjects: RadarProject[] = [];

  linkGeneratingLoading = false;
  authorizationLoading = false;

  linkForUser?: string;

  constructor(
    private restSourceUserService: RestSourceUserService,
    // private restSourceUserService: RestSourceUserMockService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private _location: Location,
    private datePipe: DatePipe,
    private platformLocation: PlatformLocation
  ) {}

  ngOnInit() {
    this.loadAllRestSourceProjects();
  }

  initialize() {
    const {params, queryParams} = this.activatedRoute.snapshot;

    if (params.hasOwnProperty('id')) {
      this.restSourceUserService.getUserById(params['id']).subscribe(
        user => {
          this.restSourceUser = user;
          this.subjects = [{ id: this.restSourceUser.userId }];
          // this.isEditing = false; // true;
          this.startDate = new Date(this.restSourceUser.startDate);
          this.endDate = new Date(this.restSourceUser.endDate);
          this.loadAllSubjectsOfProject(user.projectId);
        },
        (err: Response) => {
          console.log('Cannot retrieve current user details', err);
          this.errorMessage = 'Cannot retrieve current user details';
          window.setTimeout(() => this.router.navigate(['']), 5000);
        }
      );
      this.isEditing = queryParams.hasOwnProperty('link');
    } else {
      this.activatedRoute.queryParams.subscribe((_params: Params) => {
        if (_params.hasOwnProperty('error')) {
          this.errorMessage = 'Access Denied';
          window.setTimeout(() => this.router.navigate(['']), 5000);
        } else {
          this.errorMessage = null;
          this.handleAuthRedirect(_params);
        }
      });
    }

    // if (params.hasOwnProperty('id') && queryParams.hasOwnProperty('link')) {
    //   console.log('has id & link');
    //   this.isEditing = true;
    // } else {
    //   console.log(' doesn\'t have id or link');
    //   this.isEditing = false;
    // }
    //
    // this.activatedRoute.params.subscribe(params => {
    //   console.log(params);
    //   if (params.hasOwnProperty('id')) {
    //     console.log('has id');
    //     this.restSourceUserService.getUserById(params['id']).subscribe(
    //       user => {
    //         this.restSourceUser = user;
    //         this.subjects = [{ id: this.restSourceUser.userId }];
    //         this.isEditing = false; // true;
    //         this.startDate = new Date(this.restSourceUser.startDate);
    //         this.endDate = new Date(this.restSourceUser.endDate);
    //         this.loadAllSubjectsOfProject(user.projectId);
    //       },
    //       (err: Response) => {
    //         console.log('Cannot retrieve current user details', err);
    //         this.errorMessage = 'Cannot retrieve current user details';
    //         window.setTimeout(() => this.router.navigate(['']), 5000);
    //       }
    //     );
    //   } else {
    //     this.activatedRoute.queryParams.subscribe((_params: Params) => {
    //       if (_params.hasOwnProperty('error')) {
    //         this.errorMessage = 'Access Denied';
    //         window.setTimeout(() => this.router.navigate(['']), 5000);
    //       } else {
    //         this.errorMessage = null;
    //         this.handleAuthRedirect(_params);
    //       }
    //     });
    //   }
    // });
  }

  handleAuthRedirect(params) {
    this.addRestSourceUser(params);
  }

  updateRestSourceUser() {
    if (!this.endDate || !this.startDate) {
      this.errorMessage = 'Please select Start Date and End Date';
      return;
    }
    if (this.endDate <= this.startDate) {
      this.errorMessage = 'Please set the end date later than the start date.';
      return;
    }
    try {
      this.restSourceUser.startDate = this.startDate.toISOString();
      this.restSourceUser.endDate = this.endDate.toISOString();
    } catch (err) {
      this.errorMessage = 'Please enter valid Start Date and End Date';
      return;
    }
    this.restSourceUserService.updateUser(this.restSourceUser).subscribe(
      () => {
        return this.router.navigate(['/users'], {
          queryParams: { project: this.restSourceUser.projectId }
        });
      },
      (err: HttpErrorResponse) => {
        if (err.error instanceof ErrorEvent) {
          // A client-side or network error occurred. Handle it accordingly.
          this.errorMessage =
            'Something went wrong. Please check your connection.';
        } else {
          // The backend returned an unsuccessful response code.
          // The response body may contain clues as to what went wrong,
          this.errorMessage = `Backend Error: Status=${err.status},
          Body: ${err.error.error}, ${err.error.message}`;
          if (err.status === 417) {
            this.errorMessage +=
              ' Please check the details are correct and try again.';
          }
        }
      }
    );
  }

  private addRestSourceUser(payload: RequestTokenPayload) {
    this.restSourceUserService.addAuthorizedUser(payload).subscribe(
      data => {
        this.onChangeProject(data.projectId);
        this.restSourceUser = data;
      },
      (err: HttpErrorResponse) => {
        this.errorMessage =
          err.statusText + ' : ' + err.error.error_description;
        window.setTimeout(() => this.router.navigate(['']), 10000);
      }
    );
  }

  private loadAllRestSourceProjects() {
    this.restSourceUserService.getAllProjects().subscribe(
      (data: any) => {
        console.log(data);
        this.restSourceProjects = data; // .projects;
        this.initialize();
      },
      () => {
        this.errorMessage = 'Cannot load projects!';
      }
    );
  }

  onChangeProject(projectId: string) {
    if (projectId) {
      this.loadAllSubjectsOfProject(projectId);
    }
  }

  private loadAllSubjectsOfProject(projectId: string) {
    this.restSourceUserService.getAllSubjectsOfProjects(projectId).subscribe(
      (data: any) => {
        this.subjects = data; // .users;
        let withExternalId = this.subjects.filter(s => s.externalId);
        let withoutExternalId = this.subjects.filter(s => !s.externalId);
        withExternalId = withExternalId.sort((a, b) => {
          return a.externalId < b.externalId ? -1 : 1;
        });
        withoutExternalId = withoutExternalId.sort((a, b) => {
          return a.id < b.id ? -1 : 1;
        });
        this.subjects = [...withExternalId, ...withoutExternalId];
      },
      () => {
        this.errorMessage = 'Cannot load registered users!';
      }
    );
  }

  onSubmit(actionName: string): void {
    // alert('Thanks!');
    this.errorMessage = null;

    // console.log(e.submitter.name);
    // console.log(this.form.value);
    // console.log(this.form.value.startDate.unix());
    // console.log(this.form.value.startDate.valueOf());
    const persistent = actionName === 'link';
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
    // const user = this.restSourceUser;
    // const user = {
    //   projectId: this.restSourceUser.projectId, // this.form.value.projectId,
    //   userId: this.restSourceUser.userId, // this.form.value.userId,
    //   // sourceId: '789', // this.form.value.sourceId,
    //   startDate: this.restSourceUser.startDate, // this.form.value.startDate, // .valueOf(),
    //   endDate: this.restSourceUser.endDate ? this.restSourceUser.endDate : null,
    //   // this.form.value.endDate ? this.form.value.endDate : null, // .valueOf() : null,
    //   sourceType: this.restSourceUser.sourceType, // this.form.value.sourceType,
    // };

    this.restSourceUserService.registerUser({userId: this.restSourceUser.id, persistent}).subscribe(
      registrationResp => {
        console.log(registrationResp);
        if (registrationResp.authEndpointUrl) {
          // redirect to endpointUrl
          window.location.href = registrationResp.authEndpointUrl;
        } else if (registrationResp.secret) {
          // generate link // show // copy to clipboard

          const currentAbsoluteUrl = window.location.href;
          const currentRelativeUrl = this.router.url;
          const index = currentAbsoluteUrl.indexOf(currentRelativeUrl);
          const baseUrl = currentAbsoluteUrl.substring(0, index);

          this.linkForUser =
              `${baseUrl}/users:auth?token=${registrationResp.token}&secret=${registrationResp.secret}\n\n`
              +`Expiration date: ${this.datePipe.transform(new Date(registrationResp.expiresAt), 'EEEE, MMMM d, yyyy hh:mm:ss a')}`;
          this.linkGeneratingLoading = false;
        }
      },
      err => {
        console.log(err);
        this.errorMessage = err.error.error_description; // err.message;
        this.linkGeneratingLoading = false;
        this.authorizationLoading = false;
      }
    );
    //
    // this.restSourceUserService.createUser(user).subscribe(
    //   resp => {
    //     console.log(resp);
    //
    //   },
    //   err => {
    //     console.log(err);
    //     if (err.status === 409 && err.error.error === 'user_exists') {
    //       this.restSourceUserService.registerUser({userId: '11', persistent}).subscribe(
    //         registrationResp => {
    //           console.log(registrationResp);
    //           if (registrationResp.authEndpointUrl) {
    //             // redirect to endpointUrl
    //             window.location.href = registrationResp.authEndpointUrl;
    //           } else if (registrationResp.secret) {
    //             // generate link // show // copy to clipboard
    //             this.linkForUser =
    //               `${environment.baseUrl}/users:auth?token=${registrationResp.token}&secret=${registrationResp.secret}`;
    //             this.linkGeneratingLoading = false;
    //           }
    //         },
    //         error => {
    //           console.log(error);
    //           this.errorMessage = err.error.error_description; // err.message;
    //           this.linkGeneratingLoading = false;
    //           this.authorizationLoading = false;
    //         }
    //       );
    //     } else {
    //       this.errorMessage = err.error.error_description; // message;
    //       this.linkGeneratingLoading = false;
    //       this.authorizationLoading = false;
    //     }
    //   }
    // );
  }

  // cancelUpdateUser() {
  //   return this.router.navigate(['/users']);
  // }

  backClicked() {
    this._location.back();
  }

  copyInputMessage(inputElement: HTMLTextAreaElement) {
    inputElement.select();
    document.execCommand('copy');
    inputElement.setSelectionRange(0, 0);
  }
}
