import {ng, idiom as lang} from 'entcore';

interface EventLegend {
    canShow: boolean;
    style: string;
    legendTitle: string;
    widget?: string;
}

interface IViewModel {
    absence: boolean,
    absenceNoReason: boolean,
    absenceNotRegularized: boolean,
    absenceRegularized: boolean,
    lateness: boolean,
    departure: boolean,
    remark: boolean,
    previouslyAbsent: boolean,
    forgottenNotebook: boolean,
    widgetForgottenNotebook: boolean,
    incident: boolean,

    eventsLegend: EventLegend[];

    getLegendTitle(legend: string): string;
}

export const EventsLegend = ng.directive('eventsLegend', () => {
    return {
        restrict: 'E',
        scope: {
            absence: '=',
            absenceNoReason: '=',
            absenceNotRegularized: '=',
            absenceRegularized: '=',
            lateness: '=',
            departure: '=',
            remark: '=',
            previouslyAbsent: '=',
            forgottenNotebook: '=',
            widgetForgottenNotebook: '=',
            incident: '=',
        },
        template: `
            <div class="event-legend">
                <ul class="history vertical-spacing-twice">
                    <li ng-repeat="legend in vm.eventsLegend" ng-if="legend.canShow" ng-class="legend.style">
                        <i ng-if="legend.widget" ng-class="legend.widget"></i>
                        [[vm.getLegendTitle(legend.legendTitle)]]
                    </li>
                </ul>
            </div>
        `,
        controllerAs: 'vm',
        bindToController: true,
        replace: true,
        controller: function () {
            const vm: IViewModel = <IViewModel>this;
            vm.eventsLegend = [
                {canShow: vm.absence, style: "absence-no-reason", legendTitle: "presences.register.event_type.absences"},
                {canShow: vm.absenceNoReason, style: "absence-no-reason", legendTitle: "presences.register.event_type.absences.without.reasons"},
                {canShow: vm.absenceNotRegularized, style: "absence-not-regularized", legendTitle: "presences.register.event_type.absences.not.regularized"},
                {canShow: vm.absenceRegularized, style: "absence-regularized", legendTitle: "presences.register.event_type.absences.regularized"},
                {canShow: vm.lateness, style: "lateness", legendTitle: "presences.register.event_type.lateness"},
                {canShow: vm.departure, style: "departure", legendTitle: "presences.register.event_type.departure"},
                {canShow: vm.remark, style: "remark", legendTitle: "presences.register.event_type.remark"},
                {canShow: vm.previouslyAbsent, style: "last-absent", legendTitle: "presences.register.event_type.last.absent", widget: "last-absent"},
                {canShow: vm.forgottenNotebook, style: "forgotten-notebook", legendTitle: "presences.register.event_type.forgotten.notebook"},
                {canShow: vm.widgetForgottenNotebook, style: "forgotten-notebook", legendTitle: "presences.register.event_type.forgotten.notebook", widget: "forgotten-notebook"},
                {canShow: vm.incident, style: "incident", legendTitle: "presences.register.event_type.incident"},
            ];

            vm.getLegendTitle = (legend: string): string => {
                return lang.translate(legend);
            };
        }
    };
});