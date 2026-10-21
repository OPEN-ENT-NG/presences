import { setupServer } from 'msw/node';
import { handlers } from './handlers';
import { defaultHandlers } from './handlers/default-handlers';

export const server = setupServer(...handlers, ...defaultHandlers);
