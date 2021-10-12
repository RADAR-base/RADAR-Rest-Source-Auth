import {Component, Inject, OnDestroy} from '@angular/core';
import {DatePipe} from "@angular/common";
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {Subject, takeUntil} from "rxjs";
import {TranslateService} from "@ngx-translate/core";

import { UserService } from "@app/admin/services/user.service";
import { RestSourceUser } from '@app/admin/models/rest-source-user.model';

import { environment } from "@environments/environment";

// import {
//   MAT_MOMENT_DATE_FORMATS,
//   MomentDateAdapter,
//   MAT_MOMENT_DATE_ADAPTER_OPTIONS,
// } from '@angular/material-moment-adapter';
// import {DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE} from '@angular/material/core';
// import 'moment/locale/ja';
// import 'moment/locale/en-gb';
// import 'moment/locale/nl';
// import 'moment/locale/fr';

export enum SubjectDialogMode {
  ADD = 'add',
  VIEW = 'view',
  EDIT = 'edit',
  DELETE = 'delete'
}
@Component({
  selector: 'app-user-dialog',
  templateUrl: 'user-dialog.component.html',
  styleUrls: ['user-dialog.component.scss'],
  // providers: [
  //   // The locale would typically be provided on the root module of your application. We do it at
  //   // the component level here, due to limitations of our example generation script.
  //   {provide: MAT_DATE_LOCALE, useValue: 'en-GB'},
  //
  //   // `MomentDateAdapter` and `MAT_MOMENT_DATE_FORMATS` can be automatically provided by importing
  //   // `MatMomentDateModule` in your applications root module. We provide it at the component level
  //   // here, due to limitations of our example generation script.
  //   {
  //     provide: DateAdapter,
  //     useClass: MomentDateAdapter,
  //     deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS]
  //   },
  //   {provide: MAT_DATE_FORMATS, useValue: MAT_MOMENT_DATE_FORMATS},
  // ],
})

export class UserDialogComponent implements OnDestroy {
  linkGeneratingLoading = false;
  authorizationLoading = false;
  updateLoading = false;
  deleteLoading = false;

  error?: any;

  SubjectDialogMode = SubjectDialogMode;
  mode = this.data.mode;
  subject = this.data.subject;

  linkForUser?: string;

  form = this.fb.group({
    // sourceType: [{value: this.subject.sourceType, disabled: true}, Validators.required],
    // projectId: [{value: this.subject.projectId, disabled: true}],
    // userId: [{value: this.subject.userId, disabled: true}],
    // externalId: [{value: this.subject.externalId, disabled: true}],
    startDate: [{value: this.subject.startDate, disabled: this.mode == SubjectDialogMode.VIEW}, Validators.required],
    endDate: [{value: this.subject.endDate, disabled: this.mode == SubjectDialogMode.VIEW}],
    // isAuthorized: [{value: this.subject.isAuthorized, disabled: true}],
    // sourceId: [{value: this.subject.sourceId, disabled: true}],
    // serviceUserId: [{value: this.subject.serviceUserId, disabled: true}],
    // hasValidToken: [{value: this.subject.hasValidToken, disabled: true}],
    // timesReset: [{value: this.subject.timesReset, disabled: true}],
  });

  unsubscribe: Subject<void> = new Subject<void>();

