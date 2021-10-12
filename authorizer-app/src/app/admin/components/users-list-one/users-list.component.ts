import {AfterViewInit, Component, Injectable, Input, OnDestroy, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {MatPaginator, MatPaginatorIntl, PageEvent} from '@angular/material/paginator';
import {MatTableDataSource} from '@angular/material/table';
import {MatDialog} from '@angular/material/dialog';
import {MatSort, MatSortable, Sort} from '@angular/material/sort';
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
  id: string;
  projectId: string;
  userId: string;
  externalId: string;
  sourceId: string;
  serviceUserId: string;
  startDate: string;
  endDate: string;
  sourceType: string;
  isAuthorized: boolean;
  hasValidToken: boolean;
  timesReset: number;
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
export class UsersListComponent implements OnInit, AfterViewInit, OnDestroy {
  loading = true;
  error?: any;

  @Input()
  projects: RadarProject[] = [];

  @Input()
  sourceClients: RadarSourceClient[] = [];

  @Input('project') set project(project: string) { this.projectSubject.next(project); }
  private projectSubject = new BehaviorSubject<string>('');

  project$ = this.projectSubject.asObservable().pipe(
      filter(d => !!d),
      distinctUntilChanged(),
    );

  @Input('sourceClient') set sourceClient(sourceClient: string) { this.sourceClientSubject.next(sourceClient); }
  private sourceClientSubject = new BehaviorSubject<string>('');

  sourceClient$ = this.sourceClientSubject.asObservable().pipe(
    filter(d => !!d),
    distinctUntilChanged(),
  );

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

  updateTriggerSubject = new BehaviorSubject<string>('init');
  subjects$?: Observable<RadarSubject[]>;
  users$?: Observable<RestSourceUser[]>;

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
  matSortActive!: string;
  matSortDirection!: 'asc' | 'desc';

  users?: RestSourceUser[];

  constructor(
    private userService: UserService,
    private subjectService: SubjectService,
    public dialog: MatDialog,
    private bottomSheet: MatBottomSheet,
    private router: Router,
    private activatedRoute: ActivatedRoute
    // private translateService: TranslateService,
  ) {}

  openTemplateSheetMenu() {
    this.bottomSheet.open(this.TemplateBottomSheet);
  }

  closeTemplateSheetMenu() {
    this.bottomSheet.dismiss();
  }

  ngOnInit() {}

  ngOnDestroy() {
    this.updateTriggerSubject.complete();
  }

  ngAfterViewInit() {
    this.applyTableSort();
    // this.dataSource.sort?.sortChange.subscribe((sortChange: Sort) => {
    //   this._applySortChangesToUrlQueryParams(sortChange);
    // });
    this.dataSource.paginator = this.paginator;
    // this.dataSource.paginator.pageIndex = 2;//this.paginator;
    //this.paginator.pageIndex = 2;
    // // this.dataSource.paginator.pageIndex = 4;
    // // this.dataSource.paginator?.page.subscribe((pageEvent: PageEvent) => {
    // //   this._applyPageChangesToUrlQueryParams(pageEvent);
    // // })
    this.dataSource.filterPredicate = this.createTableFilterPredicate();

    setTimeout(() => this._initialSetup());
    // this._initialSetup();
    this.listenToStateChangeEvents();

    this.loadTableData();
    // setTimeout(()=>this.updatePageIndex());
    console.log('user list ngAfterViewInit 1')

  }

  loadTableData(): void {
    console.log('loadTableData 0');
    combineLatest([this.updateTriggerSubject, this.project$, this.sourceClient$]).pipe(
      switchMap(
        ([_, project, sourceClient]) => {
          console.log(project, sourceClient);
          return this.loadAndModifyUsers(project, sourceClient)
        }
      )
    ).subscribe({
      next: users => {
        console.log('loadTableData 1');
        console.log(users);
        this.users = users;
        this.dataSource.data = users;
        const activeDialog = this.isDialogActive();
        console.log(activeDialog);
        if (activeDialog?.edit) {
          console.log(this.users);
          const user = this.users?.filter(user => user.userId === activeDialog.edit)[0];
          console.log(user);
          if(user){
            this.openSubjectDialog('edit', user);
          } else {
            return;
          }
        }

        if (activeDialog?.delete) {
          const user = this.users?.filter(user => user.userId === activeDialog.delete)[0];
          if(user){
            this.openSubjectDialog('delete', user);
          } else {
            return;
          }
        }
        this.loading = false;
        console.log('loadTableData 2');

      },
      error: (error) => {
        this.error = error;
        // this.loading = false;
      }
    });
  }

  loadAndModifyUsers(project: string, sourceClient: string): Observable<UserData[]> {
    console.log(project, sourceClient);
    this.subjects$ = this.subjectService.getSubjectsOfProject(project);
    this.users$ = this.userService.getUsersOfProject(project).pipe(
      map(resp => resp.users)
    );
    return combineLatest([this.subjects$, this.users$]).pipe(
      map(([subjects, users]) => {
        const newSubjects: any[] = [];
        subjects?.map(subject => {
          // this.sourceClients.map(sourceClient => {
          //   const newSubject = {...subject, sourceType: sourceClient.sourceType}
          //   newSubjects.push(newSubject)
          // })
          const newSubject = {...subject, sourceType: sourceClient};
          // console.log(newSubject, sourceClient);
          newSubjects.push(newSubject)
        });
        return newSubjects.map(subject => {
          const user = users.filter(user => {
            return user.userId === subject.id && user.sourceType === subject.sourceType
          })[0];
          // console.log(user);
          return {
            ...subject,
            id: null,
            ...user,
            userId: subject.id,
            isAuthorized: !user?.isAuthorized ? false : user.isAuthorized
          }
        })
      }),
    )
    // return this.userService.getUsersOfProject(project).pipe(
    //     catchError(() => of({users: [], metadata: { pageNumber: 1, pageSize: page.pageSize, totalElements: 0 }} as RestSourceUsers)),
    //   );
  }

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
        return item[property]? new Date(item[property]) : null;
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
                data.externalId.toString().toLowerCase().indexOf(searchTerms[key]) === -1){
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
    this.dataSource.filter = JSON.stringify(this.filterValues);
    this.applyStateChangesToUrlQueryParams({[filter.columnProp]: event.value})
    // this.router.navigate([],
    //   {
    //     relativeTo: this.activatedRoute,
    //     queryParams: {[filter.columnProp]: event.value},
    //     queryParamsHandling: 'merge', // remove to replace all query params by provided
    //   }).finally();
  }

  // Reset table filters
  resetFilters() {
    // todo query params
    this.applyStateChangesToUrlQueryParams({isAuthorized: null, userId: null});
    // this.router.navigate([],
    //   {
    //     relativeTo: this.activatedRoute,
    //     queryParams: {isAuthorized: null, userId: null},
    //     queryParamsHandling: 'merge', // remove to replace all query params by provided
    //   }).finally();
    this.filterValues = {}
    this.filters.forEach((value: any, key: any) => {
      value.modelValue = undefined;
    })
    this.dataSource.filter = "";
  }
  //#endregion

  //#region User Actions
  openRemoveAuthorizationDialog(restSourceUser: RestSourceUser) {
    const dialogRef = this.dialog.open(UserDeleteDialog, {
      data: {subject: restSourceUser},
      // width: "50%",
      disableClose: true
    });

    dialogRef.afterClosed().subscribe({
      next: (command) => {
        if(command === 'deleted'){
          console.log(command, 'DEL');
          this.updateTriggerSubject.next('deleted');
        } else {
        }
      },
      error: (error) => this.error = error
    });
  }

  openSubjectDialog(mode: string, user: RestSourceUser) {

    const dialogRef = this.dialog.open(UserDialogComponent, {
      data: {subject: user, mode},
      panelClass: 'full-width-dialog',
      // width: "50%",
      disableClose: true
    });
    this.applyStateChangesToUrlQueryParams({[mode]: user.userId});

    dialogRef.afterClosed().subscribe({
      next: (command) => {
        if(command === 'updated'){
          console.log(command, 'UPD');
          this.updateTriggerSubject.next('updated');
        } else {
        }
        this.applyStateChangesToUrlQueryParams({[mode]: null});
      },
      error: (error) => this.error = error
    });
  }
  //#endregion

  // openBottomSheet(): void {
  //   this._bottomSheet.open(SortAndFiltersComponent);
  // }

  private _initialSetup(): void {



    const activePageQuery = this.isPageQueryActive();
    if (activePageQuery) {
      this.dataSource.paginator!.pageIndex = activePageQuery.pageIndex;
      this.dataSource.paginator!.pageSize = activePageQuery.pageSize;

      this.dataSource.paginator!.page.next({
        pageIndex: activePageQuery.pageIndex, //pageNumber,
        pageSize: activePageQuery.pageSize, // this.paginator.pageSize,
        length: this.dataSource.paginator!.length
      });
    }

    // Activating initial Sort
    const activeSortQuery = this.isSortQueryActive();
    if (activeSortQuery) {
      const sortActiveColumn = activeSortQuery ? (activeSortQuery.sortOrder ? activeSortQuery.sortField : activeSortQuery.sortField) : this.matSortActive;
      const sortable: MatSortable = {
        id: sortActiveColumn,
        start: activeSortQuery ? (activeSortQuery.sortOrder || null) : this.matSortDirection,
        disableClear: true
      };
      this.dataSource.sort!.sort(sortable);

      if (!sortActiveColumn) { return; }
      // Material Sort Issue: https://github.com/angular/components/issues/10242
      // Picked a hack from: https://github.com/angular/components/issues/10242#issuecomment-421490991
      const activeSortHeader: any = this.dataSource.sort!.sortables.get(sortActiveColumn);
      if (!activeSortHeader) { return; }
      activeSortHeader['_setAnimationTransitionState']({
        fromState: this.dataSource.sort!.direction,
        toState: 'active',
      });
    }

    const activeFilterQuery = this.isFilterQueryActive();
    if (activeFilterQuery) {
      // set input fields
      this.filters.forEach((value: any, key: any) => {
        if(value.columnProp === 'userId'){
          if (activeFilterQuery.userId) {
            value.modelValue = activeFilterQuery.userId;
            this.filterValues.userId = activeFilterQuery.userId;
          } else {
            value.modelValue = undefined;
            this.filterValues.userId = undefined;
          }
        }
        if(value.columnProp === 'isAuthorized') {
          if (activeFilterQuery.isAuthorized) {
            value.modelValue = activeFilterQuery.isAuthorized;
            this.filterValues.isAuthorized = activeFilterQuery.isAuthorized;
          } else {
            value.modelValue = undefined;
            this.filterValues.isAuthorized = undefined;
          }
        }
      })

      // set filtervalues
      // this.filterValues[filter.columnProp] = event.value; //filterValue; //.trim().toLowerCase()

      this.dataSource.filter = JSON.stringify(this.filterValues);

      // this.filterValues = {}

      // this.dataSource.filter = "";
    }
  }

  private isDialogActive(): {[key: string]: string} | undefined {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    if (queryParams.hasOwnProperty('edit')) {
      return {
        edit: queryParams.edit,
      };
    }
    if (queryParams.hasOwnProperty('delete')){
      return {
        delete: queryParams.delete
      };
    }
    return;
  }

  private isFilterQueryActive(): { userId: string | null, isAuthorized: 'yes' | 'pending' | 'no' | 'unset' | null } | undefined {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    if (queryParams.hasOwnProperty('userId') || queryParams.hasOwnProperty('isAuthorized')) {
      return {
        userId: queryParams.userId,
        isAuthorized: queryParams.isAuthorized
      };
    }
    return;
  }

  private isSortQueryActive(): { sortField: string, sortOrder: 'asc' | 'desc' } | undefined {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    if (queryParams.hasOwnProperty('sortField') || queryParams.hasOwnProperty('sortOrder')) {
      return {
        sortField: queryParams.sortField,
        sortOrder: queryParams.sortOrder
      };
    }
    return;
  }

  private isPageQueryActive(): { pageSize: number, pageIndex: number } | undefined{
    const queryParams = this.activatedRoute.snapshot.queryParams;
    if (queryParams.hasOwnProperty('pageSize') || queryParams.hasOwnProperty('pageIndex')) {
      return {
        pageSize: queryParams.pageSize,
        pageIndex: queryParams.pageIndex
      };
    }
    return;
  }

  private listenToStateChangeEvents(): void {
    this.dataSource.sort!.sortChange
      .subscribe((sortChange: Sort) => {
        // this._applySortChangesToUrlQueryParams(sortChange);
        this.applyStateChangesToUrlQueryParams({
          sortField: sortChange.direction? sortChange.active || null : null,
          sortOrder: sortChange.direction || null,
        });
      });

    this.dataSource.paginator!.page
      .subscribe((pageChange: PageEvent) => {
        this.applyStateChangesToUrlQueryParams({
          pageSize: pageChange.pageSize,
          pageIndex: pageChange.pageIndex,
        })
        // this._applyPageStateChangesToUrlQueryParams(pageChange);
      });
  }

  private applyStateChangesToUrlQueryParams(queryParams: any): void {
    this.router.navigate([], { queryParams: queryParams, queryParamsHandling: 'merge' }).finally();
  }

  // private _applySortChangesToUrlQueryParams(sortChange: Sort): void {
  //   const sortingQueryParams = {
  //     sortField: sortChange.direction? sortChange.active || null : null,
  //     sortOrder: sortChange.direction || null,
  //   };
  //   this.router.navigate([], { queryParams: sortingQueryParams, queryParamsHandling: 'merge' }).finally();
  // }
  //
  // private _applyPageStateChangesToUrlQueryParams(pageChange: PageEvent): void {
  //   const paginationQueryParams = {
  //     pageSize: pageChange.pageSize,
  //     pageIndex: pageChange.pageIndex,
  //   };
  //   this.router.navigate([], { queryParams: paginationQueryParams, queryParamsHandling: 'merge' }).finally();
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
