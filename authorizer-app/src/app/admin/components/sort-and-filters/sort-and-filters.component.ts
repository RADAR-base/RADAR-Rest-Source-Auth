import {AfterViewInit, Component, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {MatPaginator} from '@angular/material/paginator';
import {MatTableDataSource} from '@angular/material/table';
import {MatDialog} from '@angular/material/dialog';
import {MatSort} from '@angular/material/sort';
import {BehaviorSubject, combineLatest, distinctUntilChanged, filter, Observable, switchMap} from "rxjs";
import {map} from "rxjs/operators";
import {SubjectService} from "../../services/subject.service";
import {UserService} from "../../services/user.service";
import {UserDialogComponent} from "../../containers/user-dialog/user-dialog.component";
import {UserDeleteDialog} from "../../containers/user-delete-dialog/user-delete-dialog.component";
import {RadarProject, RadarSourceType} from "../../models/rest-source-project.model";
import {RestSourceUser} from '../../models/rest-source-user.model';
import {MatBottomSheetRef} from "@angular/material/bottom-sheet";

@Component({
  selector: 'app-sort-and-filters',
  templateUrl: 'sort-and-filters.component.html',
  styleUrls: ['sort-and-filters.component.scss']
})
export class SortAndFiltersComponent {
  constructor(private _bottomSheetRef: MatBottomSheetRef<SortAndFiltersComponent>) {}

  openLink(event: any): void {
    this._bottomSheetRef.dismiss();
    event.preventDefault();
  }
}
