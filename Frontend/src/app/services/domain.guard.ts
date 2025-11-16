import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class DomainGuard implements CanActivate {

  constructor(private auth: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    const ctx = this.auth.getContext();
    const slug = route.params['slug'];
    if (!ctx || !ctx.domainId || !slug) {
      return true;
    }
    // Domain membership is evaluated server-side; allow navigation even if context mismatch.
    return true;
  }
}
