import {Component, OnInit} from '@angular/core';
import {NavigationCancel, NavigationEnd, NavigationError, NavigationStart, Router} from "@angular/router";
import {TranslateService} from "@ngx-translate/core";
import {StorageItem} from "./shared/enums/storage-item";
import {environment} from "../environments/environment";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  loading = true;
  mainLoading = true;
  // translation$ = this.translate.onLangChange.pipe(switchMap(({ lang }) => this.translate.getTranslation(lang)));




  // translation$ = this.translate.onLangChange.pipe(switchMap(({ lang }) => this.translate.getTranslation(lang)));

  // constructor(private router: Router, private translate: TranslateService) {
  //   translate.setDefaultLang('en');
  // }

  constructor(private router: Router, private translate: TranslateService) {

    translate.addLangs(environment.languages);
    const browserLang = translate.getBrowserLang();
    console.log(browserLang);
    let language = environment.languages.includes(browserLang) ? browserLang : environment.languages[0];
    // let language = browserLang.match(/en|nl/) ? browserLang : environment.languages[0]
    const storedLocale = localStorage.getItem(StorageItem.LOCALE);
    if (storedLocale) {
      // language = storedLocale.match(/en|nl/) ? storedLocale : environment.languages[0];
      language = environment.languages.includes(browserLang) ? browserLang : environment.languages[0];
    } else {
      localStorage.setItem(StorageItem.LOCALE, language);
      translate.setDefaultLang(language);
    }
    translate.use(language).subscribe({
      next: () => this.mainLoading = false
    });

  }

  // constructor(private router: Router, public translate: TranslateService) {
  //   translate.addLangs(['en', 'nl']);
  //   translate.setDefaultLang('en');
  //
  //   const browserLang = translate.getBrowserLang();
  //   translate.use(browserLang.match(/en|nl/) ? browserLang : 'en');
  // }


  ngOnInit(): void {
    const subscription = this.router.events.subscribe(event => {
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
