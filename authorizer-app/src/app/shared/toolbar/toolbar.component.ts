import { Component, Input } from '@angular/core';

// import { AuthService } from '../../../services/auth.service';
import { Router } from '@angular/router';
import {AuthService} from '../../auth/services/auth.service';

@Component({
  selector: 'toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss']
})
export class ToolbarComponent {
  @Input() showMenu: boolean | undefined;

  constructor(private authService: AuthService, private router: Router) {}

  logoutHandler() {
    this.authService.clearAuth();
    this.router.navigate(['/login']);
  }
}
