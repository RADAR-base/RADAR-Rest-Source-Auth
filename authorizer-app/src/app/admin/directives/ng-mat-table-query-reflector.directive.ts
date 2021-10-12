import {Directive, Input, OnInit, OnDestroy, AfterViewInit} from '@angular/core';
import { Sort, MatSortable } from '@angular/material/sort';
import { Router, ActivatedRoute } from '@angular/router';
import { Subject, interval, Subscription } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatTableDataSource } from '@angular/material/table';
import { PageEvent } from '@angular/material/paginator';

@Directive({
  selector: '[NgMatTableQueryReflector]'
})
export class NgMatTableQueryReflectorDirective implements OnInit, OnDestroy {

  private unsubscribeAll$: Subject<any> = new Subject();

  @Input() matSortActive!: string;
  @Input() matSortDirection!: 'asc' | 'desc';
  @Input() dataSource!: MatTableDataSource<any>;
  private _dataSourceChecker$?: Subscription;

  constructor(
    private _router: Router,
    private _activatedRoute: ActivatedRoute
  ) { }

  async ngOnInit(): Promise<void> {
    console.log('directive 0')
    await this.waitForDatasourceToLoad();
    this._initialSetup();
    this.listenToStateChangeEvents();
    console.log('directive 1')

  }

  // async ngAfterViewInit(): Promise<void> {
  //   console.log('directive after 0')
  //   await this.waitForDatasourceToLoad();
  //   this._initialSetup();
  //   this.listenToStateChangeEvents();
  //   console.log('directive after 1')
  //
  // }

  private _initialSetup(): void {

    const activePageQuery = this.isPageQueryActive();
    //
    // console.log(activePageQuery)

    // console.log(this.pageSize, this.pageIndex);
    // this.paginator.pageIndex = 2;
    // this.paginator.pageIndex = this.pageIndex; // pageNumber; // number of the page you want to jump.
    // this.paginator.pageSize = this.pageSize; // pageNumber; // number of the page you want to jump.
    // this.paginator.page.next({
    //   pageIndex: this.pageIndex, //pageNumber,
    //   pageSize: this.pageSize, // this.paginator.pageSize,
    //   length: this.paginator.length
    // });

    if (activePageQuery) {
      this.dataSource.paginator!.pageIndex = activePageQuery.pageIndex;
      // setTimeout(() => this.dataSource.paginator!.pageSize = 25);
      this.dataSource.paginator!.pageSize = activePageQuery.pageSize;

      console.log(this.dataSource.paginator!.pageSize)
      console.log(this.dataSource.paginator!.pageIndex)
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

  }

  private isSortQueryActive(): { sortField: string, sortOrder: 'asc' | 'desc' } | undefined {

    const queryParams = this._activatedRoute.snapshot.queryParams;

    if (queryParams.hasOwnProperty('sortField') || queryParams.hasOwnProperty('sortOrder')) {
      return {
        sortField: queryParams.sortField,
        sortOrder: queryParams.sortOrder
      };
    }

    return;
  }

  private isPageQueryActive(): { pageSize: number, pageIndex: number } | undefined{

    const queryParams = this._activatedRoute.snapshot.queryParams;

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
      .pipe(
        takeUntil(this.unsubscribeAll$)
      )
      .subscribe((sortChange: Sort) => {
        this._applySortChangesToUrlQueryParams(sortChange);
      });

    this.dataSource.paginator!.page
      .pipe(
        takeUntil(this.unsubscribeAll$)
      )
      .subscribe((pageChange: PageEvent) => {
        this._applyPageStateChangesToUrlQueryParams(pageChange);
      });
  }

  private _applySortChangesToUrlQueryParams(sortChange: Sort): void {

    const sortingAndPaginationQueryParams = {
      sortField: sortChange.direction? sortChange.active || null : null,
      sortOrder: sortChange.direction || null,
    };

    this._router.navigate([], { queryParams: sortingAndPaginationQueryParams, queryParamsHandling: 'merge' }).finally();
  }

  private _applyPageStateChangesToUrlQueryParams(pageChange: PageEvent): void {

    const sortingAndPaginationQueryParams = {
      pageSize: pageChange.pageSize,
      pageIndex: pageChange.pageIndex,
    };

    this._router.navigate([], { queryParams: sortingAndPaginationQueryParams, queryParamsHandling: 'merge' }).finally();
  }

  private waitForDatasourceToLoad(): Promise<void> {

    const titleCheckingInterval$ = interval(500);

    return new Promise((resolve) => {
      this._dataSourceChecker$ = titleCheckingInterval$.subscribe(_ => {
        if (this.dataSource?.sort && this.dataSource?.paginator) {
          this._dataSourceChecker$?.unsubscribe();
          return resolve();
        }
      });
    });

  }

  ngOnDestroy(): void {
    this.unsubscribeAll$.next('');
    this.unsubscribeAll$.complete();
  }

}
