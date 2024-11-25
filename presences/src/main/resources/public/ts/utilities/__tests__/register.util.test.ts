import {Course, Register} from "@presences/models";
import { RegisterUtils } from "@presences/utilities";

describe('RegisterUtils', () => {

    it('test initCourseToFilter', done => {
       const course: Course = {
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

       expect(RegisterUtils.initCourseToFilter()).toEqual(course);
       done();
    });

    it('test createRegisterFromCourse with register not allowed', done => {

        const course: Course = {
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
            allowRegister: false
        };

        expect(RegisterUtils.createRegisterFromCourse(course)).toEqual(undefined);
        done();
    });

    it('test createRegisterFromCourse', done => {

        const course: Course = {
            classes: [],
            dayOfWeek: 0,
            endDate: "2022-12-31 23:59:59",
            groups: [],
            id: "id",
            notified: false,
            roomLabels: [],
            splitSlot: false,
            startDate: "2022-01-01 00:00:00",
            structureId: "structureId",
            subjectId: "subjectId",
            subjectName: "",
            teachers: [{id: 'teacherId', displayName: 'name'}],
            isOpenedByPersonnel: false,
            allowRegister: true
        };

        const register: Register = RegisterUtils.createRegisterFromCourse(course);

        expect(register.course_id).toEqual(course.id);
        expect(register.structure_id).toEqual(course.structureId);
        expect(register.start_date).toEqual(course.startDate);
        expect(register.end_date).toEqual(course.endDate);
        expect(register.subject_id).toEqual(course.subjectId);
        expect(register.groups).toEqual(course.groups);
        expect(register.classes).toEqual(course.classes);
        expect(register.teacherIds).toEqual([course.teachers[0].id]);
        expect(register.splitSlot).toEqual(course.splitSlot);

        done();
    });

});