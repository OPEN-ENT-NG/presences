import { HttpError, SessionExpiredError } from '~/services/api';
import { getErrorKind } from './getErrorKind';

describe('getErrorKind', () => {
  it.each([
    [400, 'course', 'courseNotFound'],
    [404, 'course', 'courseNotFound'],
    [400, 'register', 'registerNotFound'],
    [404, 'register', 'registerNotFound'],
  ] as const)(
    'maps a %i to the %s not found screen',
    (status, notFound, kind) => {
      expect(getErrorKind(new HttpError(status, ''), notFound)).toBe(kind);
    },
  );

  it.each([401, 403])('maps a %i to the forbidden screen', (status) => {
    expect(getErrorKind(new HttpError(status, ''), 'course')).toBe('forbidden');
  });

  it('maps an expired session to the session expired screen, not to a 401', () => {
    expect(getErrorKind(new SessionExpiredError(), 'course')).toBe(
      'sessionExpired',
    );
  });

  it('maps a 408 (no answer from the network) to the offline screen', () => {
    expect(getErrorKind(new HttpError(408, ''), 'course')).toBe('offline');
  });

  it.each([500, 502, 503, 504])('maps a %i to the server screen', (status) => {
    expect(getErrorKind(new HttpError(status, ''), 'course')).toBe('server');
  });

  it('maps an unexpected error to the server screen', () => {
    expect(getErrorKind(new TypeError('boom'), 'course')).toBe('server');
    expect(getErrorKind(undefined, 'register')).toBe('server');
  });
});
