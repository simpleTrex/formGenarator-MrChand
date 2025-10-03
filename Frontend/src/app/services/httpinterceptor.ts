import { Injectable } from '@angular/core';
import { HttpEvent, HttpInterceptor, HttpHandler, HttpRequest, HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService, _authService } from './auth.service';

@Injectable()
export class HttpRequestInterceptor implements HttpInterceptor {
    _authService: AuthService;
    constructor(private authService: AuthService) {
        this._authService = authService;
    }
    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        let _token: any = this._authService.getCookie('token');
        //let _token: any = localStorage.getItem('token');
        console.log(_token);
        if (_token) {
            req = req.clone({
                setHeaders: { token: _token },
                //withCredentials: true,
                /*setParams: {
                    token: _token,
                }
                */
            });
        }

        return next.handle(req);
    }
}
export const httpInterceptorProviders = [
    { provide: HTTP_INTERCEPTORS, useClass: HttpRequestInterceptor, multi: true },
];
