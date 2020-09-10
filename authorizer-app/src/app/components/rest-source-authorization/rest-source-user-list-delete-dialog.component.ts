import {
  AfterViewInit,
  Component,
  Inject,
  OnInit,
  ViewChild
} from '@angular/core';
import {
  MAT_DIALOG_DATA,
  MatDatepickerInputEvent,
  MatDialog,
  MatDialogRef,
  MatPaginator,
  MatSort,
  MatTableDataSource
} from '@angular/material';
import { RestSourceUser } from '../../models/rest-source-user.model';
import { RestSourceUserService } from '../../services/rest-source-user.service';
import {FormControl, FormGroup} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import * as moment from 'moment';
import deepcopy from 'ts-deepcopy';
import {RestSourceProject} from "../../models/rest-source-project.model";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'rest-source-list',
  templateUrl: './rest-source-user-list.component.html',
  styleUrls: ['./rest-source-user-list.component.css']
})
export class RestSourceUserListComponent implements OnInit, AfterViewInit {
  displayedColumns = [
    'id',
    // 'projectId',
    'userId',
    // 'sourceId',
    'startDate',
    'endDate',
    'externalUserId',
    'authorized',
    // 'version',
    'edit',
    'reset',
    'delete'
  ];
  columnsToDisplay = [
    'id',
    'userId',
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
  restSourceProjects: RestSourceProject[];
  selectedProject: string = ''
  //public isCollapsed = true;

  dataSource: MatTableDataSource<RestSourceUser>;

  constructor(
    private restSourceUserService: RestSourceUserService,
    public dialog: MatDialog,
    private router: Router,
    private activatedRoute: ActivatedRoute,

  ) {}

  ngOnInit() {
    console.log(this.activatedRoute.snapshot.queryParams.project)
    this.selectedProject = this.activatedRoute.snapshot.queryParams.project;

    this.loadAllRestSourceProjects()
    //this.loadAllRestSourceUsers();
    this.dataSource = new MatTableDataSource(this.restSourceUsers);
    this.dataSource.filterPredicate = function(data, filter: string): boolean {
      return data.id.toLowerCase().includes(filter) || data.userId.toLowerCase().includes(filter) || data.externalUserId.toString().includes(filter);
    };
    this.onChangeProject(this.selectedProject)
  }

  /**
   * Set the paginator and sort after the view init since this component will
   * be able to query its view for the initialized paginator and sort.
   */
  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  applyFilter(filterValue: string) {
    filterValue = filterValue.trim(); // Remove whitespace
    filterValue = filterValue.toLowerCase(); // Datasource defaults to lowercase matches
    this.dataSource.filter = filterValue;


  }

  private loadAllRestSourceUsers() {
    this.restSourceUserService.getAllUsers().subscribe(
      (data: any) => {
        this.restSourceUsers = data.users;
        this.dataSource.data = this.restSourceUsers;
        console.log(this.restSourceUsers)
      },
      () => {
        this.errorMessage = 'Cannot load registered users!';
      }
    );
  }

  private loadAllRestSourceUsersOfProject(projectId: string) {
    this.restSourceUserService.getAllUsersOfProject(projectId).subscribe(
      (data: any) => {
        this.restSourceUsers = data.users;
        this.dataSource.data = this.restSourceUsers;
        console.log("loadAllRestSourceUsersOfProject", this.restSourceUsers)
      },
      () => {
        this.errorMessage = 'Cannot load registered users!';
      }
    );
  }

  private loadAllRestSourceProjects() {
    this.restSourceUserService.getAllProjects().subscribe(
      (data: any) => {
        console.log(data)
        this.restSourceProjects = data.projects;
        // this.dataSource.data = this.restSourceUsers;


      },
      () => {
        this.errorMessage = 'Cannot load projects!';
      }
    );
  }

  removeDevice(restSourceUser: RestSourceUser) {
    this.restSourceUserService.deleteUser(restSourceUser.id).subscribe(() => {
      this.loadAllRestSourceUsers();
    });
  }

  resetUser(restSourceUser: RestSourceUser) {
    this.restSourceUserService.resetUser(restSourceUser).subscribe(() => {
      this.loadAllRestSourceUsers();
    });
  }

  openDeleteDialog(restSourceUser: RestSourceUser) {
    const dialogRef = this.dialog.open(RestSourceUserListDeleteDialog, {
      data: restSourceUser
    });

    dialogRef.afterClosed().subscribe(user => {
      console.log('Deleting user...');
      this.removeDevice(user);
    });
  }

  openResetDialog(restSourceUser: RestSourceUser) {
    const dialogRef = this.dialog.open(RestSourceUserListResetDialog, {
      data: restSourceUser
    });

    dialogRef.afterClosed().subscribe((user: RestSourceUser) => {
        console.log('Resetting user...', user);
        this.resetUser(user);
    });
  }

  onChangeProject(projectId: string) {
    this.selectedProject = projectId
    console.log("change project", projectId)
    if(projectId === ''){
     //this.loadAllRestSourceUsers()
    }else{
      console.log('get users')
      this.loadAllRestSourceUsersOfProject(projectId)
      this.applyFilter("")
    }
    this.router.navigate(['/users'], {queryParams: {project: this.selectedProject}});
  }

  // onChangeUser(userId: string) {
  //   console.log("change user", userId)
  //   if(userId!=='all') {
  //     this.applyFilter(userId)
  //   }else{
  //     this.applyFilter("")
  //   }
  // }
}

@Component({
  selector: 'rest-source-user-list-delete-dialog',
  templateUrl: 'rest-source-user-list-delete-dialog.html'
})
export class RestSourceUserListDeleteDialog {
  constructor(
    public dialogRef: MatDialogRef<RestSourceUserListDeleteDialog>,
    @Inject(MAT_DIALOG_DATA) public data: RestSourceUser
  ) {}

  closeDeleteDialog(): void {
    this.dialogRef.close();
  }
}

@Component({
  selector: 'rest-source-user-list-reset-dialog',
  templateUrl: 'rest-source-user-list-reset-dialog.html'
})
export class RestSourceUserListResetDialog {
  startDateFormControl: FormControl;
  endDateFormControl: FormControl;

  // Stores a copy of the data so as to not modify the original content
  dataCopy: RestSourceUser;

  constructor(
    public dialogRef: MatDialogRef<RestSourceUserListDeleteDialog>,
    @Inject(MAT_DIALOG_DATA) public data: RestSourceUser
  ) {
    this.startDateFormControl = new FormControl(moment(this.data.startDate));
    this.endDateFormControl = new FormControl(moment(this.data.endDate));
    this.dataCopy = deepcopy(this.data);
  }

  closeResetDialog(): void {
    this.dialogRef.close();
  }

  updateStartDateValue(event: MatDatepickerInputEvent<any>) {
    this.dataCopy.startDate = event.value.toISOString();
  }

  updateEndDateValue(event: MatDatepickerInputEvent<any>) {
    this.dataCopy.endDate = event.value.toISOString();
  }
}
