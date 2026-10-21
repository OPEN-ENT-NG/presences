import { HttpResponse, http } from 'msw';
import {
  MOCK_REGISTER_AFTERNOON_ID,
  MOCK_REGISTER_MORNING_ID,
  mockCourseAfternoon,
  mockCourseMorning,
} from '~/mocks/datas/courses';
import {
  mockRegisterAfternoon,
  mockRegisterMorning,
  toCreatedRegister,
} from '~/mocks/datas/registers';
import { server } from '~/mocks/server';
import { CreateRegisterPayload } from '~/models/register';
import { baseUrl, registerService } from '.';
import { toCreateRegisterPayload } from './registerService';

describe('toCreateRegisterPayload', () => {
  test('builds the creation body from the course', () => {
    expect(toCreateRegisterPayload(mockCourseMorning)).toStrictEqual({
      course_id: 'course-morning',
      structure_id: 'structure-1',
      start_date: '2026-10-13 08:30:00',
      end_date: '2026-10-13 11:45:00',
      subject_id: 'subject-1',
      groups: [],
      classes: ['CE1 A'],
      teacherIds: ['teacher-1'],
      split_slot: false,
    });
  });
});

describe('registerService.createRegister', () => {
  test('posts the creation body built from the course', async () => {
    let body: CreateRegisterPayload | undefined;
    server.use(
      http.post(`${baseUrl}/registers`, async ({ request }) => {
        body = (await request.json()) as CreateRegisterPayload;
        return HttpResponse.json(
          toCreatedRegister(MOCK_REGISTER_MORNING_ID, mockRegisterMorning),
          {
            status: 201,
          },
        );
      }),
    );

    await registerService.createRegister(mockCourseMorning);

    expect(body).toStrictEqual(toCreateRegisterPayload(mockCourseMorning));
  });

  test('returns the created register', async () => {
    const register = await registerService.createRegister(mockCourseMorning);

    expect(register).toStrictEqual(
      toCreatedRegister(MOCK_REGISTER_MORNING_ID, mockRegisterMorning),
    );
  });

  test('returns the existing register when it already exists', async () => {
    const register = await registerService.createRegister(mockCourseAfternoon);

    expect(register.id).toBe(MOCK_REGISTER_AFTERNOON_ID);
  });

  test('rejects with the HTTP status when the API refuses', async () => {
    server.use(
      http.post(
        `${baseUrl}/registers`,
        () => new HttpResponse(null, { status: 401 }),
      ),
    );

    await expect(
      registerService.createRegister(mockCourseMorning),
    ).rejects.toMatchObject({ name: 'HttpError', status: 401 });
  });
});

describe('registerService.getRegister', () => {
  test('gets the register with its students', async () => {
    const register = await registerService.getRegister(
      MOCK_REGISTER_AFTERNOON_ID,
    );

    expect(register).toStrictEqual(mockRegisterAfternoon);
  });

  test('rejects with a 404 when the register does not exist', async () => {
    await expect(registerService.getRegister(999)).rejects.toMatchObject({
      name: 'HttpError',
      status: 404,
    });
  });
});
