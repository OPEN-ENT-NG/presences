import { courseHandlers } from './handlers/course-handlers';
import { registerHandlers } from './handlers/register-handlers';

/**
 * MSW Handlers
 * Mock HTTP methods of the Présences API, shared by the tests and the mock mode
 */
export const handlers = [...courseHandlers, ...registerHandlers];
