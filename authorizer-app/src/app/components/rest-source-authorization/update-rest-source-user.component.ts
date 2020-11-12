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
import { Location } from '@angular/common';
import { RequestTokenPayload } from 'src/app/models/auth.model';
import { RestSourceUser } from '../../models/rest-source-user.model';
import { RestSourceUserService } from '../../services/rest-source-user.service';

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

  constructor(
    private restSourceUserService: RestSourceUserService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private _location: Location
  ) {}

  ngOnInit() {
    this.loadAllRestSourceProjects();
  }

  initialize() {
    this.activatedRoute.params.subscribe(params => {
      if (params.hasOwnProperty('id')) {
        this.restSourceUserService.getUserById(params['id']).subscribe(
          user => {
            console.log('user ', user);
            this.restSourceUser = user;
            console.log('rest user ', this.restSourceUser);
            this.subjects = [{ id: this.restSourceUser.userId }];
            this.isEditing = true;
            this.startDate = new Date(this.restSourceUser.startDate);
            this.endDate = new Date(this.restSourceUser.endDate);
            // this.onChangeProject(this.restSourceUser.projectId)
          },
          (err: Response) => {
            console.log('Cannot retrieve current user details', err);
            this.errorMessage = 'Cannot retrieve current user details';
            window.setTimeout(() => this.router.navigate(['']), 5000);
          }
        );
      } else {
        this.activatedRoute.queryParams.subscribe((params: Params) => {
          if (params.hasOwnProperty('error')) {
            this.errorMessage = params['error_description'];
          } else {
            this.errorMessage = null;
            this.handleAuthRedirect(params);
          }
        });
      }
    });
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
          if (err.status == 417) {
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
        this.restSourceProjects = data.projects;
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
        this.subjects = data.users;
      },
      () => {
        this.errorMessage = 'Cannot load registered users!';
      }
    );
  }

  cancelUpdateUser() {
    return this.router.navigate(['/users']);
  }

  backClicked() {
    this._location.back();
  }
}
