import { ActivatedRoute, Router } from '@angular/router';
import { Component, OnInit } from '@angular/core';

// import { RadarProject } from '../../models/rest-source-project.model';
// import { RestSourceUserService } from '../../services/rest-source-user.service';
import {FormBuilder, Validators} from '@angular/forms';
import {MatSelectChange} from '@angular/material/select';
import {RestSourceUserMockService} from '../../services/rest-source-user-mock.service';
// import {RestSourceUserMockService} from '../../services/rest-source-user-mock.service';

@Component({
  selector: 'app-rest-source-dashboard',
  templateUrl: './rest-source-user-dashboard.component.html',
  styleUrls: ['./rest-source-user-dashboard.component.scss']
})
export class RestSourceUserDashboardComponent implements OnInit {
  // errorMessage?: string;
  // restSourceProjects?: RadarProject[];
  selectedProject?: string;
  projects$ = this.restSourceUserService.getAllProjects();

  form = this.fb.group({
    // sourceType: [null, Validators.required],
    projectId: [null, Validators.required],
    // userId: [null, Validators.required],
    // startDate: [null, Validators.required],
    // endDate: [null],
  });

  constructor(
    private fb: FormBuilder,
    // private restSourceUserService: RestSourceUserService,
    private restSourceUserService: RestSourceUserMockService,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {}

  ngOnInit() {
    // this.loadAllRestSourceProjects();
    this.selectedProject = this.activatedRoute.snapshot.queryParams.project;
  }

  // private loadAllRestSourceProjects() {
  //   this.restSourceUserService.getAllProjects().subscribe(
  //     (data: any) => {
  //       this.restSourceProjects = data; // data.projects;
  //     },
  //     () => {
  //       this.errorMessage = 'Cannot load projects!';
  //     }
  //   );
  // }

  updateProject(projectId: string) {
    this.selectedProject = projectId;
    if (projectId) {
      return this.router.navigate(['/users'], {
        queryParams: { project: projectId }
      });
    } else {
      return this.router.navigate(['/users']);
    }
  }

  onProjectSelectionChange(e: MatSelectChange) {
    // on project change users should be changed
    console.log(e);
    this.selectedProject = e.value;
    // this.service.getAllSubjectsOfProjects(e.value).subscribe(next => console.log(next));
    // this.subjects$ = this.service.getAllSubjectsOfProjects(e.value);
  }
}
