import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';

import { RestSourceUser } from '@app/admin/models/rest-source-user.model';
import {UserService} from "@app/admin/services/user.service";

@Component({
  selector: 'app-delete-dialog',
  templateUrl: 'user-delete-dialog.component.html'
})
export class UserDeleteDialog {

  subject = this.data.subject;
  deleteLoading = false;
  error?: string;

  constructor(
    private userService: UserService,
    public dialogRef: MatDialogRef<UserDeleteDialog>,
    @Inject(MAT_DIALOG_DATA) public data: {subject: RestSourceUser;},
  ) {}

  closeDeleteDialog(user?: RestSourceUser): void {
    this.error = undefined;
    // this.userService.deleteUser(user.id).subscribe({
    //   next: () => {
    //     console.log(`user ${user.id} deleted.`)
    //     this.changeProject(this.form.value.projectId)
    //   }
    // })
    if(!user || !user.id){
      return;
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

  handleError(error: any): void {
    console.log(error);
    this.error = error.error.description || error.message;
    if(error.error.error === 'invalid_token' && error.status === 401) {
      this.dialogRef.close('error');
    }
  }
}
