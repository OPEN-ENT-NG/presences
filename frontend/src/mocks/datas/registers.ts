import { Course } from '~/models/course';
import {
  CreatedRegister,
  EventType,
  Register,
  RegisterEvent,
  RegisterState,
  RegisterStudent,
} from '~/models/register';
import {
  MOCK_REGISTER_AFTERNOON_ID,
  MOCK_REGISTER_EMPTY_CLASS_ID,
  MOCK_REGISTER_MORNING_ID,
  mockCourseAfternoon,
  mockCourseEmptyClass,
  mockCourseMorning,
} from './courses';

const mockClassGroup = { id: 'class-ce1a', type: 'CLASS' };

/** `YYYY-MM-DD HH:mm:ss` (course) to `YYYY-MM-DDTHH:mm:ss` (event). */
const toEventDate = (courseDate: string) => courseDate.replace(' ', 'T');

/** `YYYY-MM-DD HH:mm:ss` (course) to `YYYY-MM-DDTHH:mm:ss.SSS` (register). */
const toRegisterDate = (courseDate: string) => `${toEventDate(courseDate)}.000`;

const mockStudent = (
  id: string,
  name: string,
  overrides: Partial<RegisterStudent> = {},
): RegisterStudent => ({
  id,
  name,
  birth_day: null,
  group: mockClassGroup.id,
  group_name: 'CE1 A',
  events: [],
  day_history: [],
  last_course_absent: false,
  forgotten_notebook: false,
  exempted: false,
  exemptions: [],
  ...overrides,
});

const mockAbsence = (
  registerId: number,
  course: Course,
  overrides: Partial<RegisterEvent> = {},
): RegisterEvent => ({
  id: 9000 + registerId,
  type_id: EventType.ABSENCE,
  start_date: toEventDate(course.startDate),
  end_date: toEventDate(course.endDate),
  comment: '',
  owner: {
    id: 'teacher-1',
    displayName: 'MARTIN Claire',
    firstName: 'Claire',
    lastName: 'MARTIN',
    info: null,
  },
  counsellor_input: false,
  counsellor_regularisation: false,
  followed: false,
  massmailed: false,
  register_id: registerId,
  reason_id: null,
  ...overrides,
});

/** CE1 A, in reverse alphabetical order like the API: the page has to sort. */
const mockClassStudents = (): RegisterStudent[] => [
  mockStudent('student-1', 'PETIT Léa'),
  mockStudent('student-2', 'BERNARD Hugo', { last_course_absent: true }),
  mockStudent('student-3', 'ROUX Inès'),
  mockStudent('student-4', 'ANDRÉ Lucas'),
  mockStudent('student-5', 'MOREAU Jade'),
  mockStudent('student-6', 'LEFEBVRE Nathan'),
  mockStudent('student-7', 'GIRARD Chloé'),
  mockStudent('student-8', 'FOURNIER Adam'),
];

const mockRegister = (
  course: Course,
  state: RegisterState,
  students: RegisterStudent[],
): Register => ({
  personnel_id: course.teachers[0].id,
  proof_id: null,
  course_id: course.id,
  owner: course.teachers[0].id,
  notified: false,
  subject_id: course.subjectId,
  start_date: toRegisterDate(course.startDate),
  end_date: toRegisterDate(course.endDate),
  structure_id: course.structureId,
  counsellor_input: false,
  state_id: state,
  groups: course.classes.length ? [mockClassGroup] : [],
  teachers: course.teachers.map((teacher) => ({ ...teacher, functions: [] })),
  students,
});

/** Morning: just created, with an absence pre-marked by the head teacher. */
export const mockRegisterMorning = mockRegister(
  mockCourseMorning,
  RegisterState.TODO,
  mockClassStudents().map((student) =>
    student.id === 'student-5'
      ? {
          ...student,
          events: [
            mockAbsence(MOCK_REGISTER_MORNING_ID, mockCourseMorning, {
              counsellor_input: true,
            }),
          ],
        }
      : student,
  ),
);

/** Afternoon: validated, with an absence. */
export const mockRegisterAfternoon = mockRegister(
  mockCourseAfternoon,
  RegisterState.DONE,
  mockClassStudents().map((student) =>
    student.id === 'student-6'
      ? {
          ...student,
          events: [
            mockAbsence(MOCK_REGISTER_AFTERNOON_ID, mockCourseAfternoon),
          ],
        }
      : student,
  ),
);

/** Class without any student. */
export const mockRegisterEmptyClass = mockRegister(
  mockCourseEmptyClass,
  RegisterState.TODO,
  [],
);

/** Registers by identifier: the API response does not contain it. */
export const mockRegisters: Record<number, Register> = {
  [MOCK_REGISTER_MORNING_ID]: mockRegisterMorning,
  [MOCK_REGISTER_AFTERNOON_ID]: mockRegisterAfternoon,
  [MOCK_REGISTER_EMPTY_CLASS_ID]: mockRegisterEmptyClass,
};

export const toCreatedRegister = (
  id: number,
  register: Register,
): CreatedRegister => ({
  id,
  structure_id: register.structure_id,
  course_id: register.course_id,
  subject_id: register.subject_id,
  start_date: register.start_date,
  end_date: register.end_date,
  counsellor_input: register.counsellor_input,
  state_id: register.state_id,
});
