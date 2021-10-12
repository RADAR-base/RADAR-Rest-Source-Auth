import {Component, OnInit} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {FormBuilder, Validators} from '@angular/forms';
import {BehaviorSubject, combineLatest, distinctUntilChanged, filter, Observable, switchMap} from "rxjs";
import {map} from "rxjs/operators";
import {MatSelectChange} from '@angular/material/select';
import {MatDialog} from "@angular/material/dialog";

import {UserService} from "@app/admin/services/user.service";
import {SubjectService} from "@app/admin/services/subject.service";
import {UserDialogComponent} from "@app/admin/containers/user-dialog/user-dialog.component";
import {UserData} from "@app/admin/components/users-list/users-list.component";
import {RadarProject, RadarSourceClient, RadarSubject} from "@app/admin/models/radar-entities.model";
import {RestSourceUser} from "@app/admin/models/rest-source-user.model";
import {StorageItem} from "@app/shared/enums/storage-item";

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.scss']
})
export class DashboardPageComponent implements OnInit {
  loading = true;
  error?: any;

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

  private projectSubject = new BehaviorSubject<string>('');

  project$ = this.projectSubject.asObservable().pipe(
    filter(d => !!d),
    distinctUntilChanged(),
  );

  private sourceClientSubject = new BehaviorSubject<string>('');

  sourceClient$ = this.sourceClientSubject.asObservable().pipe(
    filter(d => !!d),
    distinctUntilChanged(),
  );

  updateTriggerSubject = new BehaviorSubject<string>('init');
  subjects$?: Observable<RadarSubject[]>;
  users$?: Observable<RestSourceUser[]>;

  form = this.fb.group({
    project: [null, Validators.required],
    sourceClient: [null, Validators.required]
  });

  users?: RestSourceUser[];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private userService: UserService,
    private subjectService: SubjectService,
    public dialog: MatDialog,
  ) {}

  ngOnInit() {
    this.loadTableData();

    this.activatedRoute.queryParams.subscribe({
      next: (params) => {
        localStorage.setItem(StorageItem.SAVED_URL, JSON.stringify(params));
        const {project, sourceClient} = params;
        if (project && sourceClient) {
          this.selectedProject = this.projects.filter(p => p.id === project)[0]?.id || undefined;
          this.selectedSourceClient = this.sourceClients.filter(s => s.sourceType === sourceClient)[0]?.sourceType || undefined;
          if(this.selectedProject) {
            this.projectSubject.next(this.selectedProject);
          }
          if(this.selectedSourceClient){
            this.sourceClientSubject.next(this.selectedSourceClient);
          }
          this.form.setValue({project: this.selectedProject || null, sourceClient: this.selectedSourceClient || null})
        } else if (project && !sourceClient) {
          this.selectedProject = this.projects.filter(p => p.id === project)[0]?.id || undefined;
          if(this.selectedProject) {
            this.projectSubject.next(this.selectedProject);
          }
          this.form.setValue({project: this.selectedProject || null, sourceClient: this.selectedSourceClient || null})
        } else {
          this.form.setValue({project: this.selectedProject || null, sourceClient: this.selectedSourceClient || null})
        }
      }
    });
    this.applyStateChangesToUrlQueryParams({project: this.selectedProject, sourceClient: this.selectedSourceClient});
  }

  onProjectSelectionChange(e: MatSelectChange) {
    const project = e.value;
    this.selectedProject = project;
    this.projectSubject.next(project)
    this.applyStateChangesToUrlQueryParams({project: project});
  }

  onSourceClientSelectionChange(e: MatSelectChange) {
    const sourceClient = e.value;
    this.selectedSourceClient = sourceClient;
    this.sourceClientSubject.next(sourceClient);
    this.applyStateChangesToUrlQueryParams({sourceClient: sourceClient});
  }

  loadTableData(): void {
    combineLatest([this.updateTriggerSubject, this.project$, this.sourceClient$]).pipe(
      switchMap(
        ([_, project, sourceClient]) => {
          return this.loadAndModifyUsers(project, sourceClient)
        }
      )
    ).subscribe({
      next: users => {
        this.users = users;
        this.checkActiveDialogQuery();
        this.loading = false;
      },
      error: (error) => {
        this.error = error;
        this.loading = false;
      }
    });
  }

  loadAndModifyUsers(project: string, sourceClient: string): Observable<UserData[]> {
    this.subjects$ = this.subjectService.getSubjectsOfProject(project);
    this.users$ = this.userService.getUsersOfProject(project).pipe(
      map(resp => resp.users)
    );
    return combineLatest([this.subjects$, this.users$]).pipe(
      map(([subjects, users]) => {
        const newSubjects: any[] = [];
        subjects?.map(subject => {
          const newSubject = {...subject, sourceType: sourceClient};
          newSubjects.push(newSubject)
        });
        return newSubjects.map(subject => {
          const user = users.filter(user => {
            return user.userId === subject.id && user.sourceType === subject.sourceType
          })[0];
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
  }

  private checkActiveDialogQuery(): void {
    const queryParams = this.activatedRoute.snapshot.queryParams;
    if (queryParams.hasOwnProperty('edit')) {
      console.log(this.users);
      const user = this.users?.filter(user => user.userId === queryParams.edit)[0];
      console.log(user);
      if(user){
        this.openSubjectDialog({mode: 'edit', user});
      } else {
        return;
      }
    }
    if (queryParams.hasOwnProperty('delete')){
      const user = this.users?.filter(user => user.userId === queryParams.delete)[0];
      if(user){
        this.openSubjectDialog({mode: 'delete', user});
      } else {
        return;
      }
    }
  }

  openSubjectDialog(e: {mode: string; user: RestSourceUser}) {
    const dialogRef = this.dialog.open(UserDialogComponent, {
      data: {subject: e.user, mode: e.mode},
      panelClass: 'full-width-dialog',
      disableClose: true
    });
    this.applyStateChangesToUrlQueryParams({[e.mode]: e.user.userId});

    dialogRef.afterClosed().subscribe({
      next: (command) => {
        if(command === 'updated'){
          console.log(command, 'UPD');
          this.updateTriggerSubject.next('updated');
        } else {
        }
        this.applyStateChangesToUrlQueryParams({[e.mode]: null});
      },
      error: (error) => this.error = error
    });
  }

  private applyStateChangesToUrlQueryParams(queryParams: any): void {
    this.router.navigate([], { queryParams: queryParams, queryParamsHandling: 'merge' }).finally();
  }

  ngOnDestroy() {
    this.updateTriggerSubject.complete();
  }
}
