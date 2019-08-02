import {model, moment, ng} from 'entcore';
import {SearchItem, SearchService} from "../services";
import {DateUtils} from "@common/utils";
import rights from '../rights';

declare let window: any;

interface Filter {
    search: {
        item: string;
        items: SearchItem[];
    }
}

interface ViewModel {
    filter: Filter;
    date: string;

    selectItem(model: any, student: any): void;

    searchItem(value: string): void;

    getSubSize(): string;
}

export const dashboardController = ng.controller('DashboardController', ['$scope', 'route', '$location', 'SearchService',
    function ($scope, route, $location, SearchService: SearchService) {
        const vm: ViewModel = this;
        vm.date = DateUtils.format(moment(), DateUtils.FORMAT["DATE-FULL-LETTER"]);
        vm.filter = {
            search: {
                item: null,
                items: null
            }
        };

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

        vm.getSubSize = function () {
            const SIDE_HEIGHT = 54;
            const hasRegisterWidget = model.me.hasWorkflow(rights.workflow.widget_current_course);
            const hasAbsencesWidget = model.me.hasWorkflow(rights.workflow.widget_absences);
            return `calc(100% - ${0
            + (hasRegisterWidget ? SIDE_HEIGHT : 0)
            + (hasAbsencesWidget ? SIDE_HEIGHT : 0)}px)`;
        }
    }]);