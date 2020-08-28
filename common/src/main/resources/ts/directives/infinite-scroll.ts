import {ng} from 'entcore';
import {INFINITE_SCROLL_EVENTER} from "@common/core/enum/infinite-scroll-eventer";

/**
 * Nav sub menu component
 */
export const InfiniteScroll = ng.directive('infiniteScroll', () => {
    return {
        restrict: 'E',
        scope: {
            scrolled: '&'
        },
        link: function ($scope, $element: HTMLDivElement) {
            let currentscrollHeight: number = 0;
            // latest height once scroll will reach
            const latestHeightBottom: number = 300;

            $(window).on("scroll", () => {
                const scrollHeight: number = $(document).height() as number;
                const scrollPos: number = Math.floor($(window).height() + $(window).scrollTop());
                const isBottom: boolean = scrollHeight - latestHeightBottom < scrollPos;

                if (isBottom && currentscrollHeight < scrollHeight) {
                    $scope.$apply($scope.scrolled());

                    // Storing the latest scroll that has been the longest one in order to not redo the scrolled() each time
                    currentscrollHeight = scrollHeight;
                }
            });

            // If somewhere in your controller you have to reinitialise anything that should "reset" your dom height
            // We reset currentscrollHeight
            $scope.$on(INFINITE_SCROLL_EVENTER.UPDATE, () => currentscrollHeight = 0);
        }
    }
});