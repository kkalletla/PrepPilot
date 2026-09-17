/** Pulls the backend's {"error": "..."} message out of an HttpErrorResponse, with a fallback. */
export function apiMessage(err: any, fallback = 'Something went wrong'): string {
  return err?.error?.error ?? (err?.status === 0 ? 'Cannot reach the server' : fallback);
}

export function isPaymentRequired(err: any): boolean {
  return err?.status === 402;
}
