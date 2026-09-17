import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts logged out', () => {
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.token).toBeNull();
  });

  it('stores the token after login and clears it on logout', () => {
    service.login('kay@example.com', 'hunter22!').subscribe();
    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ token: 'jwt', userId: 1, email: 'kay@example.com' });

    expect(service.isLoggedIn()).toBeTrue();
    expect(service.token).toBe('jwt');
    expect(localStorage.getItem('preppilot.token')).toBe('jwt');

    service.logout();
    expect(service.isLoggedIn()).toBeFalse();
    expect(localStorage.getItem('preppilot.token')).toBeNull();
  });
});
