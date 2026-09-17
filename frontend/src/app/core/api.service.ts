import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AnswerResult, DashboardView, DifficultyTier, HintView, ProblemCategory, ProblemDetail, ProblemSummary,
  ProgressView, QuestionSummary, SeniorityLevel, SessionView, SolveResult,
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  // DSA
  problems(category?: ProblemCategory | '', difficulty?: DifficultyTier | ''): Observable<ProblemSummary[]> {
    let params = new HttpParams();
    if (category) params = params.set('category', category);
    if (difficulty) params = params.set('difficulty', difficulty);
    return this.http.get<ProblemSummary[]>('/api/dsa/problems', { params });
  }
  problem(id: number): Observable<ProblemDetail> { return this.http.get<ProblemDetail>(`/api/dsa/problems/${id}`); }
  attempt(id: number, notes: string): Observable<ProgressView> { return this.http.post<ProgressView>(`/api/dsa/problems/${id}/attempts`, { notes }); }
  hint(id: number, attempt: string): Observable<HintView> { return this.http.post<HintView>(`/api/dsa/problems/${id}/hints`, { attempt }); }
  solve(id: number): Observable<SolveResult> { return this.http.post<SolveResult>(`/api/dsa/problems/${id}/solve`, {}); }
  dashboard(): Observable<DashboardView> { return this.http.get<DashboardView>('/api/dsa/progress'); }

  // System design
  questions(seniority?: SeniorityLevel | ''): Observable<QuestionSummary[]> {
    const params = seniority ? new HttpParams().set('seniority', seniority) : undefined;
    return this.http.get<QuestionSummary[]>('/api/design/questions', { params });
  }
  sessions(): Observable<SessionView[]> { return this.http.get<SessionView[]>('/api/design/sessions'); }
  session(id: number): Observable<SessionView> { return this.http.get<SessionView>(`/api/design/sessions/${id}`); }
  startSession(questionId: number): Observable<SessionView> { return this.http.post<SessionView>('/api/design/sessions', { questionId }); }
  answer(id: number, answer: string): Observable<AnswerResult> { return this.http.post<AnswerResult>(`/api/design/sessions/${id}/answers`, { answer }); }
}
