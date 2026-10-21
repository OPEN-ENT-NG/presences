import { CourseContext } from '~/models/course-context';
import { courseService, HttpError, registerService } from '~/services/api';

/**
 * Opens the register of the course given by the AngularJS page, and returns
 * its identifier. The register is created when it does not exist yet.
 *
 * Kept apart from the pages so that the creation can move elsewhere later.
 */
export const openRegister = async ({
  courseId,
  structureId,
  date,
}: CourseContext): Promise<number> => {
  // The API cannot filter by course identifier: read the day, then pick the course
  const courses = await courseService.getCourses({ structureId, date });
  const course = courses.find((course) => course.id === courseId);

  if (!course) {
    throw new HttpError(404, 'Course not found');
  }
  // Same rule as the AngularJS application
  if (!course.allowRegister) {
    throw new HttpError(403, 'Register not allowed for this course');
  }
  // Already created: nothing to write
  if (course.registerId !== null) {
    return course.registerId;
  }

  const register = await registerService.createRegister(course);
  return register.id;
};
