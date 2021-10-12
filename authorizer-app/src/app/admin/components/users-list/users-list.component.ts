import {
  AfterViewInit,
  Component, EventEmitter,
  Input,
  OnInit,
  Output,
  TemplateRef,
  ViewChild
} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {MatPaginator, PageEvent} from '@angular/material/paginator';
import {MatTableDataSource} from '@angular/material/table';
import {MatDialog} from '@angular/material/dialog';
import {MatSort, MatSortable, Sort} from '@angular/material/sort';
import {MatBottomSheet} from "@angular/material/bottom-sheet";

import {SubjectService} from "@app/admin/services/subject.service";
import {UserService} from "@app/admin/services/user.service";
import {RestSourceUser} from '@app/admin/models/rest-source-user.model';

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
export class UsersListComponent implements OnInit, AfterViewInit {
  loading = true;
  error?: any;

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

  @Input('users') set users(users: RestSourceUser[]) { this.dataSource.data = users as UserData[]; }

  @Output()
  action: EventEmitter<any> = new EventEmitter<any>();

  constructor(
    private userService: UserService,
    private subjectService: SubjectService,
    public dialog: MatDialog,
    private bottomSheet: MatBottomSheet,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {}


  ngOnInit() {}

  ngAfterViewInit() {
    this.applyTableSort();
    this.dataSource.paginator = this.paginator;
    this.dataSource.filterPredicate = this.createTableFilterPredicate();
    setTimeout(() => this._initialSetup());
    this.listenToStateChangeEvents();
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
    this.filterValues[filter.columnProp] = event.value;
    this.dataSource.filter = JSON.stringify(this.filterValues);
    this.applyStateChangesToUrlQueryParams({[filter.columnProp]: event.value})
  }

  resetFilters() {
    this.filterValues = {}
    this.filters.forEach((value: any, _: any) => {
      value.modelValue = undefined;
    })
    this.dataSource.filter = "";
    this.applyStateChangesToUrlQueryParams({isAuthorized: null, userId: null});
  }
  //#endregion

  //#region User Actions
  openSubjectDialog(mode: string, user: RestSourceUser) {
    this.action.emit({mode, user});
  }
  //#endregion

  private _initialSetup(): void {
    this.checkActivePageQuery();
    this.checkActiveSortQuery();
    this.checkActiveFilterQuery();
  }

  private checkActiveFilterQuery(): void {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    if (queryParams.hasOwnProperty('userId') || queryParams.hasOwnProperty('isAuthorized')) {
      this.filters.forEach((value: any, _: any) => {
        if(value.columnProp === 'userId'){
          if (queryParams.userId) {
            value.modelValue = queryParams.userId;
            this.filterValues.userId = queryParams.userId;
          } else {
            value.modelValue = undefined;
            this.filterValues.userId = undefined;
          }
        }
        if(value.columnProp === 'isAuthorized') {
          if (queryParams.isAuthorized) {
            value.modelValue = queryParams.isAuthorized;
            this.filterValues.isAuthorized = queryParams.isAuthorized;
          } else {
            value.modelValue = undefined;
            this.filterValues.isAuthorized = undefined;
          }
        }
      })
      this.dataSource.filter = JSON.stringify(this.filterValues);
    }
  }

  private checkActiveSortQuery(): void {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    if (queryParams.hasOwnProperty('sortField') && queryParams.hasOwnProperty('sortOrder')) {
      const sortActiveColumn = queryParams.sortField || this.matSortActive;
      const sortable: MatSortable = {
        id: sortActiveColumn,
        start: queryParams.sortOrder || this.matSortDirection,
        disableClear: true
      };
      this.dataSource.sort!.sort(sortable);

      const activeSortHeader: any = this.dataSource.sort!.sortables.get(sortActiveColumn);
      if (!activeSortHeader) { return; }
      activeSortHeader['_setAnimationTransitionState']({
        fromState: this.dataSource.sort!.direction,
        toState: 'active',
      });
    }
  }

  private checkActivePageQuery(): void {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    if (queryParams.hasOwnProperty('pageSize') && queryParams.hasOwnProperty('pageIndex')) {
      this.dataSource.paginator!.pageIndex = queryParams.pageIndex;
      this.dataSource.paginator!.pageSize = queryParams.pageSize;

      this.dataSource.paginator!.page.next({
        pageIndex: queryParams.pageIndex,
        pageSize: queryParams.pageSize,
        length: this.dataSource.paginator!.length
      });
    }
  }

  private listenToStateChangeEvents(): void {
    this.dataSource.sort!.sortChange
      .subscribe((sortChange: Sort) => {
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
      });
  }

  private applyStateChangesToUrlQueryParams(queryParams: any): void {
    this.router.navigate([], { queryParams: queryParams, queryParamsHandling: 'merge' }).finally();
  }

  openTemplateSheetMenu() {
    this.bottomSheet.open(this.TemplateBottomSheet);
  }

  // closeTemplateSheetMenu() {
  //   this.bottomSheet.dismiss();
  // }

  // openBottomSheet(): void {
  //   this._bottomSheet.open(SortAndFiltersComponent);
  // }
}
