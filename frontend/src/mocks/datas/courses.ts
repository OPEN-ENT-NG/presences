import { Course } from '~/models/course';
import { RegisterState } from '~/models/register';
import {
  MOCK_AFTERNOON,
  MOCK_DATE,
  MOCK_MORNING,
  MOCK_STRUCTURE_ID,
} from './entry-points';

export { MOCK_DATE, MOCK_STRUCTURE_ID };

/** Identifiers of the mocked registers, see `registers.ts` */
export const MOCK_REGISTER_MORNING_ID = 101;
export const MOCK_REGISTER_AFTERNOON_ID = 102;
export const MOCK_REGISTER_EMPTY_CLASS_ID = 103;

const mockTeacher = { id: 'teacher-1', displayName: 'MARTIN Claire' };

/** Morning course whose register has not been created yet (first opening). */
export const mockCourseMorning: Course = {
  id: 'course-morning',
  structureId: MOCK_STRUCTURE_ID,
  subjectId: 'subject-1',
  subjectName: 'Enseignement général',
  classes: ['CE1 A'],
  groups: [],
  teachers: [mockTeacher],
  startDate: `${MOCK_DATE} ${MOCK_MORNING.startTime}:00`,
  endDate: `${MOCK_DATE} ${MOCK_MORNING.endTime}:00`,
  splitSlot: false,
  allowRegister: true,
  registerId: null,
  registerStateId: null,
};

/** Afternoon course whose register is already validated. */
export const mockCourseAfternoon: Course = {
  ...mockCourseMorning,
  id: 'course-afternoon',
  startDate: `${MOCK_DATE} ${MOCK_AFTERNOON.startTime}:00`,
  endDate: `${MOCK_DATE} ${MOCK_AFTERNOON.endTime}:00`,
  registerId: MOCK_REGISTER_AFTERNOON_ID,
  registerStateId: RegisterState.DONE,
};

/** Course of a class without any student. */
export const mockCourseEmptyClass: Course = {
  ...mockCourseMorning,
  id: 'course-empty-class',
  classes: ['CP B'],
  teachers: [{ id: 'teacher-2', displayName: 'DURAND Paul' }],
};

/** Course that does not allow a register (set by the structure tags). */
export const mockCourseNotAllowed: Course = {
  ...mockCourseMorning,
  id: 'course-not-allowed',
  startDate: `${MOCK_DATE} ${MOCK_AFTERNOON.startTime}:00`,
  endDate: `${MOCK_DATE} ${MOCK_AFTERNOON.endTime}:00`,
  classes: ['CM1 C'],
  allowRegister: false,
};

export const mockCourses: Course[] = [
  mockCourseMorning,
  mockCourseAfternoon,
  mockCourseEmptyClass,
  mockCourseNotAllowed,
];
