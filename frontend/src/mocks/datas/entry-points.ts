/**
 * Constants of the mocked day, with no import so that the Vite config can use
 * them (see `vite/plugins/mockMode.ts`).
 */
export const MOCK_STRUCTURE_ID = 'structure-1';
/** A Tuesday: two courses for the CE1 A class (1D: two courses a day at most). */
export const MOCK_DATE = '2026-10-13';

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
) =>
  `/registers?${new URLSearchParams({
    courseId,
    structureId: MOCK_STRUCTURE_ID,
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
];
