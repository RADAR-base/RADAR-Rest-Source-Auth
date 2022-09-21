import { Component, Inject } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, Validators } from '@angular/forms';
import { DateAdapter } from "@angular/material/core";
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TranslateService } from "@ngx-translate/core";

import { UserService } from "@app/admin/services/user.service";
import { RestSourceUser } from '@app/admin/models/rest-source-user.model';
import {LocaleService} from "@app/admin/services/locale.service";
import {LANGUAGES} from "@app/app.module";

export enum UserDialogMode {
  ADD = 'add',
  VIEW = 'view',
  EDIT = 'edit',
  DELETE = 'delete'
}

export enum UserDialogCommand {
  UPDATED = 'updated',
  DELETED = 'deleted',
  ERROR = 'error',
}

@Component({
  selector: 'app-user-dialog',
  templateUrl: 'user-dialog.component.html',
  styleUrls: ['user-dialog.component.scss'],
})

export class UserDialogComponent {
  SubjectDialogMode = UserDialogMode;

  isGenerateUrlLoading = false;
  isAuthorizeLoading = false;
  isUpdateLoading = false;
  isDeleteLoading = false;
  error?: string;

  mode = this.data.mode as UserDialogMode;
  subject = this.data.subject;

  form = this.fb.group({
    startDate: [{value: this.subject.startDate, disabled: this.mode == UserDialogMode.VIEW}, Validators.required],
    endDate: [{value: this.subject.endDate, disabled: this.mode == UserDialogMode.VIEW}],
  });
  dateFormat = '';

  messageForUserLink?: string;
  messageForUserExpirationDate?: Date;

  constructor(
    private router: Router,
    private fb: FormBuilder,
    private userService: UserService,
    public dialogRef: MatDialogRef<UserDialogComponent>,
    private translate: TranslateService,
    private _adapter: DateAdapter<any>,
    @Inject(MAT_DIALOG_DATA) public data: {subject: RestSourceUser; mode: string},
    private session: LocaleService
  ) {
    this.initLocale();
  }

  //#region Dialog Actions
  authorize(): void {
    this.error = undefined;
    const persistent = false;
    this.isGenerateUrlLoading = persistent;
    this.isAuthorizeLoading = !persistent;
    if (this.subject.id) {
      this.updateAndRegisterUser(persistent);
    } else {
      this.createAndRegisterUser(persistent);
    }
  }

  generateLink(): void {
    this.error = undefined;
    const persistent = true;
    this.isGenerateUrlLoading = persistent;
    this.isAuthorizeLoading = !persistent;
    if (this.subject.id) {
      this.updateAndRegisterUser(persistent);
    } else {
      this.createAndRegisterUser(persistent);
    }
  }

  update(): void {
    this.isUpdateLoading = true;
    const updatedUser = {
      ...this.subject,
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate ? this.form.value.endDate : null,
    };
    this.userService.updateUser(updatedUser)
      .subscribe({
        next: () => {
          this.dialogRef.close(UserDialogCommand.UPDATED);
          this.isUpdateLoading = false;
        },
        error: (error) => {
          this.error = this.handleError(error);
          this.isUpdateLoading = false;
        },
      });
  }

  delete(): void {
    this.error = undefined;
    this.isDeleteLoading = true;
    if (this.subject.id) {
      this.userService.deleteUser(this.subject.id)
        .subscribe({
          next: () => {
            this.dialogRef.close(UserDialogCommand.DELETED);
            this.isDeleteLoading = false;
          },
          error: (error) => {
            this.isDeleteLoading = false;
            this.handleError(error);
          },
        });
    }
  }

  close(mode: UserDialogMode): void {
    if (mode !== UserDialogMode.ADD) {
      this.dialogRef.close();
    } else {
      if (this.messageForUserLink) {
        this.dialogRef.close(UserDialogCommand.UPDATED);
      } else {
        this.dialogRef.close();
      }
    }
  }

  private createAndRegisterUser(persistent: boolean): void {
    const user = {
      projectId: this.subject.projectId,
      userId: this.subject.userId,
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate ? this.form.value.endDate : null,
      sourceType: this.subject.sourceType,
    };
    this.userService.createUser(user).subscribe({
        next: (resp) => {
          this.registerUser(resp.id, persistent);
        },
        error: (error) => {
          this.error = this.handleError(error);
          this.isGenerateUrlLoading = false;
          this.isAuthorizeLoading = false;
        }
      }
    );
  }

  private updateAndRegisterUser(persistent: boolean){
    const updatedUser = {
      ...this.subject,
      startDate: this.form.value.startDate,
      endDate: this.form.value.endDate ? this.form.value.endDate : null,
    };
    this.userService.updateUser(updatedUser)
      .subscribe({
        next: () => {
          this.registerUser(this.subject.id!!, persistent);
        },
        error: (error) => {
          this.error = this.handleError(error);
          this.isGenerateUrlLoading = false;
          this.isAuthorizeLoading = false;
        }
      });
  }

  private registerUser(userId: string, persistent: boolean): void {
    this.userService.registerUser({userId, persistent})
      .subscribe({
        next: (resp) => {
          if (resp.authEndpointUrl) {
            this.userService.storeUserAuthParams(resp.authEndpointUrl);
            window.location.href = resp.authEndpointUrl;
          } else if (resp.secret) {
            const baseUrl = this.getBaseUrl();
            this.messageForUserLink = `${baseUrl}/users:auth?token=${resp.token}&secret=${resp.secret}`;
            this.messageForUserExpirationDate = new Date(resp.expiresAt);
            this.isGenerateUrlLoading = false;
          }
        },
        error: (error) => {
          this.error = this.handleError(error);
          this.isGenerateUrlLoading = false;
          this.isAuthorizeLoading = false;
        }
      }
    );
  }

  private handleError(error: any): string {
    if(error.error.error === 'invalid_token' && error.status === 401) {
      this.dialogRef.close(UserDialogCommand.ERROR);
    }
    return error.error.error_description || error.message || error;
  }
  //#endregion

  //#region Locale
  private initLocale() {
    const locale = this.session.locale;
    this._adapter.setLocale(locale);
    this.dateFormat = LANGUAGES.filter(lang => lang.locale === locale)[0].dateFormat; // getLocaleDateFormat( locale, FormatWidth.Short );
  }
  //#endregion

  private getBaseUrl(): string {
    const currentAbsoluteUrl = window.location.href;
    const currentRelativeUrl = this.router.url;
    const index = currentAbsoluteUrl.indexOf(currentRelativeUrl);
    return currentAbsoluteUrl.substring(0, index);
  }
}
