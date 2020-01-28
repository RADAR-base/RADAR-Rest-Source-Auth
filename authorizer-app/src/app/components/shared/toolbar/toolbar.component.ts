import { AuthService } from '../../../services/auth.service';
import { Component, Input } from '@angular/core';
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
    this.authService.clearAuthData();
    this.router.navigate(['/login']);
  }
}
