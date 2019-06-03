import {ng} from 'entcore';

export const pagination = ng.directive('pagination', () => ({
    restrict: 'E',
    scope: {
        pageCount: '=',
        pageNumber: '=',
        ngChange: '&'
    },
    template: `
        <ul class="pagination" ng-if="pageCount > 0">
          <li class="pagination-item arrow" data-ng-click="previousPage()" data-ng-show="pageCount > 0">
             <a class="pagination-item-link">❮</a>
          </li>
          
          <li class="pagination-item" data-ng-click="startPage()" data-ng-show="pageCount > 0 && pageNumber + 1 > 1">
            <a class="pagination-item-link">1</a>
          </li>
          
          <li class="pagination-item" data-ng-show="pageNumber > 1 && pageNumber !== 0">
            <a class="pagination-item-link">...</a>
          </li>
          
          <!-- current page-->
          <li class="pagination-item active">
            <a class="pagination-item-link">[[pageNumber + 1]]</a>
          </li>
          <!-- current page-->
          
          <li class="pagination-item" data-ng-show="pageCount !== 1 && pageNumber + 1 !== (pageCount - 1) && pageNumber + 1 !== pageCount">
            <a class="pagination-item-link">...</a>
          </li>
          
           <!-- End page-->
          <li class="pagination-item" data-ng-click="endPage()" data-ng-show="pageCount !== 1 && pageNumber + 1 !== pageCount">
            <a class="pagination-item-link">[[pageCount]]</a>
          </li>
          
          <li class="pagination-item" data-ng-click="endPage()" data-ng-show="pageCount === 1 && pageNumber !== pageCount">
            <a class="pagination-item-link">2</a>
          </li>
           <!-- End page-->

          <li class="pagination-item arrow" data-ng-click="nextPage()" data-ng-show="pageCount > 0">
             <a class="pagination-item-link">❯</a>
          </li>
        </ul>
    `,
    link: function ($scope) {
        $scope.previousPage = function () {
            if ($scope.pageCount !== 1) {
                if ($scope.pageNumber !== 0) {
                    $scope.pageNumber--;
                }
            } else {
                $scope.pageNumber = 0;
            }

        };

        $scope.nextPage = function () {
            if ($scope.pageCount !== 1) {
                if ($scope.pageNumber < $scope.pageCount - 1) {
                    $scope.pageNumber++;
                }
            } else {
                $scope.pageNumber = 1;
            }
        };

        $scope.startPage = function () {
            $scope.pageNumber = 0;
        };

        $scope.endPage = function () {
            if ($scope.pageCount !== 1) {
                $scope.pageNumber = $scope.pageCount - 1;
            } else {
                $scope.pageNumber = $scope.pageCount;
            }
        };
    }
}));
