import { ActivatedRoute, Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';

import { RadarProject } from '../../models/rest-source-project.model';
import { RestSourceUserService } from '../../services/rest-source-user.service';
import {RestSourceUserMockService} from '../../services/rest-source-user-mock.service';
import {storageItems} from "../../enums/storage";

@Component({
  selector: 'rest-source-dashboard',
  templateUrl: './rest-source-user-dashboard.component.html',
  styleUrls: ['./rest-source-user-dashboard.component.css']
})
export class RestSourceUserDashboardComponent implements OnInit {
  errorMessage: string;
  restSourceProjects: RadarProject[];
  selectedProject: string;

  constructor(
    private restSourceUserService: RestSourceUserService,
    // private restSourceUserService: RestSourceUserMockService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {}

  ngOnInit() {
    this.loadAllRestSourceProjects();
    this.selectedProject = this.activatedRoute.snapshot.queryParams.project;
  }

  private loadAllRestSourceProjects() {
    this.restSourceUserService.getAllProjects().subscribe(
      (data: any) => {
        this.restSourceProjects = data; // data.projects;
      },
      () => {
        this.errorMessage = 'Cannot load projects!';
      }
    );
  }

  updateProject(projectId: string) {
    this.selectedProject = projectId;
    if (projectId) {
      return this.router.navigate(['/users'], {
        queryParams: { project: projectId }
      }).then(()=> localStorage.setItem(storageItems.project, projectId));
    } else {
      return this.router.navigate(['/users']).then(() => localStorage.removeItem(storageItems.project));
    }
  }
}
