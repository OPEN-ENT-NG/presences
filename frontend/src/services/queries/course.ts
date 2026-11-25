import { queryOptions } from '@tanstack/react-query';
import { CourseDay } from '~/models/course';
import { courseService } from '~/services/api';

/**
 * coursesQueryOptions: the courses of a structure for one day.
 * Read when opening a register, then by the register page for its title.
 */
export const coursesQueryOptions = ({ structureId, date }: CourseDay) =>
  queryOptions({
    queryKey: ['courses', structureId, date],
    queryFn: () => courseService.getCourses({ structureId, date }),
  });
