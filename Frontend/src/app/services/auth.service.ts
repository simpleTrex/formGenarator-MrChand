import { Injectable } from '@angular/core';
import { JwtHelperService } from "@auth0/angular-jwt";
import { map } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { BaseService } from './base.service';
const helper = new JwtHelperService();

export type PrincipalType = 'OWNER' | 'DOMAIN_USER';

export interface AuthContext {
  userId: string;
  username?: string;
  email?: string;
  domainId?: string | null;
  principalType: PrincipalType;
  token?: string;
}

@Injectable()
export class AuthService {
  currentUser: AuthContext | null;

  constructor(
    private baseService: BaseService) {
    const stored = localStorage.getItem('authContext');
    this.currentUser = stored ? JSON.parse(stored) : null;
  }

  loginOwner(email: string, password: string) {
    return this.baseService.post(`${environment.adaptiveApi}/auth/owner/login`, false, { email, password })
      .pipe(map(response => this.handleAuthResponse(response)));
  }

  loginDomain(domainSlug: string, username: string, password: string) {
    const slug = domainSlug?.trim().toLowerCase();
    return this.baseService.post(`${environment.adaptiveApi}/domains/${slug}/auth/login`, false, { username, password })
      .pipe(map(response => this.handleAuthResponse(response)));
  }

  /**
   * Legacy login kept for backward compatibility until all components are updated.
   * Defaults to calling domain login with the provided username/password against the slug 'default'.
   */
  login(username: string, password: string) {
    return this.loginDomain('default', username, password);
  }

  logout() {
    localStorage.removeItem('user');
    localStorage.removeItem('authContext');
    sessionStorage.removeItem('token');
    this.currentUser = null;
    this.deleteCookie('token');
  }

  signupOwner(displayName: string, email: string, password: string) {
    return this.baseService.post(`${environment.adaptiveApi}/auth/owner/signup`, false, {
      displayName,
      email,
      password
    });
  }

  signupDomain(domainSlug: string, username: string, email: string, password: string) {
    const slug = domainSlug?.trim().toLowerCase();
    return this.baseService.post(`${environment.adaptiveApi}/domains/${slug}/auth/signup`, false, {
      username,
      email,
      password
    });
  }

  public isLoggedIn() {
    let token = this.getCookie('token');
    if (!token) {
      token = sessionStorage.getItem('token') || '';
    }
    return !!(token && !helper.isTokenExpired(token) && this.currentUser);
  }

  getContext(): AuthContext | null {
    return this.currentUser;
  }

  private handleAuthResponse(response: any) {
    if (response && response.token) {
      this.setCookie('token', response.token, 1);
      sessionStorage.setItem('token', response.token);
    }
    const context: AuthContext = {
      userId: response?.userId,
      username: response?.username,
      email: response?.email,
      domainId: response?.domainId,
      principalType: response?.principalType,
      token: response?.token
    };
    this.currentUser = context;
    localStorage.setItem('authContext', JSON.stringify(context));
    localStorage.setItem('user', JSON.stringify(context));
    return true;
  }

  getCookie(name: string) {
    let ca: Array<string> = document.cookie.split(';');
    let caLen: number = ca.length;
    let cookieName = `${name}=`;
    let c: string;

    for (let i: number = 0; i < caLen; i += 1) {
      c = ca[i].replace(/^\s+/g, '');
      if (c.indexOf(cookieName) == 0) {
        return c.substring(cookieName.length, c.length);
      }
    }
    return '';
  }

  deleteCookie(name: string) {
    this.setCookie(name, '', -1);
  }

  setCookie(name: string, value: string, expireDays: number, path: string = '') {
    let d: Date = new Date();
    d.setTime(d.getTime() + expireDays * 24 * 60 * 60 * 1000);
    let expires: string = `expires=${d.toUTCString()}`;
    let cpath: string = path ? `; path=${path}` : '';
    document.cookie = `${name}=${value}; SameSite=Strict; ${expires}${cpath}`;
  }

}

export const _authService = [
  { provide: AuthService, useClass: AuthService, multi: true },
];
