import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable()
export class AuthGuard implements CanActivate {
    constructor(private router: Router, private authService: AuthService) {}

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const ctx = this.authService.getContext();
        if (this.authService.isLoggedIn() && ctx?.principalType === 'OWNER') {
            return true;
        }
        this.router.navigate(['/owner-login'], { queryParams: { returnUrl: state.url } });
        return false;
    }
}
