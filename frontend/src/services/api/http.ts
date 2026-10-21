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
 * Rejects with an `HttpError` carrying the status of the failed request.
 * `odeServices.http()` rejects with the response body only, and maps a request
 * without response (offline) to a 408. A HTML response means the session
 * expired (redirection to the login page).
 */
export async function checkHttpError<T>(promise: Promise<T>): Promise<T> {
  const http = odeServices.http();
  try {
    const result = await promise;
    const contentType = http.latestResponse.headers?.get?.('Content-Type') as
      string | undefined;
    if (contentType?.includes('html')) {
      throw new HttpError(401, 'Unauthorized');
    }
    return result;
  } catch (error) {
    if (error instanceof HttpError) throw error;
    const { status, statusText } = http.latestResponse;
    throw new HttpError(status, statusText);
  }
}
