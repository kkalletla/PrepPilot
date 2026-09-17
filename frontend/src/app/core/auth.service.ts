import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { TokenResponse } from '../models';

const TOKEN_KEY = 'preppilot.token';
const EMAIL_KEY = 'preppilot.email';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenSig = signal<string | null>(safeGet(TOKEN_KEY));
  readonly email = signal<string | null>(safeGet(EMAIL_KEY));
  readonly isLoggedIn = computed(() => this.tokenSig() !== null);

  constructor(private http: HttpClient) {}

  get token(): string | null { return this.tokenSig(); }

  register(email: string, password: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/register', { email, password }).pipe(tap(r => this.store(r)));
  }

  login(email: string, password: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/login', { email, password }).pipe(tap(r => this.store(r)));
  }

  logout(): void {
    this.tokenSig.set(null);
    this.email.set(null);
    safeRemove(TOKEN_KEY);
    safeRemove(EMAIL_KEY);
  }

  private store(r: TokenResponse): void {
    this.tokenSig.set(r.token);
    this.email.set(r.email);
    safeSet(TOKEN_KEY, r.token);
    safeSet(EMAIL_KEY, r.email);
  }
}

function safeGet(k: string): string | null { try { return localStorage.getItem(k); } catch { return null; } }
function safeSet(k: string, v: string): void { try { localStorage.setItem(k, v); } catch { /* ignore */ } }
function safeRemove(k: string): void { try { localStorage.removeItem(k); } catch { /* ignore */ } }
