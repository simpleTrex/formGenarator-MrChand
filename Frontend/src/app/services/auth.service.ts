import { Injectable } from '@angular/core';
import { JwtHelperService } from "@auth0/angular-jwt";
import { map } from 'rxjs/operators';
import { environment } from 'src/environments/environment';
import { BaseService } from './base.service';
const helper = new JwtHelperService();

@Injectable()
export class AuthService {
  currentUser: User | null;

  constructor(
    private baseService: BaseService) {
    let user = localStorage.getItem('user');
    this.currentUser = new User();
    if (user) {
      this.currentUser = JSON.parse(user);
    }
  }

  login(username: string, password: string) {
    return this.baseService.post(`${environment.api}/auth/login`, true, { username, password })
      .pipe(map(response => {
        console.log(response);
        if (response && response.token) {
          //localStorage.setItem('token', response.token);
          this.setCookie('token', response.token, 1);
          //document.cookie = 'token_=' + response.token + ';expire=' + new Date() + '; HttpOnly=true;';
          response.token = null;
          localStorage.setItem('user', JSON.stringify(response));
          return true;
        }
        else return false;
      }));
  }

  logout() {
    //localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.currentUser = null;
    this.deleteCookie('token');
  }

  public isLoggedIn() {
    //let token = localStorage.getItem('token');
    let token = this.getCookie('token');
    if (token && this.currentUser && token) {
      return !helper.isTokenExpired(token);
    }
    return false;
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

export class User {
  id?: number;
  username?: string;
  email?: string;
  roles?: string;
  //token?: string;
}

export const _authService = [
  { provide: AuthService, useClass: AuthService, multi: true },
];
