import { HttpResponse, http } from 'msw';
import {
  MOCK_DATE,
  MOCK_STRUCTURE_ID,
  mockCourses,
} from '~/mocks/datas/courses';
import { server } from '~/mocks/server';
import { HttpError, SessionExpiredError, baseUrl, courseService } from '.';

describe('courseService.getCourses', () => {
  test('gets the courses of the structure for the day', async () => {
    const courses = await courseService.getCourses({
      structureId: MOCK_STRUCTURE_ID,
      date: MOCK_DATE,
    });

    expect(courses).toStrictEqual(mockCourses);
  });

  test('sends the day as start and end', async () => {
    let searchParams: URLSearchParams | undefined;
    server.use(
      http.get(`${baseUrl}/courses`, ({ request }) => {
        searchParams = new URL(request.url).searchParams;
        return HttpResponse.json([]);
      }),
    );

    await courseService.getCourses({
      structureId: MOCK_STRUCTURE_ID,
      date: MOCK_DATE,
    });

    expect(searchParams?.get('structure')).toBe(MOCK_STRUCTURE_ID);
    expect(searchParams?.get('start')).toBe(MOCK_DATE);
    expect(searchParams?.get('end')).toBe(MOCK_DATE);
  });

  test('returns no course on another day', async () => {
    const courses = await courseService.getCourses({
      structureId: MOCK_STRUCTURE_ID,
      date: '2026-10-14',
    });

    expect(courses).toStrictEqual([]);
  });

  test('rejects with the HTTP status when the API refuses', async () => {
    server.use(
      http.get(
        `${baseUrl}/courses`,
        () => new HttpResponse(null, { status: 403 }),
      ),
    );

    await expect(
      courseService.getCourses({
        structureId: MOCK_STRUCTURE_ID,
        date: MOCK_DATE,
      }),
    ).rejects.toMatchObject({ name: 'HttpError', status: 403 });
  });

  test('rejects with a 408 when the network is down', async () => {
    server.use(http.get(`${baseUrl}/courses`, () => HttpResponse.error()));

    await expect(
      courseService.getCourses({
        structureId: MOCK_STRUCTURE_ID,
        date: MOCK_DATE,
      }),
    ).rejects.toMatchObject({ name: 'HttpError', status: 408 });
  });

  test('rejects with a session expired error when the API answers with the login page', async () => {
    server.use(
      http.get(`${baseUrl}/courses`, () =>
        HttpResponse.html('<html>login</html>'),
      ),
    );

    const error = await courseService
      .getCourses({ structureId: MOCK_STRUCTURE_ID, date: MOCK_DATE })
      .catch((error: unknown) => error);

    expect(error).toBeInstanceOf(SessionExpiredError);
  });

  test('rejects with a plain 401 when the ENT refuses with its HTML error page', async () => {
    // What the recette answers for a structure the user does not belong to
    server.use(
      http.get(`${baseUrl}/courses`, () =>
        HttpResponse.html('<html>Erreur</html>', { status: 401 }),
      ),
    );

    const error = await courseService
      .getCourses({ structureId: MOCK_STRUCTURE_ID, date: MOCK_DATE })
      .catch((error: unknown) => error);

    expect(error).toBeInstanceOf(HttpError);
    expect(error).not.toBeInstanceOf(SessionExpiredError);
    expect(error).toMatchObject({ status: 401 });
  });
});
