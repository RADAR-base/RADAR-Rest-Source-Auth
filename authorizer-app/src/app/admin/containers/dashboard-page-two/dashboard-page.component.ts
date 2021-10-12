import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {FormBuilder, Validators} from '@angular/forms';
import {MatSelectChange} from '@angular/material/select';

import {RadarProject, RadarSourceClient, RadarSubject} from "@app/admin/models/radar-entities.model";
import {StorageItem} from "@app/shared/enums/storage-item";
import {Location} from "@angular/common";
import {BehaviorSubject, combineLatest, distinctUntilChanged, filter, Observable, switchMap} from "rxjs";
import {RestSourceUser} from "@app/admin/models/rest-source-user.model";
import {map} from "rxjs/operators";
import {UserData} from "@app/admin/components/users-list-two/users-list.component";
import {UserService} from "@app/admin/services/user.service";
import {SubjectService} from "@app/admin/services/subject.service";

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.scss']
})
export class DashboardPageComponent implements OnInit, OnDestroy {

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

  // @Input('project') set project(project: string) { this.projectSubject.next(project); }
  private projectSubject = new BehaviorSubject<string>('');

  project$ = this.projectSubject.asObservable().pipe(
    filter(d => !!d),
    distinctUntilChanged(),
  );

  // @Input('sourceClient') set sourceClient(sourceClient: string) { this.sourceClientSubject.next(sourceClient); }
  private sourceClientSubject = new BehaviorSubject<string>('');

  sourceClient$ = this.sourceClientSubject.asObservable().pipe(
    filter(d => !!d),
    distinctUntilChanged(),
  );

  updateTriggerSubject = new BehaviorSubject<string>('init');
  subjects$?: Observable<RadarSubject[]>;
  users$?: Observable<RestSourceUser[]>;

  loading = true;
  error?: any;

  modifiedUsers?: RestSourceUser[];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private userService: UserService,
    private subjectService: SubjectService,
    // private location: Location
  ) {}

  ngOnInit() {
    this.loadTableData()
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
          }).finally(()=> localStorage.setItem(StorageItem.SAVED_URL, `/?project=${this.selectedProject}`));

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

  ngOnDestroy() {
    this.updateTriggerSubject.complete();
  }

  onProjectSelectionChange(e: MatSelectChange) {
    console.log('project changed', e.value)
    const project = e.value;
    this.selectedProject = project;
    this.projectSubject.next(project);
    //this.loadTableData();
    this.router.navigate([],
      {
        relativeTo: this.activatedRoute,
        queryParams: {project: project},
        queryParamsHandling: 'merge', // remove to replace all query params by provided
      }).finally(()=> localStorage.setItem(StorageItem.SAVED_URL, `/?project=${project}`));
  }

  onSourceClientSelectionChange(e: MatSelectChange) {
    const sourceClient = e.value;
    this.selectedSourceClient = sourceClient;
    //this.loadTableData();
    this.sourceClientSubject.next(sourceClient);
    this.router.navigate([],
        {
          relativeTo: this.activatedRoute,
          queryParams: {sourceClient: sourceClient},
          queryParamsHandling: 'merge', // remove to replace all query params by provided
        }).finally(()=> localStorage.setItem(StorageItem.SAVED_URL, `/?sourceType=${sourceClient}`));
  }

  loadTableData(): void {
    console.log('loadTableData');
    combineLatest([this.updateTriggerSubject, this.project$, this.sourceClient$]).pipe(
      switchMap(
        ([_, project, sourceClient]) => {
          console.log(project, sourceClient);
          return this.loadAndModifyUsers(project, sourceClient)
        }
      )
    ).subscribe({
      next: users => {
        console.log(users);
        // this.dataSource.data = users;
        this.modifiedUsers = users;
        this.loading = false;
      },
      error: (error) => {
        this.error = error;
        this.loading = false;
      }
    });
  }

  loadAndModifyUsers(project: string, sourceClient: string): Observable<UserData[]> {
    console.log(project, sourceClient);
    this.subjects$ = this.subjectService.getSubjectsOfProject(project);
    this.users$ = this.userService.getUsersOfProject(project).pipe(
      map(resp => resp.users)
    );
    return combineLatest([this.subjects$, this.users$]).pipe(
      map(([subjects, users]) => {
        const newSubjects: any[] = [];
        subjects?.map(subject => {
          // this.sourceClients.map(sourceClient => {
          //   const newSubject = {...subject, sourceType: sourceClient.sourceType}
          //   newSubjects.push(newSubject)
          // })
          const newSubject = {...subject, sourceType: sourceClient};
          console.log(newSubject, sourceClient);
          newSubjects.push(newSubject)
        });
        return newSubjects.map(subject => {
          const user = users.filter(user => {
            return user.userId === subject.id && user.sourceType === subject.sourceType
          })[0];
          console.log(user);
          return {
            ...subject,
            id: null,
            ...user,
            userId: subject.id,
            isAuthorized: !user?.isAuthorized ? false : user.isAuthorized
          }
        })
      }),
    )
    // return this.userService.getUsersOfProject(project).pipe(
    //     catchError(() => of({users: [], metadata: { pageNumber: 1, pageSize: page.pageSize, totalElements: 0 }} as RestSourceUsers)),
    //   );
  }

}
