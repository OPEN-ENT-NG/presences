import { HttpResponse, http } from 'msw';
import {
  MOCK_DATE,
  MOCK_REGISTER_AFTERNOON_ID,
  MOCK_REGISTER_EMPTY_CLASS_ID,
  MOCK_REGISTER_MORNING_ID,
  MOCK_STRUCTURE_ID,
  mockCourseMorning,
  mockCourses,
} from '~/mocks/datas/courses';
import { server } from '~/mocks/server';
import { CourseContext } from '~/models/course-context';
import { baseUrl } from '~/services/api';
import { openRegister } from './register-opening';

const contextOf = (courseId: string): CourseContext => ({
  courseId,
  structureId: MOCK_STRUCTURE_ID,
  date: MOCK_DATE,
  startTime: '08:30',
  endTime: '11:45',
});

/** Methods and paths of the requests sent to the Présences API. */
const recordRequests = () => {
  const requests: string[] = [];
  server.events.on('request:start', ({ request }) => {
    const { pathname } = new URL(request.url);
    if (pathname.startsWith(baseUrl)) {
      requests.push(`${request.method} ${pathname}`);
    }
  });
  return requests;
};

afterEach(() => server.events.removeAllListeners());

describe('openRegister', () => {
  test('creates the register when the course has none yet', async () => {
    const requests = recordRequests();

    const registerId = await openRegister(contextOf('course-morning'));

    expect(registerId).toBe(MOCK_REGISTER_MORNING_ID);
    expect(requests).toStrictEqual([
      `GET ${baseUrl}/courses`,
      `POST ${baseUrl}/registers`,
    ]);
  });

  test('finds the existing register without writing anything', async () => {
    const requests = recordRequests();

    const registerId = await openRegister(contextOf('course-afternoon'));

    expect(registerId).toBe(MOCK_REGISTER_AFTERNOON_ID);
    expect(requests).toStrictEqual([`GET ${baseUrl}/courses`]);
  });

  test('picks the course by its identifier among courses of the same time', async () => {
    // Two courses start at 08:30 the same day: only the identifier tells them apart
    const registerId = await openRegister(contextOf('course-empty-class'));

    expect(registerId).toBe(MOCK_REGISTER_EMPTY_CLASS_ID);
  });

  test('does not create a duplicate when the register is opened twice', async () => {
    const first = await openRegister(contextOf('course-morning'));
    const second = await openRegister(contextOf('course-morning'));

    expect(second).toBe(first);
  });

  test('rejects with a 404 when the course is not in the day', async () => {
    await expect(
      openRegister(contextOf('unknown-course')),
    ).rejects.toMatchObject({ name: 'HttpError', status: 404 });
  });

  test('rejects with a 403 when the course does not allow a register', async () => {
    server.use(
      http.get(`${baseUrl}/courses`, () =>
        HttpResponse.json(
          mockCourses.map((course) =>
            course.id === mockCourseMorning.id
              ? { ...course, allowRegister: false }
              : course,
          ),
        ),
      ),
    );
    const requests = recordRequests();

    await expect(
      openRegister(contextOf('course-morning')),
    ).rejects.toMatchObject({ name: 'HttpError', status: 403 });
    expect(requests).not.toContain(`POST ${baseUrl}/registers`);
  });

  test('rejects with the status of the API when it refuses the courses', async () => {
    server.use(
      http.get(
        `${baseUrl}/courses`,
        () => new HttpResponse(null, { status: 403 }),
      ),
    );

    await expect(
      openRegister(contextOf('course-morning')),
    ).rejects.toMatchObject({ name: 'HttpError', status: 403 });
  });

  test('rejects with the status of the API when the creation fails', async () => {
    server.use(
      http.post(
        `${baseUrl}/registers`,
        () => new HttpResponse(null, { status: 500 }),
      ),
    );

    await expect(
      openRegister(contextOf('course-morning')),
    ).rejects.toMatchObject({ name: 'HttpError', status: 500 });
  });
});
