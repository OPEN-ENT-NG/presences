import { isRouteErrorResponse } from 'react-router-dom';
import { HttpError } from '~/services/api';

export type ErrorKind =
  | 'sessionExpired'
  | 'forbidden'
  | 'courseNotFound'
  | 'registerNotFound'
  | 'server'
  | 'offline';

/** What a "not found" means on the page that failed. */
export type NotFoundTarget = 'course' | 'register';

const getStatus = (error: unknown): number | undefined => {
  if (error instanceof HttpError) return error.status;
  if (isRouteErrorResponse(error)) return error.status;
  return undefined;
};

/**
 * Picks the error screen from the status of the failed request. Anything
 * unexpected (e.g. an exception while rendering) is a server error.
 * A 401 is a session that expired (the API answered with the login page).
 * A 400 is a course context the AngularJS page did not give properly: the
 * course cannot be identified, like a 404.
 */
export const getErrorKind = (
  error: unknown,
  notFound: NotFoundTarget,
): ErrorKind => {
  switch (getStatus(error)) {
    case 400:
    case 404:
      return notFound === 'course' ? 'courseNotFound' : 'registerNotFound';
    case 401:
      return 'sessionExpired';
    case 403:
      return 'forbidden';
    case 408:
      return 'offline';
    default:
      return 'server';
  }
};
