import {Course, Courses, Register, RegisterStudent} from "../models";
import {_, model, moment} from "entcore";
import rights from "../rights";
import {Mix} from "entcore-toolkit";

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

    static isAbsenceDisabled = function (student: RegisterStudent, register: Register): boolean {
        if (student.absence !== undefined && student.absence.counsellor_input) {
            return !model.me.hasWorkflow(rights.workflow.managePresences);
        }
        if (student.exempted_subjectId === register.subject_id || student.exemption_recursive_id != null) {
            if (student.exempted && !student.exemption_attendance) {
                return true;
            }
        }
        return false;
    };

    static initCourseToFilter = function (): Course {
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
            teachers: []
        }
    }

    static appendCoursesMap = function (extraCourses: Course[], currentCourses: Courses): void {
        let dataCourses: Course[] = Mix.castArrayAs(Course, extraCourses);
        dataCourses.forEach((course: Course) => course.timestamp = moment(course.startDate).valueOf());
        dataCourses = _.sortBy(dataCourses, 'timestamp');
        currentCourses.all = currentCourses.all.concat(dataCourses);


        const map: Map<number, Course[]> = RegisterUtils.formatMapCourseByDate(dataCourses);
        const keyOrders: number[] = Array.from(map.keys()).reverse();
        currentCourses.keysOrder = currentCourses.keysOrder.concat(keyOrders);

        // remove potential duplicate data
        currentCourses.keysOrder = currentCourses.keysOrder.filter((item: number, index: number) => {
            return (currentCourses.keysOrder.indexOf(item) == index)
        })

        // append map we created with the current courses map
        currentCourses.map = new Map([...Array.from(currentCourses.map.entries()), ...Array.from(map.entries())]);

    }

    static formatMapCourseByDate = function (courses: Course[]): Map<number, Course[]> {
        const map: Map<number, Course[]> = new Map<number, Course[]>();
        for (let i = 0; i < courses.length; i++) {
            const course: Course = courses[i];
            const start: number = moment(course.startDate).startOf('day').valueOf();
            if (!map.has(start)) {
                map.set(start, []);
            }
            map.get(start).push(course);
        }
        return map;
    }

}