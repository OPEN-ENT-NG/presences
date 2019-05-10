import {$, idiom as lang, ng} from 'entcore';

export const asyncAutocomplete = ng.directive('asyncAutocomplete', ['$timeout', ($timeout) => ({
    restrict: 'E',
    replace: true,
    scope: {
        ngModel: '=',
        ngDisabled: '=',
        ngChange: '&',
        onSearch: '=',
        search: '=',
        options: '='
    },
    template: `
    <div class="row async-autocomplete">
        <span class="input-async-autocomplete" ng-class="{loading: loading}">
            <input type="text" class="twelve cell" ng-disabled="disabled" ng-model="search" translate attr="placeholder" placeholder="search" autocomplete="off" />
        </span>
        <div data-drop-down class="drop-down" ng-class="{scroll: match.length > 0}">
            <div ng-if="match && match.length > 0">
                <ul class="ten cell right-magnet">
                    <li class="block-container" ng-repeat="option in match" ng-model="option" ng-click="select(option)">
                        <a class="cell right-spacing">
                            [[option.toString()]]
                        </a>
                    </li>
                </ul>
            </div>
            <div ng-if="match && match.length === 0">
                <ul class="ten cell right-magnet">
                    <li class="display-more block-container">
                        <a class="cell right-spacing">
                            [[translate('noresult')]]
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </div>
    `,
    link: function ($scope, $element) {
        let token, typingTimeout;
        const dropDownContainer = $element.find('[data-drop-down]');
        const linkedInput = $element.find('input');
        $scope.loading = false;
        $scope.disabled = false || $scope.ngDisabled;
        $scope.translate = lang.translate;
        $scope.search = $scope.search || "";

        const setLoadingStatus = (status: boolean = true) => {
            $scope.loading = status;
            $scope.$apply();
        };

        const endUserTyping = () => {
            setLoadingStatus();
            $scope.onSearch($scope.search, $scope.ngModel);
            cancelAnimationFrame(token);
        };

        $scope.$watch('options', (newVal) => {
            $scope.match = newVal;
            cancelAnimationFrame(token);
            setLoadingStatus(false);
            $scope.$apply();
        });

        $scope.select = (option) => $scope.$eval($scope.ngChange)($scope.ngModel, option);

        const closeDropDown = function (e: Event) {
            if ($element.find(e.target).length > 0 || dropDownContainer.find(e.target).length > 0) {
                return;
            }
            setLoadingStatus(false);
            $scope.match = undefined;
            $scope.$apply();
        };

        linkedInput.on('keyup', () => {
            clearTimeout(typingTimeout);
            typingTimeout = $scope.search.trim() !== '' ? setTimeout(endUserTyping, 750) : null;
        });

        linkedInput.on('keydown', () => {
            clearTimeout(typingTimeout);
            $scope.match = undefined;
            cancelAnimationFrame(token);
        });

        $scope.setDropDownHeight = function () {
            let liHeight = 0;
            const max = $scope.match.length;
            dropDownContainer.find('li').each(function (index, el) {
                liHeight += $(el).offsetHeight;

                return index < max;
            });
            dropDownContainer.height(liHeight);
        };

        $element.parent().on('remove', function () {
            cancelAnimationFrame(token);
            dropDownContainer.remove();
        });

        $scope.$on("$destroy", function () {
            cancelAnimationFrame(token);
            dropDownContainer.remove();
        });

        $('body').on('click', closeDropDown);

        $scope.$watch('search', function (newVal, oldVal) {
            if (!newVal) {
                $scope.options = undefined;
                dropDownContainer.height("");
                setLoadingStatus(false);
                return;
            } else {
                $scope.search = newVal;
                $scope.$apply();
            }
        });
    }
})]);