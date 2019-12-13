import {Course, Register} from "../models";

export class RegisterUtils {
    
    static createRegisterFromCourse = function (course: Course): Register {
        const register = new Register();
        if (course.registerId) {
            register.id = course.registerId;
            register.course_id = course.id;
            register.splitSlot = course.splitSlot;
        } else {
            register.course_id = course.id;
            register.structure_id = course.structureId;
            register.start_date = course.startDate;
            register.end_date = course.endDate;
            register.subject_id = course.subjectId;
            register.groups = course.groups;
            register.classes = course.classes;
            register.splitSlot = course.splitSlot;
        }

        return register;
    };

}