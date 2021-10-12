import {Component, OnInit} from '@angular/core';
import {NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router} from "@angular/router";
import {TranslateService} from "@ngx-translate/core";

import {StorageItem} from "@app/shared/enums/storage-item";
import {environment} from "@environments/environment";

import packageJson from '../../package.json';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  loading = true;
  mainLoading = true;
  version = packageJson.version;

  constructor(
    private router: Router,
    private translate: TranslateService
  ) {
    translate.addLangs(environment.languages);
    const browserLang = translate.getBrowserLang();
    let language = environment.languages.includes(browserLang) ? browserLang : environment.languages[0];
    const storedLocale = localStorage.getItem(StorageItem.LOCALE);
    if (storedLocale) {
      language = storedLocale;
    } else {
      localStorage.setItem(StorageItem.LOCALE, language);
      translate.setDefaultLang(language);
    }
    translate.use(language).subscribe({
      next: () => this.mainLoading = false
    });
  }

  ngOnInit(): void {
    const subscription = this.router.events.subscribe({
      next: event => {
        switch (true) {
          case event instanceof NavigationStart: {
            this.loading = true;
            break;
          }
          case event instanceof NavigationEnd: {
            this.loading = false;
            subscription.unsubscribe();
            break;
          }
          case event instanceof NavigationCancel:
          case event instanceof NavigationError: {
            this.loading = false;
            break;
          }
          default: {
            break;
          }
        }
      }
    });
  }

  switchLanguage(language: string) {
    this.mainLoading = true;
    localStorage.setItem(StorageItem.LOCALE, language);
    this.translate.use(language).subscribe({
      next: () => this.mainLoading = false
    });
  }
}
