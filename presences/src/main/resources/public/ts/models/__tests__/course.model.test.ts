import {Course, Courses, isCourseForgotten} from "@presences/models";
import {RegisterStatus} from "@presences/models/RegisterStatus";
import {moment} from "entcore";

describe('CourseModel', () => {
   it('test courses initialization', done => {
      const courses = new Courses();


      expect(courses.all.length).toEqual(0);
      expect(courses.keysOrder.length).toEqual(0);
      expect(courses.pageSize).toEqual(100);
      expect(courses.hasCourses).toEqual(true);
      done();


   });

    it('test courses clear', done => {
        const courses = new Courses();
        courses.all.push(new Course());
        courses.keysOrder.push(1);
        expect(courses.all.length).toEqual(1);
        expect(courses.keysOrder.length).toEqual(1);

        courses.clear();

        expect(courses.all.length).toEqual(0);
        expect(courses.keysOrder.length).toEqual(0);

        done();
    });

    describe('isCourseForgotten', () => {
        const buildCourse = (startDate: string, register_state_id?: RegisterStatus): Course => {
            const course = new Course();
            course.startDate = startDate;
            course.register_state_id = register_state_id;
            return course;
        };

        it('is not forgotten when the register is DONE, even long past', () => {
            const startDate = moment().subtract(1, 'hour').toISOString();
            expect(isCourseForgotten(buildCourse(startDate, RegisterStatus.DONE))).toEqual(false);
        });

        it('is not forgotten when the course started less than 15 minutes ago', () => {
            const startDate = moment().subtract(5, 'minutes').toISOString();
            expect(isCourseForgotten(buildCourse(startDate, RegisterStatus.TODO))).toEqual(false);
        });

        it('is forgotten when TODO and started more than 15 minutes ago', () => {
            const startDate = moment().subtract(30, 'minutes').toISOString();
            expect(isCourseForgotten(buildCourse(startDate, RegisterStatus.TODO))).toEqual(true);
        });

        it('is forgotten when IN_PROGRESS and started more than 15 minutes ago', () => {
            const startDate = moment().subtract(30, 'minutes').toISOString();
            expect(isCourseForgotten(buildCourse(startDate, RegisterStatus.IN_PROGRESS))).toEqual(true);
        });

        it('is forgotten when there is no register at all and start time has passed', () => {
            const startDate = moment().subtract(30, 'minutes').toISOString();
            expect(isCourseForgotten(buildCourse(startDate, undefined))).toEqual(true);
        });
    });
});