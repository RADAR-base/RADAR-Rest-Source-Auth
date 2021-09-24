import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {FormBuilder, Validators} from '@angular/forms';
import {MatSelectChange} from '@angular/material/select';
import {RadarProject} from "../../models/rest-source-project.model";
import {StorageItem} from "../../../shared/enums/storage-item";

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.scss']
})
export class DashboardPageComponent implements OnInit {

  projects: RadarProject[] = this.activatedRoute.snapshot.data.projects;
  sourceTypes: any = this.activatedRoute.snapshot.data.sourceTypes;
  selectedProject?: string;

  form = this.fb.group({
    projectId: [null, Validators.required],
  });

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private activatedRoute: ActivatedRoute
  ) {}

  ngOnInit() {
    console.log(this.sourceTypes);
    this.activatedRoute.queryParams.subscribe({
      next: ({project}) => {
        if (project) {
          this.form.setValue({projectId: project})
          this.selectedProject = project;
        }
      }
    });
  }

  onProjectSelectionChange(e: MatSelectChange) {
    const project = e.value;
    this.selectedProject = project;
    this.router.navigate([],
      {
        relativeTo: this.activatedRoute,
        queryParams: {project: project},
        queryParamsHandling: 'merge', // remove to replace all query params by provided
      }).finally(()=> localStorage.setItem(StorageItem.SAVED_URL, `/?project=${project}`));
  }
}
