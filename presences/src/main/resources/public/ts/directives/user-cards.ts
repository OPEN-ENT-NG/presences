import {$, _, ng} from 'entcore';

export const UserCard = ng.directive('userCards', () => {
    return {
        restrict: 'E',
        scope: {
            users: '=',
            ngShow: '=',
            ngModel: '=',
            ngChange: '&?'
        },
        template: `
        <div class="user-cards"
                 ng-show="ngShow">
           <div class="user-card">
                <div class="cell avatar"
                     ng-style="{'background-image': 'url(/userbook/avatar/' + ngModel.id + ')'}">
                    &nbsp;
                </div>
                <div class="cell">
                    <h2>
                        [[ngModel.displayName]] 
                        <i ng-if="users.length > 1" ng-click="display.users = !display.users" class="arrow bottom user-card-arrow-button"></i>
                     </h2>
                    <div class="functions"><em class="function metadata"
                                               ng-repeat="function in ngModel.functions"
                                               ng-bind="function"></em></div>
                </div>
           </div>
           <div class="users" ng-class="{'displayed': display.users}">
               <div class="user-card"
                     ng-click="select(user)"
                     ng-repeat="user in list track by user.id">
                    <div class="cell avatar"
                         ng-style="{'background-image': 'url(/userbook/avatar/' + user.id + ')'}">
                        &nbsp;
                    </div>
                    <div class="cell">
                        <h2>[[user.displayName]]</h2>
                        <div class="functions"><em class="function metadata"
                                                   ng-repeat="function in user.functions"
                                                   ng-bind="function"></em></div>
                    </div>
               </div>
           </div>
        </div>
        `,
        controller: function ($scope) {
            $scope.display = {users: false};
            const users = _.clone($scope.users);
            $scope.ngModel = $scope.ngModel || users[0];

            const setList = () => {
                $scope.list = _.filter(users, (user) => user.id !== $scope.ngModel.id);
                $scope.display.users = false;
            };

            $scope.select = function (user) {
                $scope.ngModel = user;
                if ($scope.$eval($scope.ngChange)) $scope.$eval($scope.ngChange)(user);
                setList();
                $scope.$apply();
            };

            $scope.$watch('ngModel', () => $scope.$apply());

            setList();

            $("body").click((evt: Event) => {
                console.log(evt);
                if (!(evt.target as Element).className.includes('user-card-arrow-button')) {
                    $scope.display.users = false;
                    $scope.$apply();
                }
            })
        }
    };
});