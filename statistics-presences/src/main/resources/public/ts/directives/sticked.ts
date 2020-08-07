import {ng, ui} from "entcore";

export const sticker = ng.directive('sticked', function () {
    return {
        restrict: 'EA',
        link: function (scope, element, attributes) {
            if ("noStickMobile" in attributes && ui.breakpoints.checkMaxWidth("wideScreen"))
                return;
            var initialPosition = null;
            var scrollTop = $(window).scrollTop()
            var actualScrollTop = $(window).scrollTop()

            var animation = function () {
                element.addClass('scrolling')
                element.offset({
                    top: element.offset().top + (actualScrollTop + $('.height-marker').height() - (element.offset().top))
                });
                requestAnimationFrame(animation)
            }

            var scrolls = false;
            $(window).scroll(function () {
                if (!initialPosition)
                    initialPosition = element.offset().top;
                actualScrollTop = $(window).scrollTop()
                if (actualScrollTop <= initialPosition - $('.height-marker').height()) {
                    actualScrollTop = initialPosition - $('.height-marker').height();
                }
                if (!scrolls) {
                    animation();
                }
                scrolls = true;
            })

        }
    }
});