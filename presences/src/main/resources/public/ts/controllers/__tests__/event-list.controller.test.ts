import {ng} from '@presences/models/__mocks__/entcore';
import * as angular from 'angular';
import * as angularMock from 'angular-mocks/angular-mocks';
import {eventListController} from "@presences/controllers";
import {GroupService} from "@common/services";
import {EventService, ReasonService} from "@presences/services";
import {EventResponse} from "@presences/models";
import {EventsUtils} from "@presences/utilities";
import {EventType} from "@presences/models/EventType";

describe('EventListController', () => {

    let vm: any;
    let eventItem: EventResponse = {
        action_abbreviation: null,
        counsellor_regularisation: false,
        created: null,
        date: null,
        massmailed: null,
        reason: null,
        student: null,
        events: [{
            id: null,
            counsellor_regularisation: false,
            student_id: null,
            reason_id: 1,
            type: EventsUtils.ALL_EVENTS.absence,
            type_id: EventType.ABSENCE
        }],
        page: null,
        type: null
    };

    beforeEach(() => {
        const testApp = angular.module('app', []);
        let $controller, $rootScope;

        angularMock.module('app');

        eventListController;

        ng.initMockedModules(testApp);

        // Controller Injection
        angularMock.inject((_$controller_, _$rootScope_) => {
            // The injector unwraps the underscores (_) from around the parameter names when matching
            $controller = _$controller_;
            $rootScope = _$rootScope_;
        });

        // Creates a new instance of scope
        let $scope = $rootScope.$new();

        // Fetching $location
        testApp.run(($rootScope, $location) => {
            $rootScope.location = $location;
        });

        vm = $controller('EventListController', {
            $scope: $scope,
            $route: undefined,
            $location: $rootScope.location,
            $timeout: undefined,
            GroupService: GroupService,
            ReasonService: ReasonService,
            eventService: EventService
        });


    });

    it('test regularizedChecked', async (done): Promise<void> => {

        let event = eventItem;

        expect(vm.regularizedChecked(event)).toEqual(false);

        event.events.push({
            id: null,
            counsellor_regularisation: true,
            student_id: null,
            reason_id: 1,
            type: EventsUtils.ALL_EVENTS.absence
        });

        expect(vm.regularizedChecked(event)).toEqual(true);

        done();
    });

    it('test isEachEventAbsence', async (done): Promise<void> => {
        let event = eventItem;
        expect(vm.isEachEventAbsence(event)).toEqual(true);
        done();
    });

    it('test isEachEventLateness', async (done): Promise<void> => {
        let event = eventItem;
        expect(vm.isEachEventLateness(event)).toEqual(false);
        done();
    });
});