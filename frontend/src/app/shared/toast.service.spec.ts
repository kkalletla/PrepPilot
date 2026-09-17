import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ToastService } from './toast.service';

describe('ToastService', () => {
  it('queues a toast and auto-dismisses it', fakeAsync(() => {
    const svc = TestBed.inject(ToastService);
    svc.success('Saved');
    expect(svc.toasts().length).toBe(1);
    expect(svc.toasts()[0].kind).toBe('success');
    tick(3500);
    expect(svc.toasts().length).toBe(0);
  }));

  it('dismisses on demand', () => {
    const svc = TestBed.inject(ToastService);
    svc.error('Boom');
    svc.dismiss(svc.toasts()[0].id);
    expect(svc.toasts().length).toBe(0);
  });
});
