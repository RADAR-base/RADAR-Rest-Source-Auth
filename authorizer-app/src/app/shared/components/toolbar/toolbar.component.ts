import {Component, EventEmitter, Output} from '@angular/core';
import {AuthService} from "../../../auth/services/auth.service";
import {switchAll, switchMap} from "rxjs";
import {Router} from "@angular/router";
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'app-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss']
})
export class ToolbarComponent {

  @Output()
  languageSwitched: EventEmitter<any> = new EventEmitter();

  languages = this.translate.getLangs();
  // constructor(private router: Router, private translate: TranslateService) {
  //   translate.setDefaultLang('en');
  // }

  constructor(public translate: TranslateService, public authService: AuthService) {
    // translate.addLangs(['en', 'nl']);
    // translate.setDefaultLang('en');
    //
    // const browserLang = translate.getBrowserLang();
    // translate.use(browserLang.match(/en|nl/) ? browserLang : 'en');
  }

  // constructor(public authService: AuthService) {}

  logout() {
    this.authService.logout();
    this.authService.clearReturnUrl();
  }

  switchLanguage(language: string) {
    this.languageSwitched.emit(language)
    // this.translate.use(language)
  }
}
