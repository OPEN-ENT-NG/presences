import {Course, Courses} from "@presences/models";

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
});