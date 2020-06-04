import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'simple-login',
  templateUrl: './simple-login.component.html',
  styleUrls: ['./login-page.component.css']
})
export class SimpleLoginComponent {
  buttonDisabled = false;
  buttonLabel = 'LOG IN';

  @Output()
  authenticate: EventEmitter<string> = new EventEmitter<string>();

  constructor() {}

  loginHandler() {
    this.buttonDisabled = true;
    this.buttonLabel = 'LOGGING IN..';
    this.authenticate.emit();
  }
}
