import {AfterViewInit, Component, Injectable, Input, OnDestroy, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {MatPaginator, MatPaginatorIntl} from '@angular/material/paginator';
import {MatTableDataSource} from '@angular/material/table';
import {MatDialog} from '@angular/material/dialog';
import {MatSort} from '@angular/material/sort';
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
import {SubjectService} from "../../services/subject.service";
import {UserService} from "../../services/user.service";
import {UserDialogComponent} from "../../containers/user-dialog/user-dialog.component";
import {UserDeleteDialog} from "../../containers/user-delete-dialog/user-delete-dialog.component";
import {RadarProject, RadarSourceType} from "../../models/rest-source-project.model";
import {RestSourceUser} from '../../models/rest-source-user.model';
import {MatBottomSheet} from "@angular/material/bottom-sheet";
import {TranslateService} from "@ngx-translate/core";
// import {SortAndFiltersComponent} from "../sort-and-filters/sort-and-filters.component";

export interface UserData {
  [key: string]: any;
  id: number;
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
  sourceTypes: RadarSourceType[] = [];

  @Input('project') set project(project: string) { this.projectSubject.next(project); }
  private projectSubject = new BehaviorSubject<string>('');

  project$ = this.projectSubject.asObservable().pipe(
      filter(p => !!p),
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
  subjects$?: Observable<RestSourceUser[]>;
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
      options: [{value: true, label: 'ADMIN.GENERAL.yes'}, {value: false, label: 'ADMIN.GENERAL.no'}],
      width: 150,
    }
  ]

  @ViewChild('templateBottomSheet') TemplateBottomSheet!: TemplateRef<any>;

  constructor(
    private userService: UserService,
    private subjectService: SubjectService,
    public dialog: MatDialog,
    private bottomSheet: MatBottomSheet,
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
    // this.dataSource = new MatTableDataSource<UserData>();
  }

  ngOnDestroy() {
    this.updateTriggerSubject.complete();
  }

  ngAfterViewInit() {
    this.applyTableSort();
    this.dataSource.paginator = this.paginator;
    this.dataSource.filterPredicate = this.createTableFilterPredicate();
    this.loadTableData();
  }

  loadTableData(): void {
    combineLatest([this.updateTriggerSubject, this.project$]).pipe(
      switchMap(
        ([_, project]) => this.loadAndModifyUsers(project)
      )
    ).subscribe({
      next: users => {
        console.log(users);
        this.dataSource.data = users;
        this.loading = false;
      },
      error: (error) => {
        this.error = error;
        this.loading = false;
      }
    });
  }

  loadAndModifyUsers(project: string): Observable<UserData[]> {
    this.subjects$ = this.subjectService.getSubjectsOfProjects(project);
    this.users$ = this.userService.getUsersOfProject(project).pipe(
      map(resp => resp.users)
    );
    return combineLatest([this.subjects$, this.users$]).pipe(
      map(([subjects, users]) => {
        const newSubjects: any[] = [];
        subjects.map(subject => {
          this.sourceTypes.map((sourceType: any) => {
            const newSubject = {...subject, sourceType: sourceType.sourceType}
            newSubjects.push(newSubject)
          })
        });
        return newSubjects.map(subject => {
          const user = users.filter(user => {
            return user.userId === subject.id && user.sourceType === subject.sourceType
          })[0];
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
    this.dataSource.sortingDataAccessor = (item: UserData, property: string) => {
      if(property === 'isAuthorized'){
        return item[property].toString().toLocaleLowerCase();
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
              if (data[key] !== searchTerms[key]){
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
    console.log(event)
    console.log(event.value)
    this.filterValues[filter.columnProp] = event.value; //filterValue; //.trim().toLowerCase()
    this.dataSource.filter = JSON.stringify(this.filterValues)
  }

  // Reset table filters
  resetFilters() {
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

    dialogRef.afterClosed().subscribe({
      next: (command) => {
        if(command === 'updated'){
          console.log(command, 'UPD');
          this.updateTriggerSubject.next('updated');
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
