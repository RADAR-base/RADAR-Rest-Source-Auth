import { AfterViewInit, Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import {
  MatDialog,
  MatPaginator,
  MatSort,
  MatTableDataSource
} from '@angular/material';
import {
  RestSourceUser, RestSourceUsers
} from '../../models/rest-source-user.model';

import { RestSourceUserListDeleteDialog } from './rest-source-user-list-delete-dialog.component';
import { RestSourceUserListResetDialog } from './rest-source-user-list-reset-dialog.component';
import { RestSourceUserService } from '../../services/rest-source-user.service';
import { BehaviorSubject, combineLatest, of } from 'rxjs';
import { PageEvent } from '@angular/material/paginator';
import {
  catchError,
  debounceTime,
  distinctUntilChanged
} from 'rxjs/operators';

@Component({
  selector: 'rest-source-list',
  templateUrl: './rest-source-user-list.component.html',
  styleUrls: ['./rest-source-user-list.component.css']
})
export class RestSourceUserListComponent implements OnInit, AfterViewInit, OnDestroy {
  columnsToDisplay = [
    'id',
    'userId',
    'externalUserId',
    'sourceType',
    'startDate',
    'endDate',
    'authorized',
    'actions'
  ];

  @Input() project: string;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  updateTrigger = new BehaviorSubject<string>('init');
  filterValue = new BehaviorSubject<string>('');
  page = new BehaviorSubject<PageEvent>({
    pageIndex: 0,
    pageSize: 50,
    length: 0,
    previousPageIndex: 0,
  });

  dataSource: MatTableDataSource<RestSourceUser>;

  constructor(
    private restSourceUserService: RestSourceUserService,
    public dialog: MatDialog,
  ) {}

  ngOnInit() {
    this.dataSource = new MatTableDataSource([]);
  }

  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
    this.paginator.pageSize = 50;

    const filterInput = this.filterValue
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
      );
    combineLatest(this.page, filterInput, this.updateTrigger)
      .subscribe(([page, filterValue, _]: [PageEvent, string, string]) => this.loadUsers(filterValue, page));

    this.paginator.page.subscribe(next => this.page.next(next));
  }

  ngOnDestroy() {
    this.updateTrigger.complete();
    this.filterValue.complete();
    this.page.complete();
  }

  applyFilter(filterValue: string) {
    const trimmedValue = filterValue.trim();
    if (trimmedValue.length <= 1) {
      this.filterValue.next('');
    } else {
      this.filterValue.next(trimmedValue);
    }
  }

  loadUsers(filterValue: string, page: PageEvent) {
    const params = {
      page: page.pageIndex + 1,
      size: page.pageSize
    };

    if (filterValue) {
      params['search'] = filterValue;
    }

    this.restSourceUserService.getAllUsersOfProject(this.project, params)
      .pipe(
        catchError(() => of({users: [], metadata: { pageNumber: 1, pageSize: page.pageSize, totalElements: 0 }} as RestSourceUsers)),
      )
      .subscribe(users => {
        this.dataSource.data = users.users;
        this.paginator.pageIndex = users.metadata.pageNumber - 1;
        this.paginator.length = users.metadata.totalElements;
      });
  }

  removeUser(restSourceUser: RestSourceUser) {
    this.restSourceUserService.deleteUser(restSourceUser.id)
      .subscribe(() => this.updateTrigger.next('delete'));
  }

  resetUser(restSourceUser: RestSourceUser) {
    this.restSourceUserService.resetUser(restSourceUser)
      .subscribe(() => this.updateTrigger.next('reset'));
  }

  openDeleteDialog(restSourceUser: RestSourceUser) {
    const dialogRef = this.dialog.open(RestSourceUserListDeleteDialog, {
      data: restSourceUser
    });

    dialogRef.afterClosed().subscribe(user => {
      if (user) {
        console.log('Deleting user...', user);
        this.removeUser(user);
      }
    });
  }

  openResetDialog(restSourceUser: RestSourceUser) {
    const dialogRef = this.dialog.open(RestSourceUserListResetDialog, {
      data: restSourceUser
    });

    dialogRef.afterClosed().subscribe((user: RestSourceUser) => {
      if (user) {
        console.log('Resetting user...', user);
        this.resetUser(user);
      }
    });
  }
}
