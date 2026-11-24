/**
 * Course context passed to the register page through URL parameters
 * (`/presences/app/registers?courseId=&structureId=&date=&startTime=&endTime=`).
 * TODO: parameter names are still to be validated.
 */
export interface CourseContext {
  courseId: string;
  structureId: string;
  /** Course day, `YYYY-MM-DD` */
  date: string;
  /** Start time, `HH:mm` */
  startTime: string;
  /** End time, `HH:mm` */
  endTime: string;
}

const DATE_PATTERN = /^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$/;
const TIME_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/;

/** Checks that a `YYYY-MM-DD` string is an existing calendar date (rejects e.g. 2026-02-31). */
const isExistingDate = (date: string): boolean => {
  const [year, month, day] = date.split('-').map(Number);
  const parsed = new Date(Date.UTC(year, month - 1, day));
  return (
    parsed.getUTCFullYear() === year &&
    parsed.getUTCMonth() === month - 1 &&
    parsed.getUTCDate() === day
  );
};

/**
 * Reads the course context from the URL parameters.
 * Returns `null` if a parameter is missing or malformed.
 */
export const parseCourseContext = (
  searchParams: URLSearchParams,
): CourseContext | null => {
  const courseId = searchParams.get('courseId');
  const structureId = searchParams.get('structureId');
  const date = searchParams.get('date');
  const startTime = searchParams.get('startTime');
  const endTime = searchParams.get('endTime');

  if (!courseId || !structureId || !date || !startTime || !endTime) {
    return null;
  }
  if (
    !DATE_PATTERN.test(date) ||
    !isExistingDate(date) ||
    !TIME_PATTERN.test(startTime) ||
    !TIME_PATTERN.test(endTime) ||
    startTime >= endTime
  ) {
    return null;
  }

  return { courseId, structureId, date, startTime, endTime };
};
