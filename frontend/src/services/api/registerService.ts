import { odeServices } from '@edifice.io/client';
import { Course } from '~/models/course';
import {
  CreateRegisterPayload,
  CreatedRegister,
  Register,
} from '~/models/register';
import { baseUrl } from '.';
import { checkHttpError } from './http';

/** Builds the register creation body from a course, like the AngularJS application does. */
export const toCreateRegisterPayload = (
  course: Course,
): CreateRegisterPayload => ({
  course_id: course.id,
  structure_id: course.structureId,
  start_date: course.startDate,
  end_date: course.endDate,
  subject_id: course.subjectId,
  groups: course.groups,
  classes: course.classes,
  teacherIds: course.teachers.map((teacher) => teacher.id),
  split_slot: course.splitSlot,
});

export const createRegisterService = () => {
  return {
    /**
     * Create the register of a course, or get the existing one: the API is
     * idempotent for a given course and dates.
     * Kept apart so that the creation can move elsewhere later.
     */
    createRegister(course: Course) {
      return checkHttpError(
        odeServices
          .http()
          .postJson<CreatedRegister>(
            `${baseUrl}/registers`,
            toCreateRegisterPayload(course),
          ),
      );
    },

    /**
     * Get a register with its students and their events.
     * @param registerId - Identifier of the register.
     */
    getRegister(registerId: number) {
      return checkHttpError(
        odeServices.http().get<Register>(`${baseUrl}/registers/${registerId}`),
      );
    },
  };
};
