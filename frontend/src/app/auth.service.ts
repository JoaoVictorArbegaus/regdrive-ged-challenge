import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, tap, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';

interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
}

export interface AuthenticatedUser {
  username: string;
  role: 'ADMIN' | 'USER' | 'VIEWER';
  tenantId: string;
  expiresAt: number;
}

const TOKEN_KEY = 'regdrive_access_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly currentUser = signal<AuthenticatedUser | null>(this.readStoredUser());

  readonly user = this.currentUser.asReadonly();
  readonly authenticated = computed(() => this.currentUser() !== null);

  login(username: string, password: string) {
    return this.http.post<LoginResponse>('/api/auth/login', { username, password }).pipe(
      tap((response) => {
        sessionStorage.setItem(TOKEN_KEY, response.accessToken);
        this.currentUser.set(this.decodeUser(response.accessToken));
      })
    );
  }

  token(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    this.currentUser.set(null);
  }

  private readStoredUser(): AuthenticatedUser | null {
    const token = sessionStorage.getItem(TOKEN_KEY);
    if (!token) {
      return null;
    }
    try {
      const user = this.decodeUser(token);
      if (user.expiresAt * 1000 <= Date.now()) {
        sessionStorage.removeItem(TOKEN_KEY);
        return null;
      }
      return user;
    } catch {
      sessionStorage.removeItem(TOKEN_KEY);
      return null;
    }
  }

  private decodeUser(token: string): AuthenticatedUser {
    const payload = token.split('.')[1];
    if (!payload) {
      throw new Error('Token invalido.');
    }
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padding = '='.repeat((4 - normalized.length % 4) % 4);
    const claims = JSON.parse(atob(normalized + padding));
    return {
      username: claims.sub,
      role: claims.role,
      tenantId: claims.tenant_id,
      expiresAt: claims.exp
    };
  }
}

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();
  let authenticatedRequest = request;
  if (token) {
    authenticatedRequest = request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(authenticatedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !request.url.endsWith('/api/auth/login')) {
        auth.logout();
        router.navigateByUrl('/login');
      }
      return throwError(() => error);
    })
  );
};

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.authenticated()) {
    return true;
  }
  return router.createUrlTree(['/login']);
};
