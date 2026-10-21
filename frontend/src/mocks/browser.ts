import { delay, http } from 'msw';
import { setupWorker } from 'msw/browser';
import { baseUrl } from '~/services/api';
import { handlers } from './handlers';
import { defaultHandlers } from './handlers/default-handlers';
import { uiOverridesHandlers } from './handlers/ui-overrides-handlers';

/** Delays the Présences API without answering, to see the loading states. */
const delayHandler = http.all(`${baseUrl}/*`, async () => {
  await delay(500);
});

/**
 * Mock mode (`pnpm dev:mock`): works offline, the session routes are mocked
 * too. Unhandled requests go through (i18n served by the Vite dev server).
 */
export const worker = setupWorker(
  delayHandler,
  ...handlers,
  ...uiOverridesHandlers,
  ...defaultHandlers,
);
