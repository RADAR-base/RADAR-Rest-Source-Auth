import { AfterViewInit, Component, OnInit, ViewChild } from '@angular/core';
import { MatPaginator, MatSort, MatTableDataSource } from '@angular/material';

import { RestSourceUser } from '../../models/rest-source-user.model';
import { RestSourceUserService } from '../../services/rest-source-user.service';

@Component({
  selector: 'rest-source-list',
  templateUrl: './rest-source-user-list.component.html',
  styleUrls: ['./rest-source-user-list.component.css']
})
export class RestSourceUserListComponent implements OnInit, AfterViewInit {
  
  displayedColumns = ['id', 'projectId', 'userId', 'sourceId', 'startDate',
    'endDate', 'externalUserId', 'authorized', 'edit', 'delete'];

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  errorMessage: string;
  restSourceUsers: RestSourceUser[];
  public isCollapsed = true;

  dataSource: MatTableDataSource<RestSourceUser>;

  constructor(private restSourceUserService: RestSourceUserService) {}

  ngOnInit() {
    this.loadAllRestSourceUsers();
    this.dataSource = new MatTableDataSource(this.restSourceUsers);
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
    this.restSourceUserService.getAllUsersFromAssignedProjects().subscribe(
      (users: any) => {
        this.restSourceUsers = users;
        this.dataSource.data = this.restSourceUsers;
      },
      () => {
        this.errorMessage = 'Cannot load registered users!';
      }
    );
  }

  removeDevice(restSourceUser: RestSourceUser) {
    this.restSourceUserService.deleteUser(restSourceUser.id).subscribe(() => {
      this.loadAllRestSourceUsers();
    });
  }
}
