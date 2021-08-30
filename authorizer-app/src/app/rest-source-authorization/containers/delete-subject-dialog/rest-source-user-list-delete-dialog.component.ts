import {
  Component,
  Inject,
} from '@angular/core';
// import {
//   MAT_DIALOG_DATA,
//   MatDialogRef,
// } from '@angular/material';
import { RestSourceUser } from '../../models/rest-source-user.model';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';

@Component({
  selector: 'app-rest-source-user-list-delete-dialog',
  templateUrl: 'rest-source-user-list-delete-dialog.html'
})
export class RestSourceUserListDeleteDialog {
  constructor(
    public dialogRef: MatDialogRef<RestSourceUserListDeleteDialog>,
    @Inject(MAT_DIALOG_DATA) public data: RestSourceUser
  ) {}

  closeDeleteDialog(): void {
    this.dialogRef.close();
  }
}
