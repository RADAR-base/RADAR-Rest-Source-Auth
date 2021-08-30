import {
  AfterViewInit,
  Component, Inject,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from '@angular/core';
// import {
//   MatDialog,
//   MatPaginator,
//   MatSort,
//   MatTableDataSource
// } from '@angular/material';
// import {
//   RestSourceUser, RestSourceUsers
// } from '../../models/rest-source-user.model';

// import { RestSourceUserListDeleteDialog } from './rest-source-user-list-delete-dialog.component';
// import { RestSourceUserListResetDialog } from './rest-source-user-list-reset-dialog.component';
import { RestSourceUserService } from '../../services/rest-source-user.service';
// import { BehaviorSubject, combineLatest, of, Subscription, Observable } from 'rxjs';
import {MatPaginator, PageEvent} from '@angular/material/paginator';
// import {
//   catchError,
//   debounceTime,
//   distinctUntilChanged,
//   filter,
//   switchMap,
// } from 'rxjs/operators';
// import {RestSourceUserMockService} from '../../services/rest-source-user-mock.service';
import {MatTable, MatTableDataSource} from '@angular/material/table';
import {MAT_DIALOG_DATA, MatDialog} from '@angular/material/dialog';
// import {RestSourceUserListDeleteDialog} from '../../containers/delete-subject-dialog/rest-source-user-list-delete-dialog.component';
// import {RestSourceUserListResetDialog} from '../../containers/update-subject-dialog/rest-source-user-list-reset-dialog.component';
import {MatSort} from '@angular/material/sort';
// import {RestSourceUserListResetDialog} from '../../containers/subject-reset-dialog/rest-source-user-list-reset-dialog.component';
import {TableDataSource, TableItem} from './table-datasource';
import {RestSourceUser} from '../../models/rest-source-user.model';
import {SubjectDialogComponent} from '../../containers/subject-dialog/subject-dialog.component';


export interface DialogData {
  animal: 'panda' | 'unicorn' | 'lion';
}


@Component({
  selector: 'app-rest-source-list',
  templateUrl: './rest-source-user-list.component.html',
  styleUrls: ['./rest-source-user-list.component.scss']
})
export class RestSourceUserListComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatTable) table!: MatTable<TableItem>;
  dataSource: TableDataSource;

  /** Columns displayed in the table. Columns IDs can be added, removed, or reordered. */
  columnsToDisplay = ['id',
    'userId',
    'externalUserId',
    'sourceType',
    'startDate',
    'endDate',
    'authorized',
    'actions'];

  // columnsToDisplay = [
  //   'id',
  //   'userId',
  //   'externalUserId',
  //   'sourceType',
  //   'startDate',
  //   'endDate',
  //   'authorized',
  //   'actions'
  // ];
  //
  @Input()
  project: any;
  // @Input('project') set project(project: string) { this.project$.next(project); }
  // private project$ = new BehaviorSubject<string>('');
  //
  // @ViewChild(MatPaginator) paginator?: MatPaginator;
  // @ViewChild(MatSort) sort?: MatSort;

  // updateTrigger = new BehaviorSubject<string>('init');
  // filterValue = new BehaviorSubject<string>('');
  // page = new BehaviorSubject<PageEvent>({
  //   pageIndex: 0,
  //   pageSize: 50,
  //   length: 0,
  //   previousPageIndex: 0,
  // });
  //
  // dataSource?: MatTableDataSource<RestSourceUser>;

  constructor(
    private restSourceUserService: RestSourceUserService,
    // private restSourceUserService: RestSourceUserMockService,
    public dialog: MatDialog,
  ) {
    this.dataSource = new TableDataSource();
  }

  ngOnInit() {
    // this.dataSource = new MatTableDataSource([]);
  }

  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
    this.table.dataSource = this.dataSource;

    // this.dataSource.sort = this.sort;
    // this.paginator.pageSize = 50;
    // this.paginator.page.subscribe(next => this.page.next(next));
    // this.subscribeToUsers();
  }

  // subscribeToUsers(): Subscription {
  //   const filterInput = this.filterValue
  //     .pipe(
  //       debounceTime(300),
  //       distinctUntilChanged(),
  //     );
  //   const projectInput = this.project$
  //     .pipe(
  //       filter(p => !!p),
  //       distinctUntilChanged(),
  //     );
  //   const pageInput = this.page
  //     .pipe(
  //       distinctUntilChanged((p1, p2) =>
  //         p1.pageIndex === p2.pageIndex && p1.pageSize === p2.pageSize)
  //     );
  //   return combineLatest<PageEvent, string, string, string>(pageInput, filterInput, this.updateTrigger, projectInput)
  //     .pipe(
  //       switchMap(([page, filterValue, _, project]) => this.loadUsers(filterValue, page, project))
  //     )
  //     .subscribe(users => {
  //       console.log(users);
  //       this.dataSource.data = users.users;
  //       this.paginator.pageIndex = users.metadata.pageNumber - 1;
  //       this.paginator.length = users.metadata.totalElements;
  //     });
  // }

  ngOnDestroy() {
    // this.updateTrigger.complete();
    // this.filterValue.complete();
    // this.page.complete();
  }

  // applyFilter(filterValue: string) {
  //   const trimmedValue = filterValue.trim();
  //   if (trimmedValue.length <= 1) {
  //     this.filterValue.next('');
  //   } else {
  //     this.filterValue.next(trimmedValue);
  //   }
  // }
  //
  // loadUsers(filterValue: string, page: PageEvent, project: string): Observable<RestSourceUsers> {
  //   const params = {
  //     page: page.pageIndex + 1,
  //     size: page.pageSize
  //   };
  //
  //   if (filterValue) {
  //     params['search'] = filterValue;
  //   }
  //
  //   return this.restSourceUserService.getAllUsersOfProject(project, params)
  //     .pipe(
  //       catchError(() => of({users: [], metadata: { pageNumber: 1, pageSize: page.pageSize, totalElements: 0 }} as RestSourceUsers)),
  //     );
  // }
  //
  // removeUser(restSourceUser: RestSourceUser) {
  //   this.restSourceUserService.deleteUser(restSourceUser.id)
  //     .subscribe(() => this.updateTrigger.next('delete'));
  // }
  //
  // resetUser(restSourceUser: RestSourceUser) {
  //   this.restSourceUserService.resetUser(restSourceUser)
  //     .subscribe(() => this.updateTrigger.next('reset'));
  // }
  //
  openUnauthorizeDialog(restSourceUser: RestSourceUser) {
    // const dialogRef = this.dialog.open(RestSourceUserListDeleteDialog, {
    //   data: restSourceUser
    // });
    //
    // dialogRef.afterClosed().subscribe(user => {
    //   if (user) {
    //     console.log('Deleting user...', user);
    //     this.removeUser(user);
    //   }
    // });
  }
  //
  openSubjectDialog(mode: string, restSourceUser?: RestSourceUser) {
    // this.dialog.open(DialogDataExampleDialog, {
    //   data: {
    //     animal: 'panda'
    //   }
    // });

    const dialogRef = this.dialog.open(SubjectDialogComponent, {
      data: {subject: restSourceUser, mode},
      width: "50%",
      disableClose: true
    });

    dialogRef.afterClosed().subscribe((user: RestSourceUser) => {
      if (user) {
        console.log('Resetting user...', user);
        // this.resetUser(user);
      }
    });
  }

  // authorizeSubject() {}
}

// @Component({
//   selector: 'dialog-data-example-dialog',
//   template: `
//     <h1 mat-dialog-title>Favorite Animal</h1>
//     <div mat-dialog-content>
//       My favorite animal is:
//       <ul>
//         <li>
//           <span *ngIf="data.animal === 'panda'">&#10003;</span> Panda
//         </li>
//         <li>
//           <span *ngIf="data.animal === 'unicorn'">&#10003;</span> Unicorn
//         </li>
//         <li>
//           <span *ngIf="data.animal === 'lion'">&#10003;</span> Lion
//         </li>
//       </ul>
//     </div>
//   `,
// })
// export class DialogDataExampleDialog {
//   constructor(@Inject(MAT_DIALOG_DATA) public data: DialogData) {}
// }
