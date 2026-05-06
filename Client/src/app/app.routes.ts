import { Routes } from '@angular/router';

import { LandingPageComponent } from './pages/landing-page/landing-page';

export const routes: Routes = [
  {
    path: '',
    component: LandingPageComponent,
    title: 'Honest Car',
  },
  {
    path: 'about',
    loadComponent: () =>
      import('./pages/about-page/about-page').then((m) => m.AboutPageComponent),
    title: 'About Us · Honest Car',
  },
  {
    path: 'sign-in',
    loadComponent: () =>
      import('./pages/sign-in-page/sign-in-page').then(
        (m) => m.SignInPageComponent,
      ),
    title: 'Sign In · Honest Car',
  },
  {
    path: 'sign-up',
    loadComponent: () =>
      import('./pages/sign-up-page/sign-up-page').then(
        (m) => m.SignUpPageComponent,
      ),
    title: 'Sign Up · Honest Car',
  },
  {
    path: '**',
    redirectTo: '',
  },
];
