import {AfterViewInit, Component, Injectable, Input, OnDestroy, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {MatPaginator, MatPaginatorIntl, PageEvent} from '@angular/material/paginator';
import {MatTableDataSource} from '@angular/material/table';
import {MatDialog} from '@angular/material/dialog';
import {MatSort, Sort} from '@angular/material/sort';
import {
  BehaviorSubject,
  combineLatest,
  distinctUntilChanged,
  filter,
  Observable,
  Subject,
  switchMap,
  takeUntil
} from "rxjs";
import {map} from "rxjs/operators";
import {MatBottomSheet} from "@angular/material/bottom-sheet";
import {TranslateService} from "@ngx-translate/core";

import {SubjectService} from "@app/admin/services/subject.service";
import {UserService} from "@app/admin/services/user.service";
import {UserDialogComponent} from "@app/admin/containers/user-dialog/user-dialog.component";
import {UserDeleteDialog} from "@app/admin/containers/user-delete-dialog/user-delete-dialog.component";
import {RadarProject, RadarSourceClient, RadarSubject} from "@app/admin/models/radar-entities.model";
import {RestSourceUser} from '@app/admin/models/rest-source-user.model';
import {StorageItem} from "@app/shared/enums/storage-item";
import {ActivatedRoute, Router} from "@angular/router";

export interface UserData extends RestSourceUser{
  [key: string]: any;
  id?: string;
  projectId: string;
  userId: string;
  externalId?: string;
  sourceId?: string;
  serviceUserId?: string;
  startDate?: string;
  endDate?: string;
  sourceType: string;
  isAuthorized?: boolean;
  hasValidToken?: boolean;
  timesReset?: number;
}

export interface FilterItem {
  name: string,
  columnProp: string,
  type: string,
  width: number,
  options?: {value: string | boolean | number, label: string}[],
  modelValue?: any,
}

@Component({
  selector: 'app-users-list',
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.scss']
})
export class UsersListComponent implements OnInit, AfterViewInit {
  loading = true;
  error?: any;

  _modifiedUsers!: UserData[];
  @Input('modifiedUsers')
  set modifiedUsers(value: UserData[]) {
    this._modifiedUsers = [...value];
  }

  get modifiedUsers(): UserData[] {
    return this._modifiedUsers;
  }

  // @Input('modifiedUsers') get modifiedUsers(): RestSourceUser[] { return this.modifiedUsers}
  // @Input()
  // projects: RadarProject[] = [];
  //
  // @Input()
  // sourceClients: RadarSourceClient[] = [];
  //
  // @Input('project') set project(project: string) { this.projectSubject.next(project); }
  // private projectSubject = new BehaviorSubject<string>('');
  //
  // project$ = this.projectSubject.asObservable().pipe(
  //     filter(d => !!d),
  //     distinctUntilChanged(),
  //   );
  //
  // @Input('sourceClient') set sourceClient(sourceClient: string) { this.sourceClientSubject.next(sourceClient); }
  // private sourceClientSubject = new BehaviorSubject<string>('');
  //
  // sourceClient$ = this.sourceClientSubject.asObservable().pipe(
  //   filter(d => !!d),
  //   distinctUntilChanged(),
  // );

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort: MatSort = new MatSort();
  displayedColumns: string[] = [
    'userId',
    'externalId',
    'sourceType',
    'startDate',
    'endDate',
    'isAuthorized',
    'actions'
  ];
  dataSource: MatTableDataSource<UserData> = new MatTableDataSource<UserData>();

  // updateTriggerSubject = new BehaviorSubject<string>('init');
  // subjects$?: Observable<RadarSubject[]>;
  // users$?: Observable<RestSourceUser[]>;

  filterValues: any = {}; // todo type
  filters: FilterItem[] = [
    {
      name: 'ADMIN.USERS_LIST.filters.userIdLabel',
      columnProp: 'userId',
      type: 'input',
      width: 300,
    },
    {
      name: 'ADMIN.USERS_LIST.filters.authorizedLabel',
      columnProp: 'isAuthorized',
      type: 'select',
      options: [{value: 'yes', label: 'ADMIN.GENERAL.yes'}, {value: 'pending', label: 'ADMIN.GENERAL.pending'}, {value: 'no', label: 'ADMIN.GENERAL.no'}, {value: 'unset', label: 'ADMIN.GENERAL.unset'}],
      width: 150,
    }
  ]

  @ViewChild('templateBottomSheet') TemplateBottomSheet!: TemplateRef<any>;

  constructor(
    private userService: UserService,
    private subjectService: SubjectService,
    public dialog: MatDialog,
    private bottomSheet: MatBottomSheet,
    private router: Router,
    private activatedRoute: ActivatedRoute
    // private translateService: TranslateService,
  ) {
    // this.translateService.get(['']).subscribe(translations => {
    //   console.info(this.translateService.instant('ADMIN.USERS_LIST.filters.userIdLabel'));
    //   // console.info(this.translateService.instant('AD'));
    //   // console.info(this.translateService.instant('key2'));
    //   // console.info(this.translateService.instant('key3'));
    // });
  }

  openTemplateSheetMenu() {
    this.bottomSheet.open(this.TemplateBottomSheet);
  }

  closeTemplateSheetMenu() {
    this.bottomSheet.dismiss();
  }

  ngOnInit() {
    console.log('user list ngOnInit 0', this.modifiedUsers)
    // this.dataSource = new MatTableDataSource<UserData>(this.modifiedUsers);
    // // this.loadTableData();
    // this.applyTableSort();
    // // this.dataSource.sort?.sortChange.subscribe((sortChange: Sort) => {
    // //   this._applySortChangesToUrlQueryParams(sortChange);
    // // });
    // this.dataSource.paginator = this.paginator;
    // // this.dataSource.paginator?.page.subscribe((pageEvent: PageEvent) => {
    // //   this._applyPageChangesToUrlQueryParams(pageEvent);
    // // })
    // this.dataSource.filterPredicate = this.createTableFilterPredicate();
    // // this.loadTableData();
    console.log('user list ngOnInit 1')
  }

  // ngOnDestroy() {
  //   this.updateTriggerSubject.complete();
  // }

  ngAfterViewInit() {
    console.log('user list ngAfterViewInit 0')
    this.dataSource = new MatTableDataSource<UserData>(this.modifiedUsers);
    // this.loadTableData();
    this.applyTableSort();
    // this.dataSource.sort?.sortChange.subscribe((sortChange: Sort) => {
    //   this._applySortChangesToUrlQueryParams(sortChange);
    // });
    this.dataSource.paginator = this.paginator;
    // this.dataSource.paginator?.page.subscribe((pageEvent: PageEvent) => {
    //   this._applyPageChangesToUrlQueryParams(pageEvent);
    // })
    this.dataSource.filterPredicate = this.createTableFilterPredicate();
    // this.loadTableData();
    console.log('user list ngAfterViewInit 1')

  }

  // private _applyPageChangesToUrlQueryParams(pageEvent: PageEvent): void {
  //   console.log(pageEvent.pageSize, pageEvent.pageIndex);
  //   const paginationQueryParams = {
  //     pageSize: pageEvent.pageSize,
  //     pageIndex: pageEvent.pageIndex,
  //   };
  //   this.router.navigate([],
  //     {
  //       relativeTo: this.activatedRoute,
  //       queryParams: paginationQueryParams,
  //       queryParamsHandling: 'merge', // remove to replace all query params by provided
  //     }).finally();
  // }

  // private _applySortChangesToUrlQueryParams(sortChange: Sort): void {
  //   console.log(sortChange.active, sortChange.direction);
  //   const sortingAndPaginationQueryParams = {
  //     sortField: sortChange.direction? sortChange.active || null : null,
  //     sortOrder: sortChange.direction || null,
  //   };
  //   this.router.navigate([],
  //     {
  //       relativeTo: this.activatedRoute,
  //       queryParams: sortingAndPaginationQueryParams,
  //       queryParamsHandling: 'merge', // remove to replace all query params by provided
  //     }).finally();
  // }

  // loadTableData(): void {
  //   console.log('loadTableData');
  //   combineLatest([this.updateTriggerSubject, this.project$, this.sourceClient$]).pipe(
  //     switchMap(
  //       ([_, project, sourceClient]) => {
  //         console.log(project, sourceClient);
  //         return this.loadAndModifyUsers(project, sourceClient)
  //       }
  //     )
  //   ).subscribe({
  //     next: users => {
  //       console.log(users);
  //       this.dataSource.data = users;
  //       this.loading = false;
  //     },
  //     error: (error) => {
  //       this.error = error;
  //       this.loading = false;
  //     }
  //   });
  // }

  // loadAndModifyUsers(project: string, sourceClient: string): Observable<UserData[]> {
  //   console.log(project, sourceClient);
  //   this.subjects$ = this.subjectService.getSubjectsOfProject(project);
  //   this.users$ = this.userService.getUsersOfProject(project).pipe(
  //     map(resp => resp.users)
  //   );
  //   return combineLatest([this.subjects$, this.users$]).pipe(
  //     map(([subjects, users]) => {
  //       const newSubjects: any[] = [];
  //       subjects?.map(subject => {
  //         // this.sourceClients.map(sourceClient => {
  //         //   const newSubject = {...subject, sourceType: sourceClient.sourceType}
  //         //   newSubjects.push(newSubject)
  //         // })
  //         const newSubject = {...subject, sourceType: sourceClient};
  //         console.log(newSubject, sourceClient);
  //         newSubjects.push(newSubject)
  //       });
  //       return newSubjects.map(subject => {
  //         const user = users.filter(user => {
  //           return user.userId === subject.id && user.sourceType === subject.sourceType
  //         })[0];
  //         console.log(user);
  //         return {
  //           ...subject,
  //           id: null,
  //           ...user,
  //           userId: subject.id,
  //           isAuthorized: !user?.isAuthorized ? false : user.isAuthorized
  //         }
  //       })
  //     }),
  //   )
  //   // return this.userService.getUsersOfProject(project).pipe(
  //   //     catchError(() => of({users: [], metadata: { pageNumber: 1, pageSize: page.pageSize, totalElements: 0 }} as RestSourceUsers)),
  //   //   );
  // }

  //#region Sort and Filter
  applyTableSort(): void {
    this.dataSource.sort = this.sort;
    console.log(this.dataSource.sort);
    this.dataSource.sortingDataAccessor = (item: UserData, property: string) => {
      if(property === 'isAuthorized'){
        if (item[property]) {
          return 1; //'Yes';
        } else {
          if (item.id) {
            if (item.registrationCreatedAt) {
              return 2; //'Pending';
            }else {
              return 3; //'No';
            }
          } else {
            return 4; //'Unset';
          }
        }
      }
      if(property === 'startDate' || property === 'endDate'){
        return item[property]? new Date(item[property] as string) : null;
      }
      return item[property].toLocaleLowerCase();
    };
  }

  // Custom filter method fot Angular Material Datatable
  createTableFilterPredicate() {
    return function (data: UserData, filter: string): boolean {
      let searchTerms = JSON.parse(filter);
      let isFilterSet = false;
      for (const key in searchTerms) {
        if (searchTerms.hasOwnProperty(key) && searchTerms[key].toString() !== '') {
          isFilterSet = true;
        } else {
          delete searchTerms[key];
        }
      }
      if (!isFilterSet) {
        return true;
      }
      for (const key in searchTerms) {
        if(searchTerms.hasOwnProperty(key)) {
          switch(key){
            case 'userId':
              if (data.userId.toString().toLowerCase().indexOf(searchTerms[key]) === -1 &&
                data.externalId?.toString().toLowerCase().indexOf(searchTerms[key]) === -1){
                return false;
              }
              break;
            case 'isAuthorized':
              let isAuthorized = 'yes';
              if (data.isAuthorized) {
                isAuthorized = 'yes';
              } else {
                if (data.id) {
                  if (data.registrationCreatedAt) {
                    isAuthorized = 'pending';
                  }else {
                    isAuthorized = 'no';
                  }
                } else {
                  isAuthorized = 'unset';
                }
              }
              if (isAuthorized !== searchTerms[key]){
                return false;
              }
              break;
          }
        }
      }
      return true;
    }
  }

  filterChange(filter: any, event: any) {
    // todo query params
    console.log(event)
    console.log(event.value)
    this.filterValues[filter.columnProp] = event.value; //filterValue; //.trim().toLowerCase()
    this.dataSource.filter = JSON.stringify(this.filterValues)
    this.router.navigate([],
      {
        relativeTo: this.activatedRoute,
        queryParams: {[filter.columnProp]: event.value},
        queryParamsHandling: 'merge', // remove to replace all query params by provided
      }).finally();
  }

  // Reset table filters
  resetFilters() {
    // todo query params
    this.router.navigate([],
      {
        relativeTo: this.activatedRoute,
        queryParams: {isAuthorized: null, userId: null},
        queryParamsHandling: 'merge', // remove to replace all query params by provided
      }).finally();
    this.filterValues = {}
    this.filters.forEach((value: any, key: any) => {
      value.modelValue = undefined;
    })
    this.dataSource.filter = "";
  }
  //#endregion

  //#region User Actions
  // openRemoveAuthorizationDialog(restSourceUser: RestSourceUser) {
  //   const dialogRef = this.dialog.open(UserDeleteDialog, {
  //     data: {subject: restSourceUser},
  //     // width: "50%",
  //     disableClose: true
  //   });
  //
  //   dialogRef.afterClosed().subscribe({
  //     next: (command) => {
  //       if(command === 'deleted'){
  //         console.log(command, 'DEL');
  //         this.updateTriggerSubject.next('deleted');
  //       } else {
  //       }
  //     },
  //     error: (error) => this.error = error
  //   });
  // }

  openSubjectDialog(mode: string, user: RestSourceUser) {

    const dialogRef = this.dialog.open(UserDialogComponent, {
      data: {subject: user, mode},
      panelClass: 'full-width-dialog',
      // width: "50%",
      disableClose: true
    });

    dialogRef.afterClosed().subscribe({
      next: (command) => {
        if(command === 'updated'){
          console.log(command, 'UPD');
          // this.updateTriggerSubject.next('updated');
        } else {
        }
      },
      error: (error) => this.error = error
    });
  }
  //#endregion

  // openBottomSheet(): void {
  //   this._bottomSheet.open(SortAndFiltersComponent);
  // }

}

@Injectable()
export class CustomMatPaginatorIntl extends MatPaginatorIntl
  implements OnDestroy {
  unsubscribe: Subject<void> = new Subject<void>();
  OF_LABEL = 'of';

  constructor(private translate: TranslateService) {
    super();

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

  getAndInitTranslations() {
    this.translate
      .get([
        'ADMIN.USERS_LIST.PAGINATOR.ITEMS_PER_PAGE',
        'ADMIN.USERS_LIST.PAGINATOR.NEXT_PAGE',
        'ADMIN.USERS_LIST.PAGINATOR.PREVIOUS_PAGE',
        'ADMIN.USERS_LIST.PAGINATOR.OF_LABEL',
      ]).pipe(
        takeUntil(this.unsubscribe)
      ).subscribe(translation => {
        this.itemsPerPageLabel =
          translation['ADMIN.USERS_LIST.PAGINATOR.ITEMS_PER_PAGE'];
        this.nextPageLabel = translation['ADMIN.USERS_LIST.PAGINATOR.NEXT_PAGE'];
        this.previousPageLabel =
          translation['ADMIN.USERS_LIST.PAGINATOR.PREVIOUS_PAGE'];
        this.OF_LABEL = translation['ADMIN.USERS_LIST.PAGINATOR.OF_LABEL'];
        this.changes.next();
      });
  }

  getRangeLabel = (page: number, pageSize: number, length: number) => {
    if (length === 0 || pageSize === 0) {
      return `0 ${this.OF_LABEL} ${length}`;
    }
    length = Math.max(length, 0);
    const startIndex = page * pageSize;
    const endIndex =
      startIndex < length
        ? Math.min(startIndex + pageSize, length)
        : startIndex + pageSize;
    return `${startIndex + 1} - ${endIndex} ${
      this.OF_LABEL
    } ${length}`;
  };
}
