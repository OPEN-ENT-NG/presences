import { odeServices } from '@edifice.io/client';
import { Course, CourseDay } from '~/models/course';
import { baseUrl } from '.';
import { checkHttpError } from './http';

export const createCourseService = () => {
  return {
    /**
     * Get the courses of a structure for one day.
     * The API cannot filter by course identifier: the caller picks the course in the day.
     */
    getCourses({ structureId, date }: CourseDay) {
      return checkHttpError(
        odeServices.http().get<Course[]>(`${baseUrl}/courses`, {
          queryParams: { structure: structureId, start: date, end: date },
        }),
      );
    },
  };
};
