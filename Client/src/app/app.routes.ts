import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { LandingPageComponent } from './pages/landing-page/landing-page';

export const routes: Routes = [
  {
    path: '',
    component: LandingPageComponent,
    title: 'Honest Car',
  },
  {
    path: 'home',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/home-page/home-page').then((m) => m.HomePageComponent),
    title: 'Home · Honest Car',
  },
  {
    path: 'about',
    loadComponent: () => import('./pages/about-page/about-page').then((m) => m.AboutPageComponent),
    title: 'About Us · Honest Car',
  },
  {
    path: 'sign-in',
    component: LandingPageComponent,
    title: 'Sign In · Honest Car',
    data: { authMode: 'sign-in' },
  },
  {
    path: 'sign-up',
    component: LandingPageComponent,
    title: 'Sign Up · Honest Car',
    data: { authMode: 'sign-up' },
  },
  {
    path: '**',
    redirectTo: '',
  },
];
