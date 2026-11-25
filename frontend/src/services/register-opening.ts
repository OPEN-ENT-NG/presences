import { QueryClient } from '@tanstack/react-query';
import { findCourse } from '~/models/course';
import { CourseContext } from '~/models/course-context';
import { HttpError, registerService } from '~/services/api';
import { coursesQueryOptions } from '~/services/queries/course';

/**
 * Opens the register of the course given by the AngularJS page, and returns
 * its identifier. The register is created when it does not exist yet.
 *
 * Kept apart from the pages so that the creation can move elsewhere later.
 * The courses of the day stay in the query cache: the register page reads its
 * course there.
 */
export const openRegister = async (
  queryClient: QueryClient,
  { courseId, structureId, date }: CourseContext,
): Promise<number> => {
  // The API cannot filter by course identifier: read the day, then pick the course
  const courses = await queryClient.query(
    coursesQueryOptions({ structureId, date }),
  );
  const course = findCourse(courses, courseId);

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
