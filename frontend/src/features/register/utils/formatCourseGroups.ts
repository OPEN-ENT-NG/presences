import { Course } from '~/models/course';

/**
 * Classes then groups of a course, as shown by the AngularJS register page
 * (`[...course.classes, ...course.groups]`), e.g. `CP, CE1, Chorale`.
 */
export const formatCourseGroups = ({
  classes,
  groups,
}: Pick<Course, 'classes' | 'groups'>): string =>
  [...classes, ...groups].join(', ');
