import {
  AfterViewInit,
  Component,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
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
import { BehaviorSubject, combineLatest, of, Subscription, Observable } from 'rxjs';
import { PageEvent } from '@angular/material/paginator';
import {
  catchError,
  debounceTime,
  distinctUntilChanged,
  filter,
  switchMap,
} from 'rxjs/operators';
import {RestSourceUserMockService} from '../../services/rest-source-user-mock.service';

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

  @Input('project') set project(project: string) { this.project$.next(project); }
  private project$ = new BehaviorSubject<string>('');

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
    // private restSourceUserService: RestSourceUserMockService,
    public dialog: MatDialog,
  ) {}

  ngOnInit() {
    this.dataSource = new MatTableDataSource([]);
  }

  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
    this.paginator.pageSize = 50;
    this.paginator.page.subscribe(next => this.page.next(next));
    this.subscribeToUsers();
  }

  subscribeToUsers(): Subscription {
    const filterInput = this.filterValue
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
      );
    const projectInput = this.project$
      .pipe(
        filter(p => !!p),
        distinctUntilChanged(),
      );
    const pageInput = this.page
      .pipe(
        distinctUntilChanged((p1, p2) =>
          p1.pageIndex === p2.pageIndex && p1.pageSize === p2.pageSize)
      );
    return combineLatest<PageEvent, string, string, string>(pageInput, filterInput, this.updateTrigger, projectInput)
      .pipe(
        switchMap(([page, filterValue, _, project]) => this.loadUsers(filterValue, page, project))
      )
      .subscribe(users => {
        console.log(users);
        this.dataSource.data = users.users;
        this.paginator.pageIndex = users.metadata.pageNumber - 1;
        this.paginator.length = users.metadata.totalElements;
      });
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

  loadUsers(filterValue: string, page: PageEvent, project: string): Observable<RestSourceUsers> {
    const params = {
      page: page.pageIndex + 1,
      size: page.pageSize
    };

    if (filterValue) {
      params['search'] = filterValue;
    }

    return this.restSourceUserService.getAllUsersOfProject(project, params)
      .pipe(
        catchError(() => of({users: [], metadata: { pageNumber: 1, pageSize: page.pageSize, totalElements: 0 }} as RestSourceUsers)),
      );
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

  registerUser() {

  }
}
