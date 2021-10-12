import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {FormBuilder, Validators} from '@angular/forms';
import {MatSelectChange} from '@angular/material/select';

import {RadarProject, RadarSourceClient} from "@app/admin/models/radar-entities.model";
import {StorageItem} from "@app/shared/enums/storage-item";
import {Location} from "@angular/common";

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.scss']
})
export class DashboardPageComponent implements OnInit {

  // projects: RadarProject[] = [...[], this.activatedRoute.snapshot.data.projects[0]];
  projects: RadarProject[] = this.activatedRoute.snapshot.data.projects;
  // sourceClients: RadarSourceClient[] = this.activatedRoute.snapshot.data.sourceClients;
  sourceClients: RadarSourceClient[] = [
      {
        authorizationEndpoint: "https://www.fitbit.com/oauth2/authorize",
        clientId: "239Z46",
        scope: "activity heartrate sleep profile",
        sourceType: "FitBit",
        tokenEndpoint: "https://api.fitbit.com/oauth2/token",
      },
      {
        authorizationEndpoint: "https://www.fitbit1.com/oauth2/authorize",
        clientId: "239Z461",
        scope: "activity1 heartrate sleep profile",
        sourceType: "Withings",
        tokenEndpoint: "https://api.fitbit.com/oauth2/token1",
      }
    ];

  selectedProject?: string = this.projects.length === 1 ? this.projects[0].id : undefined;
  selectedSourceClient?: string = this.sourceClients.length === 1 ? this.sourceClients[0].sourceType : undefined;

  form = this.fb.group({
    project: [null, Validators.required],
    sourceClient: [null, Validators.required]
  });

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    // private location: Location
  ) {}

  ngOnInit() {
    // this.location.subscribe(location => {
    //   console.log(location)
    // })
    console.log(this.selectedProject, this.selectedSourceClient)
    console.log(this.sourceClients);
    this.activatedRoute.queryParams.subscribe({
      next: (params) => {
        console.log(params)
        localStorage.setItem(StorageItem.SAVED_URL, JSON.stringify(params));
        const {project, sourceClient} = params;
        console.log(project, sourceClient)
        // this.form.setValue({project: project || null, sourceClient: sourceClient || null})
        // this.selectedProject = project;
        // this.selectedSourceClient = sourceClient;

        if (project && sourceClient) {
          this.selectedProject = this.projects.filter(p => p.id === project)[0]?.id || undefined;
          this.selectedSourceClient = this.sourceClients.filter(s => s.sourceType === sourceClient)[0]?.sourceType || undefined;
          // this.form.setValue({project: project, sourceClient: sourceClient})
          this.form.setValue({project: this.selectedProject || null, sourceClient: this.selectedSourceClient || null})
        } else if (project && !sourceClient) {
          this.selectedProject = this.projects.filter(p => p.id === project)[0]?.id || undefined;
          // this.selectedProject = project;
          this.form.setValue({project: this.selectedProject || null, sourceClient: this.selectedSourceClient || null})
          // this.form.setValue({project: project, sourceClient: null})
          // this.selectedSourceClient = undefined;
        } else {
          // this.form.setValue({project: null, sourceClient: null})
          this.form.setValue({project: this.selectedProject || null, sourceClient: this.selectedSourceClient || null})
          // this.selectedProject = undefined;
          // this.selectedSourceClient = undefined;
        }
      }
    });

      this.router.navigate([],
          {
            relativeTo: this.activatedRoute,
            queryParams: {project: this.selectedProject, sourceClient: this.selectedSourceClient},
            queryParamsHandling: 'merge', // remove to replace all query params by provided
          }).finally();

    // if(this.sourceClients.length === 1) {
    //   const sourceClient = this.sourceClients[0].sourceType;
    //   this.router.navigate([],
    //       {
    //         relativeTo: this.activatedRoute,
    //         queryParams: {sourceClient: sourceClient},
    //         queryParamsHandling: 'merge', // remove to replace all query params by provided
    //       }).finally(()=> localStorage.setItem(StorageItem.SAVED_URL, `/?sourceType=${sourceClient}`));
    // }
  }

  onProjectSelectionChange(e: MatSelectChange) {
    console.log('project changed', e.value)
    const project = e.value;
    this.selectedProject = project;
    this.router.navigate([],
      {
        relativeTo: this.activatedRoute,
        queryParams: {project: project},
        queryParamsHandling: 'merge', // remove to replace all query params by provided
      }).finally();
  }

  onSourceClientSelectionChange(e: MatSelectChange) {
    const sourceClient = e.value;
    this.selectedSourceClient = sourceClient;
    this.router.navigate([],
        {
          relativeTo: this.activatedRoute,
          queryParams: {sourceClient: sourceClient},
          queryParamsHandling: 'merge', // remove to replace all query params by provided
        }).finally();
  }
}
