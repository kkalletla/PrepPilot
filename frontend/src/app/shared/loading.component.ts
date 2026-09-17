import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loading',
  template: `<p class="loading" role="status"><span class="spinner"></span> {{ label() }}</p>`,
})
export class LoadingComponent {
  label = input('Loading…');
}
