import {Me} from "entcore";
import {Course, Register} from "../models";

export class RegisterUtils {

    static async initPreference(isActive?: boolean): Promise<void> {
        if (Me && Me.preferences) {
            let registerPreference = await Me.preference('register');
            if (!registerPreference) {
                Me.preferences.register = {};
            }
            if (isActive != undefined) {
                Me.preferences.register.multipleSlot = isActive;
            } else {
                Me.preferences.register.multipleSlot = registerPreference ? registerPreference.multipleSlot : true
            }
            await Me.savePreference('register');
        }
    }

    static createRegisterFromCourse = function (course: Course): Register {
        const register = new Register();
        if (course.register_id) {
            register.id = course.register_id;
            register.course_id = course._id;
            register.split_slot = course.split_slot;
        } else {
            register.course_id = course._id;
            register.structure_id = course.structureId;
            register.start_date = course.startDate;
            register.end_date = course.endDate;
            register.subject_id = course.subjectId;
            register.groups = course.groups;
            register.classes = course.classes;
            register.split_slot = course.split_slot;
        }

        return register;
    };

}