  linkForUserMessage = '';
  linkForUserExpirationDate = '';

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    // private datePipe: LocalizedDatePipe,
    private datePipe: DatePipe,
    public dialogRef: MatDialogRef<UserDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {subject: RestSourceUser; mode: string},
    private translate: TranslateService
  ) {
    this.translate.onLangChange.pipe(
      takeUntil(this.unsubscribe)
    ).subscribe(() => {
      this.getAndInitTranslations();
    });

    this.getAndInitTranslations();
  }

  ngOnDestroy() {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  onSubmit(submitterName: string): void {
    this.error = null;
    const persistent = submitterName === 'link';
    this.linkGeneratingLoading = persistent;
    this.authorizationLoading = !persistent;
    const user = {
      projectId: this.subject.projectId,
      userId: this.subject.userId,
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate ? this.form.value.endDate : null,
      sourceType: this.subject.sourceType,
    };
    if (this.subject.id) {
      // this.updateLoading = true;
      const updatedUser = {
        ...this.subject,
        startDate: this.form.value.startDate,
        endDate: this.form.value.endDate ? this.form.value.endDate : null,
      };
      this.userService.resetUser(updatedUser)
        .subscribe({
          next: () => {
            this.registerUser(this.subject.id!!, persistent);
            // this.dialogRef.close('updated');
          },
          error: (error) => {
            this.error = this.handleError(error);
          },
          complete: () => this.updateLoading = false
        });
    } else {
      this.userService.createUser(user).subscribe({
          next: (resp) => {
            this.registerUser(resp.id, persistent);
          },
          error: (error) => {
            this.error = this.handleError(error);
            this.linkGeneratingLoading = false;
            this.authorizationLoading = false;
          }
        }
      );
    }
  }

  updateUser(user: RestSourceUser) {
    this.updateLoading = true;
    const updatedUser = {
      ...user,
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate ? this.form.value.endDate : null,
    };
    this.userService.resetUser(updatedUser)
      .subscribe({
        next: () => {
          this.dialogRef.close('updated');
        },
        error: (error) => {
          this.error = this.handleError(error);
        },
        complete: () => this.updateLoading = false
      });
  }


  registerUser(userId: string, persistent: boolean): void {
    this.userService.registerUser({userId, persistent}).subscribe({
        next: (registrationResp) => {
          if (registrationResp.authEndpointUrl) {
            window.location.href = registrationResp.authEndpointUrl;
          } else if (registrationResp.secret) {
            const datePipe: DatePipe = new DatePipe(this.translate.currentLang);
            const date = datePipe.transform(new Date(registrationResp.expiresAt), 'EEEE, MMMM d, yyyy hh:mm:ss a');
            this.linkForUser =
              `${this.linkForUserMessage}\n\n`
              +`${environment.radarBaseUrl}${environment.BASE_HREF}users:auth?token=${registrationResp.token}&secret=${registrationResp.secret}\n\n`
              +`${this.linkForUserExpirationDate}: ${date}`;
            this.linkGeneratingLoading = false;
            //todo update ui
          }
        },
        error: (error) => {
          this.error = this.handleError(error);
          this.linkGeneratingLoading = false;
          this.authorizationLoading = false;
        }
      }
    );
  }

  closeDialog(): void {
    this.dialogRef.close('updated');
  }

  handleError(error: any): any {
    this.error = error;
    if(error.error.error === 'invalid_token' && error.status === 401) {
      this.dialogRef.close('error');
    }
  }

  getAndInitTranslations() {
    this.translate
      .get([
        'ADMIN.USER_DIALOG.linkForUserMessage',
        'ADMIN.USER_DIALOG.linkForUserExpirationDate'
      ]).pipe(
      takeUntil(this.unsubscribe)
    ).subscribe(translation => {
      this.linkForUserMessage = translation['ADMIN.USER_DIALOG.linkForUserMessage'];
      this.linkForUserExpirationDate = translation['ADMIN.USER_DIALOG.linkForUserExpirationDate'];
    });
  }

  closeDeleteDialog(user?: RestSourceUser): void {
    this.error = undefined;
    // this.userService.deleteUser(user.id).subscribe({
    //   next: () => {
    //     console.log(`user ${user.id} deleted.`)
    //     this.changeProject(this.form.value.projectId)
    //   }
    // })
    if(!user || !user.id){
      return this.dialogRef.close('closed');
      // return;
    }
    this.deleteLoading = true;
    this.userService.deleteUser(user.id)
      .subscribe({
        next: () => {
          this.dialogRef.close('deleted');
        },
        error: (error) => {
          this.handleError(error);
          // console.log(error);
          // this.error = error.error.description || error.message;
          // if(error.error === 'invalid_token' && error.status === 401) {
          //   this.dialogRef.close('error');
          // }
          // url: "https://radar-k3s-test.thehyve.net/managementportal/oauth/token"
        },
        complete: () => this.deleteLoading = false
      });
    // this.dialogRef.close();
  }
}
