import { Component, OnInit } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { NavbarComponent } from './components/shared/navbar/navbar.component';
import { FooterComponent } from './components/shared/footer/footer.component';
import { ToastComponent } from './components/shared/toast/toast.component';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, FooterComponent, ToastComponent, CommonModule],
  template: `
    <!-- Navbar: always shown, admin/seller pages push content via dashboard-layout -->
    <app-navbar></app-navbar>
    <main class="page-content">
      <router-outlet></router-outlet>
    </main>
    <app-footer *ngIf="showFooter"></app-footer>
    <app-toast></app-toast>
  `
})
export class AppComponent implements OnInit {
  showFooter = true;

  constructor(private router: Router) {
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe((e: any) => {
      this.showFooter = !e.urlAfterRedirects.startsWith('/admin') && !e.urlAfterRedirects.startsWith('/seller');
    });
  }

  ngOnInit(): void {}
}
