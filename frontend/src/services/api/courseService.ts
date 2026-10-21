import { odeServices } from '@edifice.io/client';
import { Course } from '~/models/course';
import { baseUrl } from '.';
import { checkHttpError } from './http';

export const createCourseService = () => {
  return {
    /**
     * Get the courses of a structure for one day.
     * The API cannot filter by course identifier: the caller picks the course in the day.
     * @param structureId - Structure of the courses.
     * @param date - Day of the courses, `YYYY-MM-DD`.
     */
    getCourses({ structureId, date }: { structureId: string; date: string }) {
      return checkHttpError(
        odeServices.http().get<Course[]>(`${baseUrl}/courses`, {
          queryParams: { structure: structureId, start: date, end: date },
        }),
      );
    },
  };
};
