import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginComponent } from './pages/login/login.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { DsaListComponent } from './pages/dsa-list/dsa-list.component';
import { DsaProblemComponent } from './pages/dsa-problem/dsa-problem.component';
import { DesignListComponent } from './pages/design-list/design-list.component';
import { DesignSessionComponent } from './pages/design-session/design-session.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'dsa', component: DsaListComponent, canActivate: [authGuard] },
  { path: 'dsa/:id', component: DsaProblemComponent, canActivate: [authGuard] },
  { path: 'design', component: DesignListComponent, canActivate: [authGuard] },
  { path: 'design/sessions/:id', component: DesignSessionComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' },
];
