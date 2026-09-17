// Mirrors the backend DTOs (see backend/src/main/java/com/preppilot/**/…Dtos.java)

export type DifficultyTier = 'EASY' | 'MEDIUM' | 'HARD' | 'FAANG_BAR';
export type ProblemCategory = 'ARRAYS' | 'LINKED_LISTS' | 'STACKS_QUEUES' | 'TREES' | 'GRAPHS' | 'DP';
export type ProgressStatus = 'IN_PROGRESS' | 'SOLVED';
export type DesignStage = 'REQUIREMENTS' | 'COMPONENTS' | 'DATA_MODEL' | 'SCALING' | 'COMPLETE';
export type RubricDimension = 'SCALABILITY' | 'DATA_MODELING' | 'TRADE_OFF_REASONING' | 'COMMUNICATION_CLARITY';
export type SeniorityLevel = 'MID' | 'SENIOR' | 'STAFF';

export interface TokenResponse { token: string; userId: number; email: string; }

export interface ProblemSummary {
  id: number; slug: string; title: string; category: ProblemCategory; difficulty: DifficultyTier;
  status: ProgressStatus | null; hintsUsed: number;
}
export interface ProgressView {
  problemId: number; difficulty: DifficultyTier; attempts: number; hintsUsed: number;
  status: ProgressStatus; lastAttemptAt: string;
}
export interface ProblemDetail {
  id: number; slug: string; title: string; category: ProblemCategory; difficulty: DifficultyTier;
  statement: string; progress: ProgressView | null;
}
export interface HintResponse { hint: string; depth: number; maxDepth: number; exhausted: boolean; followUpPrompt: string | null; }
export interface HintView { hint: HintResponse; progress: ProgressView; }
export interface SolveResult { progress: ProgressView; recommendedTier: DifficultyTier; escalated: boolean; streakDays: number; }
export interface DashboardView {
  streakDays: number; solvedCount: number; inProgressCount: number;
  recommendedTiers: Record<ProblemCategory, DifficultyTier>; recent: ProgressView[];
}

export interface QuestionSummary { id: number; slug: string; title: string; category: string; seniority: SeniorityLevel; }
export interface TranscriptTurn { role: 'COACH' | 'CANDIDATE'; stage: DesignStage; content: string; }
export interface RubricScore { dimensions: Record<RubricDimension, number>; overall: number; narrative: string; }
export interface DesignFeedback {
  stage: DesignStage; stageScore: number; strengths: string[]; gaps: string[]; narrative: string;
  dimensionScores: Partial<Record<RubricDimension, number>>; followUpPrompt: string;
}
export interface SessionView {
  id: number; questionId: number; questionTitle: string; stage: DesignStage; transcript: TranscriptTurn[];
  rubric?: RubricScore; startedAt: string; completedAt?: string;
}
export interface AnswerResult { feedback: DesignFeedback; session: SessionView; }

export const STAGE_LABELS: Record<DesignStage, string> = {
  REQUIREMENTS: 'Requirements', COMPONENTS: 'High-level components', DATA_MODEL: 'Data model',
  SCALING: 'Scaling', COMPLETE: 'Complete',
};
export const TIERS: DifficultyTier[] = ['EASY', 'MEDIUM', 'HARD', 'FAANG_BAR'];
export const CATEGORIES: ProblemCategory[] = ['ARRAYS', 'LINKED_LISTS', 'STACKS_QUEUES', 'TREES', 'GRAPHS', 'DP'];

export type SubscriptionTier = 'FREE' | 'PAID';
export type SubscriptionStatus = 'NONE' | 'ACTIVE' | 'PAST_DUE' | 'CANCELED';
export interface BillingStatus {
  tier: SubscriptionTier; status: SubscriptionStatus; unlimited: boolean;
  dsaUsedToday: number; dsaDailyLimit: number | null;
  designUsedThisWeek: number; designWeeklyLimit: number | null;
  hasStripeCustomer: boolean; billingConfigured: boolean;
}
export interface RedirectResponse { url: string; }
