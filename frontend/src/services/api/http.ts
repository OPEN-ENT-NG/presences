import { odeServices } from '@edifice.io/client';

/** Error thrown by the API services, carrying the HTTP status for the error screens. */
export class HttpError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'HttpError';
  }
}

/**
 * The session expired: the ENT redirected the request to the login page, which
 * the browser followed. Not to be mixed with a 401 from the API, which is how
 * the ENT refuses an action the user has no right for.
 */
export class SessionExpiredError extends HttpError {
  constructor() {
    super(401, 'Session expired');
    this.name = 'SessionExpiredError';
  }
}

/**
 * Rejects with an `HttpError` carrying the status of the failed request.
 * `odeServices.http()` rejects with the response body only, and maps a request
 * without response (offline) to a 408. A HTML response to a request that
 * succeeded is the login page: the session expired.
 */
export async function checkHttpError<T>(promise: Promise<T>): Promise<T> {
  const http = odeServices.http();
  try {
    const result = await promise;
    const contentType = http.latestResponse.headers?.get?.('Content-Type') as
      string | undefined;
    if (contentType?.includes('html')) {
      throw new SessionExpiredError();
    }
    return result;
  } catch (error) {
    if (error instanceof HttpError) throw error;
    const { status, statusText } = http.latestResponse;
    throw new HttpError(status, statusText);
  }
}
