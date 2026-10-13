import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CourseContext } from '~/models/course-context';

const DATE_PATTERN = /^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$/;
const TIME_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/;

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
    !TIME_PATTERN.test(startTime) ||
    !TIME_PATTERN.test(endTime) ||
    startTime >= endTime
  ) {
    return null;
  }

  return { courseId, structureId, date, startTime, endTime };
};

export const useCourseContext = (): CourseContext | null => {
  const [searchParams] = useSearchParams();
  return useMemo(() => parseCourseContext(searchParams), [searchParams]);
};
