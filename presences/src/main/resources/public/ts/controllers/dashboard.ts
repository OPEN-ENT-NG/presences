import {ng} from 'entcore';
import {SearchItem, SearchService} from "../services";

declare let window: any;

interface Filter {
    search: {
        item: string;
        items: SearchItem[];
    }
}

interface ViewModel {
    filter: Filter;

    selectItem(model: any, student: any): void;

    searchItem(value: string): void;
}

export const dashboardController = ng.controller('DashboardController', ['$scope', 'route', '$location', 'SearchService',
    function ($scope, route, $location, SearchService: SearchService) {
        const vm: ViewModel = this;
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
        }
    }]);