import {ng} from 'entcore';

export const pagination = ng.directive('pagination', () => ({
    restrict: 'E',
    scope: {
        pageCount: '=',
        pageNumber: '=',
        ngChange: '&'
    },
    template: `
        <ul class="pagination" ng-if="pageCount > 1">
          <li class="pagination-item arrow" data-ng-click="previousPage()" ng-show="pageCount > 1">
             <a class="pagination-item-link">❮</a>
          </li>
          
          <li class="pagination-item" data-ng-click="startPage()" ng-show="pageCount > 2 && pageNumber + 1 !== 1">
            <a class="pagination-item-link">1</a>
          </li>
          
          <li class="pagination-item" ng-show="pageNumber > 2 && pageNumber !== 1">
            <a class="pagination-item-link">...</a>
          </li>
          
          <li class="pagination-item active">
            <a class="pagination-item-link">[[pageNumber + 1]]</a>
          </li>
          
          <li class="pagination-item" ng-show="pageNumber + 1 !== (pageCount - 1) && pageNumber + 1 !== pageCount">
            <a class="pagination-item-link">...</a>
          </li>
          
          <li class="pagination-item" data-ng-click="endPage()" ng-show="pageNumber + 1 !== pageCount">
            <a class="pagination-item-link">[[pageCount]]</a>
          </li>

          <li class="pagination-item arrow" data-ng-click="nextPage()" ng-show="pageCount > 1">
             <a class="pagination-item-link">❯</a>
          </li>
        </ul>
    `,
    link: function ($scope) {
        $scope.previousPage = function () {
            if ($scope.pageNumber !== 1) {
                $scope.pageNumber--;
            }
        };

        $scope.nextPage = function () {
            if ($scope.pageNumber < $scope.pageCount) {
                $scope.pageNumber++;
            }
        };

        $scope.startPage = function () {
            $scope.pageNumber = 0;
        };

        $scope.endPage = function () {
            $scope.pageNumber = $scope.pageCount - 1;
        };
    }
}));
