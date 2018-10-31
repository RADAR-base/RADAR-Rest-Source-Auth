import {Component, OnInit} from '@angular/core';
import {RestSourceUser} from "../../models/rest-source-user.model";
import {RestSourceUserService} from "../../services/rest-source-user.service";

@Component({
  selector: 'rest-source-list',
  templateUrl: './rest-source-user-list.component.html',
})
export class RestSourceUserListComponent implements OnInit {
  errorMessage: string;
  restSourceUsers: RestSourceUser[];
  public isCollapsed = true;

  constructor(private restSourceUserService: RestSourceUserService) {}

  ngOnInit() {
    this.loadAllRestSourceUsers();
  }

  private loadAllRestSourceUsers() {
    this.restSourceUserService.getAllUsers().subscribe((data: any) => {
        this.restSourceUsers = data.users;
      },
      () => {
        this.errorMessage = 'Cannot load registered users!';
      });
  }

  removeDevice(restSourceUser: RestSourceUser) {
    this.restSourceUserService.deleteUser(restSourceUser.id).subscribe(() => {
      this.loadAllRestSourceUsers();
    });
  }
}
