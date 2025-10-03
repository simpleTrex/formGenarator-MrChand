import { Component, OnInit } from '@angular/core';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'home-page',
  templateUrl: './home-page.component.html',
  styleUrls: ['./home-page.component.css']
})

export class HomePageComponent implements OnInit {
  _authService: AuthService;
  constructor(private authService: AuthService) {
    this._authService = authService;
  }
  ngOnInit(): void {
  }

}
