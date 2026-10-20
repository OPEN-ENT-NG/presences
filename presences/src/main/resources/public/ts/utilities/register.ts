import {Course, ExemptionRegister, Register, RegisterStudent} from "../models";
import {model} from "entcore";
import rights from "../rights";
import {DateUtils} from "@common/utils";

export class RegisterUtils {

    static REACT_REGISTER_PATH: string = '/presences/app/registers';

    /**
     * Users with the "Présences Beta" workflow right open registers in the React application.
     */
    static canOpenReactRegister = (): boolean => model.me.hasWorkflow(rights.workflow.presencesBeta);

    /**
     * Builds the React register URL with the course context (the React page creates the register if needed).
     */
    static getReactRegisterUrl = (course: Course): string => {
        const params: URLSearchParams = new URLSearchParams({
            courseId: course.id,
            structureId: course.structureId,
            date: DateUtils.format(course.startDate, DateUtils.FORMAT['YEAR-MONTH-DAY']),
            startTime: DateUtils.format(course.startDate, DateUtils.FORMAT['HOUR-MIN']),
            endTime: DateUtils.format(course.endDate, DateUtils.FORMAT['HOUR-MIN'])
        });
        return `${RegisterUtils.REACT_REGISTER_PATH}?${params.toString()}`;
    }

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
        return RegisterUtils.isAbsenceDisabledWithoutWorkFlow(student, register)
    }

    static isAbsenceDisabledWithoutWorkFlow = (student: RegisterStudent, register: Register): boolean => {
        const exempted: ExemptionRegister = student.exemptions
            .find((exempted: ExemptionRegister) => (exempted.subject_id == register.subject_id || (exempted.recursive_id != null && exempted.recursive_id != undefined)) && !exempted.attendance);
        return !!exempted && student.exempted;
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