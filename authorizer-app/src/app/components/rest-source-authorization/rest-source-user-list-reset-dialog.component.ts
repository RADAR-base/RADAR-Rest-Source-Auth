import {
  Component,
  Inject,
} from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDatepickerInputEvent,
  MatDialogRef
} from '@angular/material';
import { RestSourceUser } from '../../models/rest-source-user.model';
import {FormControl} from '@angular/forms';
import * as moment from 'moment';
import deepcopy from 'ts-deepcopy';
import {RestSourceUserListDeleteDialog} from "./rest-source-user-list-delete-dialog.component";

@Component({
  selector: 'rest-source-user-list-reset-dialog',
  templateUrl: 'rest-source-user-list-reset-dialog.html'
})

export class RestSourceUserListResetDialog {
  startDateFormControl: FormControl;
  endDateFormControl: FormControl;

  // Stores a copy of the data so as to not modify the original content
  dataCopy: RestSourceUser;

  constructor(
    public dialogRef: MatDialogRef<RestSourceUserListDeleteDialog>,
    @Inject(MAT_DIALOG_DATA) public data: RestSourceUser
  ) {
    this.startDateFormControl = new FormControl(moment(this.data.startDate));
    this.endDateFormControl = new FormControl(moment(this.data.endDate));
    this.dataCopy = deepcopy(this.data);
  }

  closeResetDialog(): void {
    this.dialogRef.close();
  }

  updateStartDateValue(event: MatDatepickerInputEvent<any>) {
    this.dataCopy.startDate = event.value.toISOString();
  }

  updateEndDateValue(event: MatDatepickerInputEvent<any>) {
    this.dataCopy.endDate = event.value.toISOString();
  }
}
