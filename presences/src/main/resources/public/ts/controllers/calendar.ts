import {model, ng} from 'entcore';
import {Scope} from './main'

declare let window: any;

export const calendarController = ng.controller('CalendarController', ['$scope', 'route', 'StructureService',
    function ($scope: Scope, route, StructureService) {
        console.log('CalendarController');

        const vm = this;
        vm.courses = [];
        vm.slots = {list: []};

        model.calendar.eventer.on('calendar.create-item', () => {
            console.info(model.calendar.newItem);
            console.info(model.calendar.newItem.beginning.format());
            console.info(model.calendar.newItem.beginning.toString());
            console.info(model.calendar.newItem.beginning.isValid());
            console.info(model.calendar.newItem.beginning.toLocaleString());
        });

        $scope.$watch(() => window.structure, async () => {
            const structure_slots = await StructureService.getSlotProfile(window.structure.id);
            if (Object.keys(structure_slots).length > 0) vm.slots.list = structure_slots.slots;
            else vm.slots.list = null;
            $scope.safeApply();
        });
    }]);