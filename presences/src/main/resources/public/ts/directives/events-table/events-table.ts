import {ng, model, idiom} from 'entcore';
import {ILocationService, IScope, IWindowService} from "angular";
import {absenceService, EventService, reasonService} from "../../services";
import {Absence, ActionBody, Event, EventResponse, IAbsence, IEvent, Reason} from "../../models";
import {DateUtils, safeApply} from "@common/utils";
import {ROOTS} from "../../core/enum/roots";
import {REASON_TYPE_ID} from "@common/core/enum/reason-type-id";
import {EventsUtils} from "@presences/utilities";

declare let window: any;

interface IViewModel {
    events: Array<Event>;
    event: Event;
    dates: boolean;
    reason: boolean;
    owner: boolean;
    calendar: boolean;
    action: boolean;
    regularized: boolean;

    reasons: Array<Reason>;
    provingReasonsMap: Map<number, boolean>;
    loading: boolean;
    /* Filters and actions lightbox*/
    lightbox: {
        filter: boolean;
        action: boolean;
    };

    actionForm: ActionBody;
    actionEvent: Array<ActionBody>;

    translate(key: string): string;

    loadReasons(): Promise<void>;
    selectReason(absence: Absence, studentId: string): Promise<void>;
    reasonSelect($event: any): void;

    getAbsenceDateString(absence: Absence): string;

    redirectCalendar(absence: IAbsence): void;

    doAction($event: MouseEvent, event: any): Promise<void>;
    getEventActions(): Promise<void>;

    toggleEventRegularised(event: any): Promise<void>;
}

class Controller implements ng.IController, IViewModel {
    events: Array<Event>;
    event: Event;
    dates: boolean;
    reason: boolean;
    owner: boolean;
    calendar: boolean;
    action: boolean;
    regularized: boolean;
    reasons: Array<Reason>;
    provingReasonsMap: Map<number, boolean>;
    loading: boolean;
    /* Filters and actions lightbox*/
    lightbox: {
        filter: boolean;
        action: boolean;
    };

    actionForm: ActionBody;
    actionEvent: Array<ActionBody>;

    constructor(private $scope: IScope,
                private $location: ILocationService,
                private $window: IWindowService,
                private eventService: EventService) {}

    async $onInit(): Promise<void> {
        this.provingReasonsMap =  new Map<number, boolean>();
        this.lightbox = {
            filter: false,
            action: false
        };
        this.actionForm = {} as ActionBody;
        this.event = new Event(0, "", "", "");

        await this.loadReasons();
    }

    translate = (key: string): string => idiom.translate(key);

    async loadReasons(): Promise<void> {
        this.reasons = await reasonService.getReasons(window.structure.id);
        if (this.reasons) {
            this.reasons.forEach((reason: Reason) => {
                reason.isSelected = true;
                this.provingReasonsMap[reason.id] = reason.proving;
            });
        }
        safeApply(this.$scope);
    }

    reasonSelect = ($event): void => {
        $event.stopPropagation();
    }

    async selectReason(absence: Absence): Promise<void> {
        await absence.updateAbsenceReason([absence.id], absence.reason_id);
        safeApply(this.$scope);
    }

    /**
     * Redirect view to the selected absence/event student calendar
     * @param absence student absence (does not support Event type yet)
     */
    redirectCalendar(absence: IAbsence): void {
        const date: string = DateUtils.format(absence.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        window.item = {
            id: absence.student.id,
            date: date,
            displayName: absence.student.name,
            type: 'USER',
            groupName: absence.student.className,
            toString: (): string => {
                return absence.student.name;
            }
        };
        this.$location.path(`/calendar/${absence.student.id}?date=${date}`);
    }

    getAbsenceDateString(absence: Absence): string {
        let startDate: string = DateUtils.format(absence.start_date, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
        let endDate: string = DateUtils.format(absence.end_date, DateUtils.FORMAT["DAY-MONTH-YEAR"]);
        return (startDate !== endDate) ?
            `${this.translate(`presences.from`)} ${startDate} ${this.translate(`presences.to`)} ${endDate}` :
            `${this.translate(`presences.the`)} ${startDate}`;
    }

    async doAction($event: MouseEvent, event: any): Promise<void> {
        this.lightbox.action = true;
        this.event = event;
        this.actionForm.owner = model.me.userId;
        this.actionForm.eventId = [event.id];
        if (event.id) { // if action on sub-line
            this.actionForm.eventId.push(event.id);
        } else if (event.events) { // if action on global line
            event.events.forEach((event: IEvent) => {
                if (this.actionForm.eventId.indexOf(event.id) === -1) this.actionForm.eventId.push(event.id);
            });
        }
        this.actionForm.actionId = null;
        this.actionForm.comment = '';
        await this.getEventActions();
    }

    getEventActions = async (): Promise<void> => {
        this.actionEvent = await this.eventService.getEventActions(
            (this.actionForm.eventId && this.actionForm.eventId.length > 0) ? this.actionForm.eventId[0] : null);
        safeApply(this.$scope);
    }

    async toggleEventRegularised(event: any): Promise<void> {
        await absenceService.regularizeAbsences([event.id], event.counsellor_regularisation);
    }

}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${ROOTS.directive}events-table/events-table.html`,
        scope: {
            events: '=',
            students: '=',
            dates: '=',
            reason: '=',
            owner: '=',
            calendar: '=',
            action: '=',
            regularized: '='
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$location', '$window', 'EventService', Controller],
        link: function (scope: ng.IScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: ng.IController) {
        }
    };
}
export const EventsTable = ng.directive('eventsTable', directive);