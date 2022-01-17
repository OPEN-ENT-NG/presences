/**
 * Safe apply to use for sniplets and directives.
 */
export function safeApply($scope: any) {
    let phase = $scope.$root.$$phase;
    if (phase !== '$apply' && phase !== '$digest') {
        $scope.$apply();
    }
}