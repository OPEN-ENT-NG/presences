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
