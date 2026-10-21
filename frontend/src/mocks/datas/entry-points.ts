/**
 * Constants of the mocked day, with no import so that the Vite config can use
 * them (see `vite/plugins/mockMode.ts`).
 */
export const MOCK_STRUCTURE_ID = 'structure-1';
/** A Tuesday: two courses for the CE1 A class (1D: two courses a day at most). */
export const MOCK_DATE = '2026-10-13';

/** Structures whose courses the mocked API fails to give, to see the error screens */
export const MOCK_STRUCTURE_FORBIDDEN = 'structure-forbidden';
export const MOCK_STRUCTURE_SERVER_ERROR = 'structure-server-error';
export const MOCK_STRUCTURE_OFFLINE = 'structure-offline';
export const MOCK_STRUCTURE_SESSION_EXPIRED = 'structure-session-expired';

export const MOCK_MORNING = { startTime: '08:30', endTime: '11:45' };
export const MOCK_AFTERNOON = { startTime: '13:30', endTime: '16:30' };

export interface MockEntryPoint {
  label: string;
  /** What the AngularJS buttons give to the app: the course context in the URL */
  path: string;
}

const entryPath = (
  courseId: string,
  { startTime, endTime }: { startTime: string; endTime: string },
  structureId = MOCK_STRUCTURE_ID,
) =>
  `/registers?${new URLSearchParams({
    courseId,
    structureId,
    date: MOCK_DATE,
    startTime,
    endTime,
  })}`;

/** URLs that work in mock mode, the first one is opened in the browser. */
export const mockEntryPoints: MockEntryPoint[] = [
  {
    label: 'Morning, register created on opening',
    path: entryPath('course-morning', MOCK_MORNING),
  },
  {
    label: 'Afternoon, register already validated',
    path: entryPath('course-afternoon', MOCK_AFTERNOON),
  },
  {
    label: 'Morning, class without any student',
    path: entryPath('course-empty-class', MOCK_MORNING),
  },
  {
    label: 'Error: course not found',
    path: entryPath('unknown-course', MOCK_MORNING),
  },
  {
    label: 'Error: the course does not allow a register',
    path: entryPath('course-not-allowed', MOCK_MORNING),
  },
  {
    label: 'Error: access denied by the API',
    path: entryPath('course-morning', MOCK_MORNING, MOCK_STRUCTURE_FORBIDDEN),
  },
  {
    label: 'Error: session expired',
    path: entryPath(
      'course-morning',
      MOCK_MORNING,
      MOCK_STRUCTURE_SESSION_EXPIRED,
    ),
  },
  {
    label: 'Error: server error',
    path: entryPath(
      'course-morning',
      MOCK_MORNING,
      MOCK_STRUCTURE_SERVER_ERROR,
    ),
  },
  {
    label: 'Error: offline',
    path: entryPath('course-morning', MOCK_MORNING, MOCK_STRUCTURE_OFFLINE),
  },
  {
    label: 'Error: invalid course context (parameters missing)',
    path: '/registers?courseId=course-morning',
  },
  {
    label: 'Error: register not found',
    path: '/registers/999',
  },
];
