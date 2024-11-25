import {ng} from '@presences/models/__mocks__/entcore';
import * as angular from 'angular';
import 'angular-mocks';

import {DayCourseVm, dayCourse} from '@presences/controllers/widgets/day-course';
import {Course} from "@presences/models";
import DoneCallback = jest.DoneCallback;

describe('DayCourse', () => {

    let dayCourseVm: DayCourseVm;

    beforeEach(() => {
        const testApp = angular.module('app', []);
        let $controller, $rootScope;

        angular.mock.module('app');


        dayCourse;

        ng.initMockedModules(testApp);

        // Controller Injection
        angular.mock.inject((_$controller_, _$rootScope_) => {
            // The injector unwraps the underscores (_) from around the parameter names when matching
            $controller = _$controller_;
            $rootScope = _$rootScope_;
        });

        // Creates a new instance of scope
        let $scope = $rootScope.$new();

        dayCourseVm = $controller('DayCourse', {
            $scope: $scope
        });


    });

    it('test isFutureCourse', (done: DoneCallback): void => {
        let course: Course = undefined;

        expect(dayCourseVm.isFutureCourse(course)).toEqual(true);

        let startDate: string = '2020-01-01 00:00:00';
        course = new Course();
        course.startDate = startDate;

        expect(dayCourseVm.isFutureCourse(course)).toEqual(false);

        done();
    });


});