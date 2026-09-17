import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Shown when the API answers 402: the free-tier limit was hit. */
@Component({
  selector: 'app-upgrade-prompt',
  imports: [RouterLink],
  template: `
    <section class="card upgrade">
      <h2>Free-tier limit reached</h2>
      <p>{{ message() }}</p>
      <a class="button primary" routerLink="/billing">Upgrade to Pro</a>
    </section>
  `,
})
export class UpgradePromptComponent {
  message = input.required<string>();
}
