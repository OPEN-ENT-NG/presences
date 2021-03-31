/**
 * Safe apply to use for sniplets and directives.
 */
export class SafeApplyUtils {

    static safeApply ($scope: any) {
        let phase = $scope.$root.$$phase;
        if (phase !== '$apply' && phase !== '$digest') {
            $scope.$apply();
        }
    }

}