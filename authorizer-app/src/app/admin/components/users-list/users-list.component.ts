import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  Output,
  TemplateRef,
  ViewChild
} from '@angular/core';
import { ActivatedRoute, Router } from "@angular/router";
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort, MatSortable, Sort } from '@angular/material/sort';
import { MatBottomSheet } from "@angular/material/bottom-sheet";

import { SubjectService } from "@app/admin/services/subject.service";
import { UserService } from "@app/admin/services/user.service";
import { RestSourceUser } from '@app/admin/models/rest-source-user.model';
import { UserDialogMode } from "@app/admin/containers/user-dialog/user-dialog.component";

export interface UserData extends RestSourceUser {
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
  options?: { value: string | boolean | number, label: string }[],
  modelValue?: any,
}

@Component({
  selector: 'app-users-list',
  templateUrl: './users-list.component.html',
  styleUrls: ['./users-list.component.scss']
})
export class UsersListComponent implements AfterViewInit {
  UserDialogMode = UserDialogMode;

  error?: string;

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort: MatSort = new MatSort();

  dataSource: MatTableDataSource<UserData> = new MatTableDataSource<UserData>();

  displayedColumns: string[] = [
    'userId',
    'externalId',
    'startDate',
    'endDate',
    'isAuthorized',
    'registrationCreatedAt',
    'actions'
  ];

  filterEnabled = false;
  filterValues: { [key: string]: string | undefined; } = {};
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
      options: [
        {value: 'yes', label: 'ADMIN.USERS_LIST.authorizationStatus.yes'},
        {value: 'pending', label: 'ADMIN.USERS_LIST.authorizationStatus.pending'},
        {value: 'no', label: 'ADMIN.USERS_LIST.authorizationStatus.no'},
        {value: 'unset', label: 'ADMIN.USERS_LIST.authorizationStatus.unset'}
      ],
      width: 150,
    }
  ]

  matSortActive!: string;
  matSortDirection!: 'asc' | 'desc';

  @ViewChild('templateBottomSheet') TemplateBottomSheet!: TemplateRef<any>;

  @Input('users') set users(users: RestSourceUser[]) {
    this.dataSource.data = users as UserData[];
  }

  @Output() actionClicked: EventEmitter<{ mode: UserDialogMode, user: RestSourceUser }> =
    new EventEmitter<{ mode: UserDialogMode, user: RestSourceUser }>();

  constructor(
    private userService: UserService,
    private subjectService: SubjectService,
    private bottomSheet: MatBottomSheet,
    private router: Router,
    private activatedRoute: ActivatedRoute,
  ) {
  }

  ngAfterViewInit() {
    this.applyTableSort();
    this.dataSource.paginator = this.paginator;
    this.dataSource.filterPredicate = this.createTableFilterPredicate();
    setTimeout(() => this.checkActiveQuery());
    this.listenToStateChangeEvents();
  }

  //#region Sort and Filter
  private applyTableSort(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.sortingDataAccessor = (item: UserData, property: string) => {
      switch (property) {
        case 'isAuthorized':
          if (item.isAuthorized) {
            return 1; //'Yes';
          } else if (!item.id) {
            return 4; //'Unset';
          } else if (item.registrationCreatedAt) {
            return 2; //'Pending';
          } else {
            return 3; //'No';
          }
        case 'startDate': case 'endDate':
          return item[property] ? new Date(item[property]) : null;
        case  'registrationCreatedAt':
          return item.registrationCreatedAt ? new Date(item.registrationCreatedAt) : null;
        default:
          return item[property].toLocaleLowerCase();
      }
    };
  }

  private createTableFilterPredicate() {
    return function (data: UserData, filter: string): boolean {
      const searchTerms = JSON.parse(filter);
      return Object.entries(searchTerms)
        .filter(([, value]) => value)
        .map(([key, value]) => [key, (value as any).toString().toLocaleLowerCase()])
        .every(([key, value]) => {
          switch (key) {
            case 'userId':
              const matchesUserId = data.userId && data.userId.toLocaleLowerCase().indexOf(value) !== -1
              const matchesExternalId = !data.externalId || data.externalId.toLocaleLowerCase().indexOf(value) !== -1;
              return matchesUserId || matchesExternalId;
            case 'isAuthorized':
              if (data.isAuthorized) {
                return value === 'yes';
              } else if (!data.id) {
                return value === 'unset';
              } else if (data.registrationCreatedAt) {
                return value === 'pending';
              } else {
                return value === 'no';
              }
            default:
              return true;
          }
        });
    }
  }

  filterChange(filter: FilterItem, event: any) {
    if (event.value) {
      this.filterValues[filter.columnProp] = event.value;
    } else {
      delete this.filterValues[filter.columnProp];
    }
    this.filterEnabled = this.isFilterEnabled();
    this.dataSource.filter = JSON.stringify(this.filterValues);
    this.applyStateChangesToUrlQueryParams({[filter.columnProp]: event.value !== '' ? event.value : null})
  }

  resetFilters() {
    this.filterEnabled = false;
    this.filterValues = {}
    this.filters.forEach((value: any, _: any) => {
      value.modelValue = undefined;
    })
    this.dataSource.filter = "";
    this.applyStateChangesToUrlQueryParams({isAuthorized: null, userId: null});
  }

  isFilterEnabled(): boolean {
    return Object.entries(this.filterValues).length > 0;
  }

  //#endregion

  //#region User Actions
  onActionClick(mode: UserDialogMode, user: RestSourceUser) {
    this.actionClicked.emit({mode: mode, user: user});
  }

  //#endregion

  //#region Query Params
  private checkActiveQuery(): void {
    this.checkActivePageQuery();
    this.checkActiveSortQuery();
    this.checkActiveFilterQuery();
  }

  private checkActiveFilterQuery(): void {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    if (queryParams.hasOwnProperty('userId') || queryParams.hasOwnProperty('isAuthorized')) {
      this.filters.forEach((value: any, _: any) => {
        if (value.columnProp === 'userId') {
          if (queryParams.userId) {
            value.modelValue = queryParams.userId;
            this.filterValues.userId = queryParams.userId;
            this.filterEnabled = true;
          } else {
            value.modelValue = undefined;
            this.filterValues.userId = undefined;
          }
        }
        if (value.columnProp === 'isAuthorized') {
          if (queryParams.isAuthorized) {
            value.modelValue = queryParams.isAuthorized;
            this.filterValues.isAuthorized = queryParams.isAuthorized;
            this.filterEnabled = true;
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
      const activeSortHeader: any = this.dataSource.sort!.sortables.get(sortActiveColumn);
      if (!activeSortHeader) {
        this.applyStateChangesToUrlQueryParams({
          sortField: null,
          sortOrder: null,
        });
        return;
      }
      activeSortHeader['_setAnimationTransitionState']({
        fromState: this.dataSource.sort!.direction,
        toState: 'active',
      });
      const sortable: MatSortable = {
        id: sortActiveColumn,
        start: queryParams.sortOrder || this.matSortDirection,
        disableClear: true
      };
      this.dataSource.sort!.sort(sortable);
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
        sortField: sortChange.direction ? sortChange.active || null : null,
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
    this.router.navigate([], {queryParams: queryParams, queryParamsHandling: 'merge'}).finally();
  }

  //#endregion

  openTemplateSheetMenu() {
    this.bottomSheet.open(this.TemplateBottomSheet);
  }

}
