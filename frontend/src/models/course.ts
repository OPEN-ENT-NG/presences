/**
 * Course as returned by `GET /presences/courses` (Jackson serialization of the
 * Java `Course` model: field names follow its getters, e.g. `id`, not `_id`).
 * Only the fields used by the register page are typed.
 */
export interface Course {
  id: string;
  structureId: string;
  subjectId: string;
  subjectName: string;
  /** Class names */
  classes: string[];
  /** Group names */
  groups: string[];
  teachers: CourseTeacher[];
  /** `YYYY-MM-DD HH:mm:ss` */
  startDate: string;
  /** `YYYY-MM-DD HH:mm:ss` */
  endDate: string;
  splitSlot: boolean;
  allowRegister: boolean;
  /** `null` as long as the register has not been created */
  registerId: number | null;
  /** `null` as long as the register has not been created */
  registerStateId: number | null;
}

export interface CourseTeacher {
  id: string;
  displayName: string;
}
