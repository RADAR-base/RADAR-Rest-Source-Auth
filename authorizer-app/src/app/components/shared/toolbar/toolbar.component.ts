import { Component, Input } from '@angular/core';

import { AuthService } from '../../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.css']
})
export class ToolbarComponent {
  @Input() showMenu: boolean;

  constructor(private authService: AuthService, private router: Router) {}

  logoutHandler() {
    this.authService.clearAuth();
    this.router.navigate(['/login']);
  }
}
