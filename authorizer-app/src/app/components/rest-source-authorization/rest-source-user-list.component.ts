import { ActivatedRoute, Router } from '@angular/router';
import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import {
  MatDialog,
  MatPaginator,
  MatSort,
  MatTableDataSource
} from '@angular/material';

import { RadarProject } from '../../models/rest-source-project.model';
import { RestSourceUser } from '../../models/rest-source-user.model';
import { RestSourceUserListDeleteDialog } from './rest-source-user-list-delete-dialog.component';
import { RestSourceUserListResetDialog } from './rest-source-user-list-reset-dialog.component';
import { RestSourceUserService } from '../../services/rest-source-user.service';

@Component({
  selector: 'rest-source-list',
  templateUrl: './rest-source-user-list.component.html',
  styleUrls: ['./rest-source-user-list.component.css']
})
export class RestSourceUserListComponent implements OnInit, AfterViewInit {
  columnsToDisplay = [
    'id',
    'userId',
    'sourceType',
    'externalUserId',
    'startDate',
    'endDate',
    'authorized',
    'actions'
  ];

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  errorMessage: string;
  restSourceUsers: RestSourceUser[];
  restSourceProjects: RadarProject[];
  selectedProject = '';
  totalItems = 0;

  dataSource: MatTableDataSource<RestSourceUser>;

  constructor(
    private restSourceUserService: RestSourceUserService,
    public dialog: MatDialog,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {}

  ngOnInit() {
    this.selectedProject = this.activatedRoute.snapshot.queryParams.project;

    this.loadAllRestSourceProjects();
    this.dataSource = new MatTableDataSource(this.restSourceUsers);
    this.dataSource.filterPredicate = function(data, filter: string): boolean {
      return (
        data.id.toLowerCase().includes(filter) ||
        data.userId.toLowerCase().includes(filter) ||
        data.externalId.toString().includes(filter)
      );
    };
    this.onChangeProject(this.selectedProject);
  }

  /**
   * Set the paginator and sort after the view init since this component will
   * be able to query its view for the initialized paginator and sort.
   */
  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
  }

  applyFilter(filterValue: string) {
    filterValue = filterValue.trim(); // Remove whitespace
    filterValue = filterValue.toLowerCase(); // Datasource defaults to lowercase matches
    this.dataSource.filter = filterValue;
  }

  private loadAllRestSourceUsersOfProject(
    projectId: string,
    page: number,
    pageSize: number
  ) {
    this.restSourceUserService
      .getAllUsersOfProject(projectId, { page: page, size: pageSize })
      .subscribe(
        (res: HttpResponse<any>) => {
          this.restSourceUsers = res.body.users;
          this.dataSource.data = this.restSourceUsers;
          this.totalItems = parseInt(res.headers.get('x-total-count'));
        },
        (res: HttpErrorResponse) => {
          this.errorMessage = 'Cannot load registered users!';
        }
      );
  }

  private loadAllRestSourceProjects() {
    this.restSourceUserService.getAllProjects().subscribe(
      (data: any) => {
        this.restSourceProjects = data.projects;
      },
      () => {
        this.errorMessage = 'Cannot load projects!';
      }
    );
  }

  loadPage(event) {
    this.loadAllRestSourceUsersOfProject(
      this.selectedProject,
      event.pageIndex + 1,
      event.pageSize
    );
  }

  removeDevice(restSourceUser: RestSourceUser) {
    this.restSourceUserService.deleteUser(restSourceUser.id).subscribe(() => {
      this.loadAllRestSourceUsersOfProject(this.selectedProject, 1, 20);
    });
  }

  resetUser(restSourceUser: RestSourceUser) {
    this.restSourceUserService.resetUser(restSourceUser).subscribe(() => {
      this.loadAllRestSourceUsersOfProject(this.selectedProject, 1, 20);
    });
  }

  openDeleteDialog(restSourceUser: RestSourceUser) {
    const dialogRef = this.dialog.open(RestSourceUserListDeleteDialog, {
      data: restSourceUser
    });

    dialogRef.afterClosed().subscribe(user => {
      if (user) {
        console.log('Deleting user...', user);
        this.removeDevice(user);
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

  onChangeProject(projectId: string) {
    this.selectedProject = projectId;
    if (projectId) {
      this.loadAllRestSourceUsersOfProject(projectId, 1, 20);
      this.applyFilter('');
      this.router.navigate(['/users'], {
        queryParams: { project: this.selectedProject }
      });
    } else {
      this.router.navigate(['/users']);
    }
  }
}
