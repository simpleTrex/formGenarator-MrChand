import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import type { PrincipalType } from '../services/auth.service';

@Injectable()
export class AuthGuard implements CanActivate {
    constructor(private router: Router, private authService: AuthService) { }

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        const ctx = this.authService.getContext();

        const allowed: PrincipalType[] = (route.data?.['principalTypes'] as PrincipalType[] | undefined) ?? ['OWNER'];

        if (this.authService.isLoggedIn() && ctx?.principalType && allowed.includes(ctx.principalType)) {
            return true;
        }

        const loginRoute = allowed.length === 1 && allowed[0] === 'DOMAIN_USER'
            ? '/domain-login'
            : '/owner-login';

        this.router.navigate([loginRoute], { queryParams: { returnUrl: state.url } });
        return false;
    }
}
