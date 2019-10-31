import {model, moment, ng} from 'entcore';
import {Group, GroupService, SearchItem, SearchService} from "../services";
import {DateUtils} from "@common/utils";
import rights from '../rights';
import {EventType} from "../models";

declare let window: any;

interface Filter {
    search: {
        item: string;
        items: SearchItem[];
    }
    searchClass: {
        item: string;
        items: Group[];
    }
}

interface ViewModel {
    filter: Filter;
    date: string;
    eventType: string[]; /* [0]:ABSENCE, [1]:LATENESS, [2]:INCIDENT, [3]:DEPARTURE */

    selectItem(model: any, student: any): void;

    searchItem(value: string): void;

    selectItemToRegistry(model: any, student: any): void;

    searchItemToRegistry(value: string): void;


    getSubSize(): string;
}

export const dashboardController = ng.controller('DashboardController', ['$scope', 'route', '$location',
    'SearchService', 'GroupService',
    function ($scope, route, $location, SearchService: SearchService, GroupService: GroupService) {
        const vm: ViewModel = this;
        vm.date = DateUtils.format(moment(), DateUtils.FORMAT["DATE-FULL-LETTER"]);
        vm.filter = {
            search: {
                item: null,
                items: null
            },
            searchClass: {
                item: null,
                items: null
            }
        };
        vm.eventType = [
            EventType[EventType.ABSENCE],
            EventType[EventType.LATENESS],
            EventType[EventType.INCIDENT],
            EventType[EventType.DEPARTURE]
        ];

        /* Calendar interaction */
        vm.selectItem = function (model, item) {
            window.item = item;
            $location.path(`/calendar/${item.id}`);
        };

        vm.searchItem = async function (value) {
            const structureId = window.structure.id;
            try {
                vm.filter.search.items = await SearchService.search(structureId, value);
                $scope.safeApply();
            } catch (err) {
                vm.filter.search.items = [];
            }
        };

        /* Registry interaction */
        vm.selectItemToRegistry = (model, item) => {
            const structureId = window.structure.id;
            window.item = item;
            $location.path('/registry').search({
                structureId: structureId,
                month: moment(new Date()).format(DateUtils.FORMAT["YEAR-MONTH"]),
                group: [item.id],
                type: vm.eventType
            });
        };

        vm.searchItemToRegistry = async (value) => {
            const structureId = window.structure.id;
            try {
                vm.filter.searchClass.items = await GroupService.search(structureId, value);
                vm.filter.searchClass.items.forEach((item) => item.toString = () => item.name);
                $scope.safeApply();
            } catch (err) {
                vm.filter.searchClass.items = [];
            }
        };

        vm.getSubSize = function () {
            const SIDE_HEIGHT = 54;
            const hasRegisterWidget = model.me.hasWorkflow(rights.workflow.widget_current_course);
            const hasAbsencesWidget = model.me.hasWorkflow(rights.workflow.widget_absences);
            return `calc(100% - ${0
            + (hasRegisterWidget ? SIDE_HEIGHT : 0)
            + (hasAbsencesWidget ? SIDE_HEIGHT : 0)}px)`;
        }
    }]);