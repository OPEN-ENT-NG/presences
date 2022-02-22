import {idiom as lang, model, ng} from 'entcore';
import rights from "../../ts/rights";

interface EventLegend {
    canShow: boolean;
    style: string;
    legendTitle: string;
    widget?: string;
}

interface IViewModel {
    $onInit(): any;

    $onDestroy(): any;

    absence: boolean,
    absenceNoReason: boolean,
    absenceNotRegularized: boolean,
    absenceRegularized: boolean,
    absenceFollowed: boolean,
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
            absenceFollowed: '=',
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

            vm.$onInit = () => {
                vm.eventsLegend = [
                {canShow: vm.absence, style: "absence-no-reason", legendTitle: "presences.register.event_type.absences"},
                {canShow: vm.absenceNoReason, style: "absence-no-reason", legendTitle: "presences.register.event_type.abs.without.reasons"},
                {canShow: vm.absenceNotRegularized, style: "absence-not-regularized", legendTitle: "presences.register.event_type.abs.not.regularized"},
                {canShow: vm.absenceRegularized, style: "absence-regularized", legendTitle: "presences.register.event_type.abs.regularized"},
                {canShow: vm.absenceFollowed, style: "absence-followed", legendTitle: "presences.register.event_type.abs.followed"},
                {canShow: vm.lateness, style: "lateness", legendTitle: "presences.register.event_type.lateness"},
                {canShow: vm.departure, style: "departure", legendTitle: "presences.register.event_type.departure"},
                {canShow: vm.remark, style: "remark", legendTitle: "presences.register.event_type.remark"},
                {canShow: vm.previouslyAbsent, style: "last-absent", legendTitle: "presences.register.event_type.last.absent", widget: "last-absent"},
                {canShow: vm.forgottenNotebook, style: "forgotten-notebook", legendTitle: "presences.register.event_type.forgotten.notebook"},
                {canShow: (vm.widgetForgottenNotebook && model.me.hasWorkflow(rights.workflow.manageForgottenNotebook)),
                    style: "forgotten-notebook", legendTitle: "presences.register.event_type.forgotten.notebook", widget: "forgotten-notebook"},
                {canShow: vm.incident, style: "incident", legendTitle: "presences.register.event_type.incident"},
            ];

            };

            vm.getLegendTitle = (legend: string): string => {
                return lang.translate(legend);
            };
        }
    };
});