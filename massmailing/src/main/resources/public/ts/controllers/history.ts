import {ng} from 'entcore';
import {SearchService} from "../services";

declare let window: any;

interface ViewModel {
}

export const historyController = ng.controller('HistoryController',
    ['$scope', '$route', '$location', 'SearchService',
        function ($scope, $route, $location, searchService: SearchService) {
            const vm: ViewModel = this;

            console.log("test");

        }]);