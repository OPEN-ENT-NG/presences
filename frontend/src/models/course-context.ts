/**
 * Course context passed to the register page through URL parameters
 * (`/presences/app/registers?courseId=&structureId=&date=&startTime=&endTime=`).
 * Parameter names: hypothesis to be validated (FS-01 §6).
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
