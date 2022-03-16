import {Course, ExemptionRegister, Register, RegisterStudent} from "../models";
import {model} from "entcore";
import rights from "../rights";

export class RegisterUtils {

    static createRegisterFromCourse = (course: Course): Register => {
        if (!course.allowRegister) {
            return;
        }
        const register: Register = new Register();
        register.course_id = course.id;
        if (course.registerId) {
            register.id = course.registerId;
            register.splitSlot = course.splitSlot;
        } else {
            register.structure_id = course.structureId;
            register.start_date = course.startDate;
            register.end_date = course.endDate;
            register.subject_id = course.subjectId;
            register.groups = course.groups;
            register.classes = course.classes;
            register.teacherIds = course.teachers ?
                course.teachers.map((teacher: {displayName: string, id: string}) => teacher.id) : [];
            register.splitSlot = course.splitSlot;
        }

        return register;
    }

    static isAbsenceDisabled = (student: RegisterStudent, register: Register): boolean => {
        if (student.absence !== undefined && student.absence.counsellor_input) {
            return !model.me.hasWorkflow(rights.workflow.managePresences);
        }
        const exempted = student.exemptions.find((exempted: ExemptionRegister) => exempted.subject_id == register.subject_id);
        if (!!exempted) {
            if (student.exempted && !exempted.attendance) {
                return true;
            }
        }
        return false;
    }

    static isAbsenceDisabledWithoutWorkFlow = (student: RegisterStudent, register: Register): boolean => {
        const exempted = student.exemptions.find(exempted => exempted.subject_id == register.subject_id);
        return (!!exempted && student.exempted && !exempted.attendance);
    }

    static initCourseToFilter = (): Course => {
        return {
            classes: [],
            dayOfWeek: 0,
            endDate: "",
            groups: [],
            id: "",
            notified: false,
            roomLabels: [],
            splitSlot: false,
            startDate: "",
            structureId: "",
            subjectId: "",
            subjectName: "",
            teachers: [],
            isOpenedByPersonnel: false,
            allowRegister: true
        };
    }
}