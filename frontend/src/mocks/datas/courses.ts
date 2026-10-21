import { Course } from '~/models/course';
import { RegisterState } from '~/models/register';

export const MOCK_STRUCTURE_ID = 'structure-1';
/** Identifiers of the mocked registers, see `registers.ts` */
export const MOCK_REGISTER_MORNING_ID = 101;
export const MOCK_REGISTER_AFTERNOON_ID = 102;
export const MOCK_REGISTER_EMPTY_CLASS_ID = 103;
/** A Tuesday: two courses for the CE1 A class (1D: two courses a day at most). */
export const MOCK_DATE = '2026-10-13';

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
  startDate: `${MOCK_DATE} 08:30:00`,
  endDate: `${MOCK_DATE} 11:45:00`,
  splitSlot: false,
  allowRegister: true,
  registerId: null,
  registerStateId: null,
};

/** Afternoon course whose register is already validated. */
export const mockCourseAfternoon: Course = {
  ...mockCourseMorning,
  id: 'course-afternoon',
  startDate: `${MOCK_DATE} 13:30:00`,
  endDate: `${MOCK_DATE} 16:30:00`,
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

export const mockCourses: Course[] = [
  mockCourseMorning,
  mockCourseAfternoon,
  mockCourseEmptyClass,
];
