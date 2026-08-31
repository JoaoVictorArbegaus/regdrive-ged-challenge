import { Routes } from '@angular/router';
import { authGuard } from './auth.service';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./login').then((component) => component.Login)
  },
  {
    path: 'documents',
    canActivate: [authGuard],
    loadComponent: () => import('./documents').then((component) => component.Documents)
  },
  {
    path: 'documents/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./document-detail').then((component) => component.DocumentDetail)
  },
  { path: '', pathMatch: 'full', redirectTo: 'documents' },
  { path: '**', redirectTo: 'documents' }
];
