import {_, angular, idiom as lang, Me, model, moment, ng} from 'entcore';
import {Action, ActionBody, Event, EventListCalendarFilter, EventResponse, Events, EventType,
    IEvent, IEventFormBody, IStructureSlot, Student, TimeSlotHourPeriod} from '../models';
import {DateUtils, PreferencesUtils, PresencesPreferenceUtils} from '@common/utils';
import {Group, GroupService} from '@common/services/GroupService';
import {actionService, EventRequest, EventService, ReasonService, ViescolaireService} from '../services';
import {EventsFilter, EventsFormFilter, EventsUtils} from '../utilities';
import {Reason} from '@presences/models/Reason';
import {INFINITE_SCROLL_EVENTER} from '@common/core/enum/infinite-scroll-eventer';
import {
    ABSENCE_FORM_EVENTS, EVENTS_DATE,
    EVENTS_FORM,
    EVENTS_SEARCH,
    LATENESS_FORM_EVENTS
} from '@common/core/enum/presences-event';
import {SNIPLET_FORM_EMIT_EVENTS} from '@common/model';
import {IEventSlot} from '@presences/models/Event';
import {EXPORT_TYPE, ExportType} from "@common/core/enum/export-type.enum";
import {REASON_TYPE_ID} from "@common/core/enum/reason-type-id";
import {IAngularEvent} from "angular";

declare let window: any;

interface ViewModel {
    filter: EventsFilter;
    formFilter: EventsFormFilter;
    reasonType: typeof REASON_TYPE_ID;

    /* Get reasons type */
    eventReasonsType: Array<Reason>;
    eventReasonsTypeDescription: Array<Reason>;

    /* Events */
    eventType: Array<number>;
    event: Event;
    interactedEvent: EventResponse;
    events: Events;
    multipleSelect: Reason;
    noReason: Reason;
    provingReasonsMap: any;
    isScroll: boolean;

    /* Filters and actions lightbox*/
    lightbox: {
        filter: boolean;
        action: boolean;
    };

    /* Get actions type */
    actionType: Array<Action>;
    action_abbreviation: string;
    actionEvent: Array<ActionBody>;
    actionForm: ActionBody;
    action_typeId: string;
    action: {
        seeAll: boolean;
    };

    /* Action drag parameters */
    actionDrag: {
        slot: IEventSlot;
        indexEvent: number;
        slotStartIndex: number;
        slotEndIndex: number;
        mouseHold: boolean;
    };

    structureTimeSlot: IStructureSlot;
    timeSlotHourPeriod: typeof TimeSlotHourPeriod;

    $onDestroy(): void;

    eventTypeState(periods: IEventSlot, index: number, indexSlot: number): string;

    editPeriod($event, event): Promise<void>;

    reasonSelect($event): void;

    getRegularizedValue(event): boolean;

    filterSelect(options: Array<Reason>, event): Array<Reason>;

    stopAbsencePropagation($event): void;

    regularizedChecked(event: EventResponse): boolean;

    changeAllReason(event: EventResponse, studentId: string, reasonType: REASON_TYPE_ID): Promise<void>;

    changeReason(history: Event, event: EventResponse, studentId: string, reasonType: REASON_TYPE_ID): Promise<void>;

    toggleAllEventsRegularised(event: EventResponse, studentId: string): Promise<void>;

    toggleEventRegularised(history: Event, event: EventResponse, studentId: string): Promise<void>;

    hasEventsAllRegularized(event: Array<Event>): boolean;

    hideGlobalCheckbox(event): boolean;

    formatMainDate(date: string): string;

    formatDate(date: string): string;

    /* tooltip */
    formatHourTooltip(date: string): string;

    findEvent(event: Array<Event>): Event;

    isEachEventAbsence(event: EventResponse): boolean;

    isEachEventLateness(event: EventResponse): boolean;

    /* Action */
    doAction($event: MouseEvent, event: any): void;

    getLastAction(): void;

    createAction(): Promise<void>;

    showHistory(): void;

    /* Open filter lightbox */
    openForm(): void;

    validForm(formFilter: EventsFormFilter): void;

    /* Open action lightbox */
    validActionForm(): void;

    /* Collapse event */
    eventId: number;
    collapse: boolean;

    toggleCollapse(event: BaseJQueryEventObject): void;

    openEventForm(event: MouseEvent, slot: IEventSlot, studentId: string): void;

    formatEventForm(slot: IEventSlot, studentId: string, typeId: number): IEventFormBody;

    preventCollapse($event: MouseEvent): void;

    isCollapsibleOpen($index: number): boolean;

    dragSlotStart($event: MouseEvent, slot: IEventSlot, studentId: string, index: number, indexSlot: number): void;

    dragSlotMove($event: MouseEvent, index: number, indexSlot: number): void;

    dragSlotEnd($event: MouseEvent, slot: IEventSlot, studentId: string, index: number): void;

    updateFilter(student?, audience?): void;

    updateDate(): void;

    onScroll(): void;

    getAbsenceReasons(reasonArray: Array<Reason>): Array<Reason>;

    getAbsenceReasonsWithMultipleSelection(reasonArray: Array<Reason>): Array<Reason>;

    getLatenessReasons(reasonArray: Array<Reason>): Array<Reason>;

    getLatenessReasonsWithMultipleSelection(reasonArray: Array<Reason>): Array<Reason>;

    getLatenessReasonsWithNoReason(reasonArray: Array<Reason>): Array<Reason>;

    /* Export*/
    exportType: typeof EXPORT_TYPE;
    export(exportType: ExportType): void;
}

export const eventListController = ng.controller('EventListController', ['$scope', '$route', '$location', '$timeout',
    'GroupService', 'ReasonService', 'EventService',
    function ($scope, $route, $location, $timeout, GroupService: GroupService,
              ReasonService: ReasonService, eventService: EventService) {
        const vm: ViewModel = this;
        vm.reasonType = REASON_TYPE_ID;
        vm.filter = {
            startDate: DateUtils.add(new Date(), -7, 'd'),
            endDate: moment().endOf('day').toDate(),
            timeslots: {
                start: {name: '', startHour: '', endHour: '', id: ''},
                end: {name: '', startHour: '', endHour: '', id: ''}
            },
            students: [],
            classes: [],
            absences: true,
            departure: true,
            late: true,
            regularized: true,
            allAbsenceReasons: true,
            allLatenessReasons: true,
            noReasons: true,
            noReasonsLateness: true,
            reasons: {} as Reason,
            reasonIds: [],
            notRegularized: true,
            followed: true,
            notFollowed: true,
            page: 0
        };
        vm.provingReasonsMap = {};
        vm.isScroll = false;
        vm.eventType = [];
        vm.multipleSelect = {
            id: -1,
            label: lang.translate('presences.absence.select.multiple'),
            structure_id: '',
            comment: '',
            default: false,
            proving: false,
            group: false
        } as Reason;
        vm.noReason = {
            id: 0,
            label: lang.translate('presences.absence.no.reason'),
            structure_id: '',
            comment: '',
            default: false,
            proving: false,
            group: false
        } as Reason;

        vm.event = new Event(0, "", "", "");
        vm.interactedEvent = {} as EventResponse;
        vm.events = new Events();
        vm.events.regularized = null;
        vm.events.eventer.on('loading::true', () => $scope.safeApply());
        vm.events.eventer.on('loading::false', () => {
            vm.eventId = null;
            $scope.safeApply();
        });
        vm.lightbox = {
            filter: false,
            action: false
        };
        vm.actionForm = {} as ActionBody;
        vm.action = {
            seeAll: false
        };

        vm.actionDrag = {
            mouseHold: false,
            slot: null,
            indexEvent: null,
            slotEndIndex: null,
            slotStartIndex: null
        };

        vm.structureTimeSlot = {} as IStructureSlot;
        vm.timeSlotHourPeriod = TimeSlotHourPeriod;
        vm.exportType = EXPORT_TYPE;

        let parentVm: any = $scope.$parent.$parent ? $scope.$parent.$parent.vm : null;
        if (parentVm && parentVm.groupsSearch && parentVm.studentsSearch
            && parentVm.groupsSearch.structureId && parentVm.studentsSearch.structureId) {
            vm.filter.classes = parentVm.groupsSearch.getSelectedGroups();
            vm.filter.students = parentVm.studentsSearch.getSelectedStudents();
        }

        const loadFormFilter = async (): Promise<void> => {
            let formFilters: any = await Me.preference(PresencesPreferenceUtils.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_FILTER);
            formFilters = formFilters ? formFilters[window.structure.id] : null;
            if (formFilters) {
                const {startDate, endDate} = vm.filter;
                vm.filter = {...vm.filter, ...formFilters, startDate, endDate};
                vm.eventReasonsType.forEach((r: Reason) => {
                    r.isSelected = vm.filter.reasonIds.find((reasonId: number) => reasonId === r.id) !== undefined;
                });
                vm.eventType = [];
            } else {
                vm.eventType = [];
                vm.filter = {
                    ...vm.filter, ...{
                        absences: true,
                        departure: true,
                        late: true,
                        allAbsenceReasons: true,
                        allLatenessReasons: true,
                        noReasons: true,
                        noReasonsLateness: true,
                        notRegularized: true,
                        regularized: false,
                        followed: true,
                        notFollowed: true,
                        reasonIds: []
                    }
                };
            }
        };

        const loadReasonTypes = async (): Promise<void> => {
            vm.eventReasonsType = await ReasonService.getReasons(window.structure.id, REASON_TYPE_ID.ALL);
            vm.eventReasonsTypeDescription = _.clone(vm.eventReasonsType);
            vm.eventReasonsType.map((reason: Reason) => {
                reason.isSelected = true;
                vm.provingReasonsMap[reason.id] = reason.proving;
            });
            vm.eventReasonsType.push(vm.multipleSelect);
            vm.eventReasonsType.push(vm.noReason);
        };

        const getEvents = async (actionMode?: boolean): Promise<void> => {
            vm.events.structureId = window.structure.id;
            vm.events.startDate = vm.filter.startDate.toDateString();
            vm.events.endDate = vm.filter.endDate.toDateString();
            vm.events.startTime = (vm.filter.timeslots && vm.filter.timeslots.start) ? vm.filter.timeslots.start.startHour : null;
            vm.events.endTime = (vm.filter.timeslots && vm.filter.timeslots.end) ? vm.filter.timeslots.end.endHour : null;
            vm.events.regularized = (vm.filter.noReasons && !vm.filter.regularized && !vm.filter.notRegularized) ||
            (vm.filter.regularized && vm.filter.notRegularized) ? null : vm.filter.regularized;
            vm.events.noReason = vm.filter.noReasons;
            if (vm.filter.absences && !vm.eventType.some((e: number) => e === EventType.ABSENCE)) {
                vm.eventType.push(EventType.ABSENCE);
            }
            if (vm.filter.late && !vm.eventType.some((e: number) => e === EventType.LATENESS)) {
                vm.eventType.push(EventType.LATENESS);
            }
            if (vm.filter.departure && !vm.eventType.some((e: number) => e === EventType.DEPARTURE)) {
                vm.eventType.push(EventType.DEPARTURE);
            }

            vm.events.eventType = vm.eventType.toString();

            EventsUtils.setStudentToSync(vm.events, vm.filter);
            EventsUtils.setClassToSync(vm.events, vm.filter);
            // actionMode to define if we display the loading icon mode while changing filter, date etc...
            if (!actionMode) {
                // "page" uses sync() method at the same time it sets 0 (See LoadingCollection Class)
                await updateListEventCalendarFilter();
                vm.updateFilter();
            } else {
                // dynamic mode : case if we only interact with action, reason, counsellor regularized...
                await refreshGetEventWhileAction();
            }
            $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            $scope.safeApply();
        };

        const updateListEventCalendarFilter = async (): Promise<void> => {
            const calendarFilterKey: string = PresencesPreferenceUtils.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_CALENDAR_FILTER;
            let calendarFilter: EventListCalendarFilter = Me.preferences[calendarFilterKey];
            if (calendarFilter && Object.keys(calendarFilter).length !== 0) {
                if (calendarFilter.startDate) {
                    vm.filter.startDate = calendarFilter.startDate;
                    vm.events.startDate = vm.filter.startDate.toDateString();
                }
                if (calendarFilter.endDate) {
                    vm.filter.endDate = calendarFilter.endDate;
                    vm.events.startDate = vm.filter.startDate.toDateString();
                }
                if (calendarFilter.students) vm.filter.students = calendarFilter.students;
                if (calendarFilter.classes) vm.filter.classes = calendarFilter.classes;
                await PreferencesUtils.resetPreference(calendarFilterKey);
            }
        };

        const getActions = async (): Promise<void> => {
            vm.actionType = await actionService.getActions(window.structure.id);
        };

        const getEventActions = async (): Promise<void> => {
            vm.actionEvent = await eventService.getEventActions(vm.actionForm.eventId[0]);
            $scope.safeApply();
        };

        const refreshGetEventWhileAction = async (): Promise<void> => {
            let filter: EventRequest = {
                structureId: vm.events.structureId,
                startDate: vm.events.startDate,
                endDate: vm.events.endDate,
                startTime: vm.events.startTime,
                endTime: vm.events.endTime,
                noReason: vm.events.noReason,
                noReasonLateness: vm.events.noReasonLateness,
                eventType: vm.events.eventType,
                // If neither absences nor lateness are selected, the list of reasons is empty
                listReasonIds: (vm.filter.regularized || vm.filter.notRegularized || vm.filter.late) ? vm.filter.reasonIds.toString() : '',
                userId: vm.events.userId,
                classes: vm.events.classes,
                page: vm.interactedEvent.page
            };
            filter.regularized = (!(<any> vm.eventType).includes(1)) ? null : vm.filter.regularized;
            filter.followed = (!(<any> vm.eventType).includes(1)) ? null : vm.filter.followed;
            filter.notFollowed = (!(<any> vm.eventType).includes(1)) ? null : vm.filter.notFollowed;
            let events = await eventService.get(filter);

            vm.events.pageCount = events.pageCount;
            vm.events.events = events.events;

            // replace events list by the event we fetched based on their page for each event
            vm.events.all = EventsUtils.interactiveConcat(vm.events.all, events.all, vm.interactedEvent.page);

            $scope.safeApply();
        };

        const getStructureTimeSlots = async (): Promise<void> => {
            vm.structureTimeSlot = await ViescolaireService.getSlotProfile(window.structure.id);
        };

        vm.$onDestroy = (): void => {
            $scope.$emit(EVENTS_DATE.EVENT_LIST_SAVE,
                {startDate: vm.filter.startDate, endDate: vm.filter.endDate});
        };

        vm.formatMainDate = (date: string): string => DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-YEAR"]);

        vm.formatDate = (date: string) => {
            return DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-HALFYEAR"]);
        };

        vm.formatHourTooltip = (date: string): string => {
            return DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);
        };

        vm.findEvent = (events: Array<Event>): Event => {
            return events.find(event => event.type === EventsUtils.ALL_EVENTS.event);
        };

        vm.createAction = async () => {
            await eventService.createAction(vm.actionForm);
            getEvents(true);
        };

        vm.editPeriod = async ($event, {student, date}): Promise<void> => {
            $event.stopPropagation();
            window.item = {
                id: student.id,
                date,
                displayName: student.displayName,
                type: 'USER',
                groupName: student.classeName,
                groupId: student.classId,
                toString: function (): string {
                    return this.displayName;
                }
            };
            let filter: EventListCalendarFilter = {
                startDate: vm.filter.startDate,
                endDate: vm.filter.endDate,
                students: vm.filter.students,
                classes: vm.filter.classes
            };
            PresencesPreferenceUtils.updatePresencesEventListCalendarFilter(filter);
            $location.path(`/calendar/${student.id}?date=${date}`);
            $scope.safeApply();
        };

        /* Change CSS class depending on their event_type id */
        vm.eventTypeState = (periods: IEventSlot, index: number, indexSlot: number): string => {
            const className: Array<string> = ['empty', 'remark', 'departure', 'late', 'absent', 'absent-no-regularized',
                'absent-regularized', 'event-absent', 'no-regularized', 'regularized', 'absent-followed', 'action-drag-event'];
            let indexes: Array<number> = [className.indexOf('empty')];

            // Check if drag on the event line. Coloring items between the first clicked on and the one below the pointer
            if (vm.actionDrag.mouseHold && (vm.actionDrag.indexEvent === index) &&
                (((indexSlot >= vm.actionDrag.slotStartIndex) && (indexSlot <= vm.actionDrag.slotEndIndex)) ||
                    ((indexSlot <= vm.actionDrag.slotStartIndex) && (indexSlot >= vm.actionDrag.slotEndIndex)))) {
                indexes.push(className.indexOf('action-drag-event'));
            } else if (periods.events.length === 0) {
                return '';
            } else {
                // We store every type of events in indexes and we prioritize the largest value (according to classNames)
                for (let i = 0; i < periods.events.length; i++) {
                    if ('type_id' in periods.events[i]) {
                        switch (periods.events[i].type_id) {
                            case (EventType.ABSENCE):
                                // If absence has a reason
                                if (periods.events[i].followed === true) {
                                    indexes.push(className.indexOf('absent-followed'));
                                }
                                if (periods.events[i].reason_id !== null && periods.events[i].reason_id !== -1) {
                                    (periods.events[i].counsellor_regularisation === true) ?
                                        indexes.push(className.indexOf('regularized')) :
                                        indexes.push(className.indexOf('no-regularized'));
                                } else {
                                    indexes.push(className.indexOf('event-absent'));
                                }
                                break;
                            case (EventType.LATENESS):
                                indexes.push(className.indexOf('late'));
                                break;
                            case (EventType.DEPARTURE):
                                indexes.push(className.indexOf('departure'));
                                break;
                            case (EventType.REMARK):
                                indexes.push(className.indexOf('remark'));
                                break;
                        }
                    } else if ('type' in periods.events[i]) {
                        if (periods.events[i].type === 'absence') {
                            if (periods.events[i].followed === true) {
                                indexes.push(className.indexOf('absent-followed'));
                            } else if (periods.events[i].reason_id !== null && periods.events[i].reason_id !== -1) {
                                (periods.events[i].counsellor_regularisation === true) ?
                                    indexes.push(className.indexOf('absent-regularized')) :
                                    indexes.push(className.indexOf('absent-no-regularized'));
                            } else {
                                indexes.push(className.indexOf('absent'));
                            }
                        }
                    }
                }
            }

            // get the largest value
            return className[Math.max(...indexes)] || '';
        };

        vm.reasonSelect = ($event): void => {
            $event.stopPropagation();
        };

        /* filtering by removing multiple choices if there is no reason_id */
        vm.filterSelect = (options: Array<Reason>, event): Array<Reason> => {
            let reasonIds: Array<number> = EventsUtils.getReasonIds(event.events);
            if (reasonIds.every((val: number, i: number, arr: Array<number>) => val === arr[0])) {
                return options.filter(option => option.id !== 0);
            }
            return options;
        };

        vm.doAction = ($event: MouseEvent, event: any): void => {
            $event.stopPropagation();
            vm.lightbox.action = true;
            vm.interactedEvent = event;
            vm.event = event;
            vm.actionForm.owner = model.me.userId;
            vm.actionForm.eventId = [];
            if (event.id) { // if action on sub-line
                vm.actionForm.eventId.push(event.id);
            } else if (event.events) { // if action on global line
                event.events.forEach((event: IEvent) => {
                    if (vm.actionForm.eventId.indexOf(event.id) === -1) vm.actionForm.eventId.push(event.id);
                });
            }
            vm.actionForm.actionId = null;
            vm.actionForm.comment = '';
            getEventActions();
        };

        vm.showHistory = (): void => {
            vm.action.seeAll = !vm.action.seeAll;
        };

        vm.stopAbsencePropagation = ($event): void => {
            $event.stopPropagation();
        };

        vm.regularizedChecked = (event: EventResponse): boolean => {
            let regularized: Array<boolean> = [];
            event.events.forEach((elem: IEvent) => {
                regularized.push(elem.reason_id && elem.counsellor_regularisation &&
                    (elem.type === EventsUtils.ALL_EVENTS.absence || elem.type_id === 1));
            });
            return !event.counsellor_regularisation && regularized.some((r: boolean) => r === true);
        };

        vm.isEachEventAbsence = (event: EventResponse): boolean => {
            return event.events && EventsUtils.hasTypeEventAbsence(event.events);
        };

        vm.isEachEventLateness = (event: EventResponse): boolean => {
            return event.events && EventsUtils.hasTypeEventLateness(event.events);
        };

        /* Add global reason_id to all events that exist */
        vm.changeAllReason = async (event: EventResponse, studentId: string, reasonType: REASON_TYPE_ID): Promise<void> => {
            let initialReasonId: number = event.reason.id;
            vm.interactedEvent = event;
            let fetchedEvent: Event | Array<EventResponse> = [];
            EventsUtils.fetchEvents(event, fetchedEvent, reasonType);

            await vm.events.updateReason(fetchedEvent, initialReasonId, studentId, window.structure.id)
                .then(() => {
                    getEvents(true);
                    if (vm.filter.regularized) {
                        vm.eventId = null;
                    }
                });
            $scope.safeApply();
        };

        /* Change its description reason id */
        vm.changeReason = async (history: Event, event: EventResponse, studentId: string, reasonType: REASON_TYPE_ID): Promise<void> => {
            let initialReasonId: number = history.reason ? history.reason.id : history.reason_id;
            vm.interactedEvent = event;
            let fetchedEvent: Array<Event | EventResponse> = [];
            history.counsellor_regularisation = vm.provingReasonsMap[history.reason_id];
            fetchedEvent.push(history);
            if (reasonType === REASON_TYPE_ID.ABSENCE || (reasonType === REASON_TYPE_ID.LATENESS && !vm.isEachEventAbsence(event))) {
                event.reason.id = EventsUtils.initGlobalReason(event, reasonType);
            }
            await vm.events.updateReason(fetchedEvent, initialReasonId, studentId, window.structure.id)
                .then(() => {

                    if (EventsUtils.isEachEventsCounsellorRegularized(event.events) &&
                        EventsUtils.hasSameEventsReason(event.events)) {
                        if (!vm.filter.regularized) {
                            getEvents(true);
                            vm.eventId = null;
                        }
                    }
                });
            $scope.safeApply();
        };

        vm.toggleAllEventsRegularised = async (event: EventResponse, studentId: string): Promise<void> => {
            let initialCounsellorRegularisation: boolean = event.counsellor_regularisation;
            vm.interactedEvent = event;
            let fetchedEvent: IEvent | Array<EventResponse> = [];
            EventsUtils.fetchEvents(event, fetchedEvent, REASON_TYPE_ID.ABSENCE);
            vm.events.updateRegularized(fetchedEvent, initialCounsellorRegularisation, studentId, window.structure.id)
                .then(async () => {
                    $timeout(async () => {
                        // we use $timeout trick since we figured that updateRegularized was too fast to handle our data for our getEvent
                        // some data might not be updated at this time then we use timeout
                        await getEvents(true);
                        vm.eventId = null;
                        $scope.safeApply();
                    }, 500);
                });
            $scope.safeApply();
        };

        vm.toggleEventRegularised = async (history: Event, event: EventResponse, studentId: string): Promise<void> => {
            let initialCounsellorRegularisation: boolean = history.counsellor_regularisation;
            vm.interactedEvent = event;
            let fetchedEvent: Array<Event | EventResponse> = [];
            if (history.type === EventsUtils.ALL_EVENTS.event) {
                fetchedEvent.push(history);
            }
            event.counsellor_regularisation = EventsUtils.initGlobalCounsellor(event);
            await vm.events.updateRegularized(fetchedEvent, initialCounsellorRegularisation, studentId, window.structure.id)
                .then(() => {
                    if (EventsUtils.hasSameEventsCounsellor(event.events)) {
                        getEvents(true);
                        if (!vm.filter.regularized) {
                            vm.eventId = null;
                        }
                    }
                });
            $scope.safeApply();
        };

        vm.toggleCollapse = (event: BaseJQueryEventObject): void => {
            if (vm.actionDrag.mouseHold) return;
            let data: string = event.currentTarget.getAttribute('data-id');
            if ((vm.eventId !== null) && (vm.eventId.toString() === data)) {
                vm.collapse = !vm.collapse;
                vm.eventId = vm.collapse ? +data : null;
            } else {
                vm.collapse = true;
                vm.eventId = +data;
            }
        };

        vm.openEventForm = ($event: MouseEvent, slot: IEventSlot, studentId: string): void => {
            if (vm.actionDrag.mouseHold) return;

            if (slot.events.length > 0) {
                if (slot.events[0].type === EventsUtils.ALL_EVENTS.absence) {
                    $scope.$broadcast(ABSENCE_FORM_EVENTS.EDIT_EVENT, vm.formatEventForm(slot, studentId, EventType.ABSENCE));
                } else {
                    switch (slot.events[0].type_id) {
                        case EventType.ABSENCE:
                            $scope.$broadcast(ABSENCE_FORM_EVENTS.EDIT_EVENT, vm.formatEventForm(slot, studentId, EventType.ABSENCE));
                            break;
                        case EventType.LATENESS:
                            $scope.$broadcast(LATENESS_FORM_EVENTS.EDIT, vm.formatEventForm(slot, studentId, EventType.LATENESS));
                            break;
                    }
                }
            } else {
                $scope.$broadcast(ABSENCE_FORM_EVENTS.OPEN, vm.formatEventForm(slot, studentId, EventType.ABSENCE));
            }
        };

        vm.formatEventForm = (slot: IEventSlot, studentId: string, typeId: number): IEventFormBody => {
            switch (typeId) {
                case EventType.ABSENCE:
                case EventType.LATENESS:
                    let startDate: Date = moment(slot.start).toDate();
                    let endDate: Date = moment(slot.end).toDate();
                    let counsellor_regularisation: boolean = false;
                    let followed: boolean = false;

                    if (slot.events && slot.events.length === 1) { // only absence event
                        startDate = moment(slot.events[0].start_date).toDate();
                        endDate = moment(slot.events[0].end_date).toDate();
                        counsellor_regularisation = slot.events[0].counsellor_regularisation;
                        followed = !!slot.events.find((e: IEvent) => e.followed); // find at least one followed true at same time slot
                    } else if (slot.events && slot.events.length > 1) { // with absence
                        startDate = moment(slot.events[slot.events.length - 1].start_date).toDate();
                        endDate = moment(slot.events[slot.events.length - 1].end_date).toDate();
                        counsellor_regularisation = slot.events[0].counsellor_regularisation;
                        followed = !!slot.events.find((e: IEvent) => e.followed); // find at least one followed true at same time slot
                    }
                    return {
                        id: (slot.events && slot.events.length > 0) ? slot.events[0].id : null,
                        startDate: (startDate < endDate) ? startDate : endDate,
                        endDate: (startDate < endDate) ? endDate : startDate,
                        startTime: (startDate < endDate) ? startDate : endDate,
                        endTime: (startDate < endDate) ? endDate : startDate,
                        comment: (slot.events && slot.events.length > 0) ? slot.events[0].comment : null,
                        studentId: studentId,
                        eventType: EventsUtils.ALL_EVENTS.event,
                        counsellor_regularisation: counsellor_regularisation,
                        followed: followed,
                        absences: slot.events
                    };
            }
        };

        /* As we drag the nodes, preventing our event to collapse */
        vm.preventCollapse = ($event: MouseEvent): void => {
            $event.stopPropagation();
        };

        /* Open the concerned event */
        vm.isCollapsibleOpen = ($index: number): boolean => {
            return $index === vm.eventId;
        };

        vm.dragSlotStart = ($event: MouseEvent, slot: IEventSlot, studentId: string, index: number, indexSlot: number): void => {
            if (vm.actionDrag.mouseHold) return;
            vm.actionDrag.mouseHold = (indexSlot !== vm.actionDrag.slotStartIndex);
            vm.actionDrag.slot = {start: slot.start, end: slot.end};
            vm.actionDrag.slotStartIndex = indexSlot;
            vm.actionDrag.slotEndIndex = indexSlot;
            vm.actionDrag.indexEvent = index;
        };

        vm.dragSlotMove = ($event: MouseEvent, index: number, indexSlot: number): void => {
            if (vm.actionDrag.mouseHold && index === vm.actionDrag.indexEvent) {
                vm.actionDrag.slotEndIndex = indexSlot;
            }
        };

        vm.dragSlotEnd = ($event: MouseEvent, slot: IEventSlot, studentId: string, index: number): void => {
            vm.actionDrag.mouseHold = false;
            vm.actionDrag.slotStartIndex = null;
            vm.actionDrag.slotEndIndex = null;

            if (index === vm.actionDrag.indexEvent &&
                (((slot.events.length > 0) && (slot.start !== vm.actionDrag.slot.start)) || (slot.events.length === 0))) {
                vm.actionDrag.slot.end = slot.end;
                $scope.$broadcast(ABSENCE_FORM_EVENTS.OPEN, vm.formatEventForm(vm.actionDrag.slot, studentId, EventType.ABSENCE));
                vm.actionDrag.slot = {};
                $scope.safeApply();
            }
        };

        /* ----------------------------
          update filter methods
        ---------------------------- */
        vm.updateFilter = (student?, audience?) => {
            if (audience && !_.find(vm.filter.classes, audience)) {
                vm.filter.classes.push(audience);
            }
            if (student && !_.find(vm.filter.students, student)) {
                vm.filter.students.push(student);
            }
            /* Manage state regularized */
            vm.filter.regularized = (vm.filter.noReasons && !vm.filter.regularized && !vm.filter.notRegularized) ||
            (vm.filter.regularized && vm.filter.notRegularized) ? null : vm.filter.regularized;
            if (vm.filter.absences) {
                if (!vm.eventType.some(e => e === EventType.ABSENCE)) {
                    vm.eventType.push(EventType.ABSENCE);
                }
            } else {
                vm.eventType = vm.eventType.filter((e: number) => e !== EventType.ABSENCE);
            }
            if (vm.filter.late) {
                if (!vm.eventType.some(e => e === EventType.LATENESS)) {
                    vm.eventType.push(EventType.LATENESS);
                }
            } else {
                vm.eventType = vm.eventType.filter((e: number) => e !== EventType.LATENESS);
            }
            if (vm.filter.departure) {
                if (!vm.eventType.some(e => e === EventType.DEPARTURE)) {
                    vm.eventType.push(EventType.DEPARTURE);
                }
            } else {
                vm.eventType = vm.eventType.filter((e: number) => e !== EventType.DEPARTURE);
            }
            vm.events.eventType = vm.eventType.toString();

            // If neither absences nor lateness are selected, the list of reasons is empty
            vm.events.listReasonIds = (vm.filter.regularized || vm.filter.notRegularized
                || vm.filter.late) ? vm.filter.reasonIds.toString() : "";
            vm.events.noReason = vm.filter.noReasons;
            vm.events.noReasonLateness = vm.filter.noReasonsLateness;
            vm.events.followed = vm.filter.followed;
            vm.events.notFollowed = vm.filter.notFollowed;
            vm.events.regularized = (!(<any> vm.eventType).includes(1)) ? null : vm.filter.regularized;
            vm.events.startDate = vm.filter.startDate.toDateString();
            vm.events.endDate = vm.filter.endDate.toDateString();
            vm.events.startTime = (vm.filter.timeslots && vm.filter.timeslots.start) ? vm.filter.timeslots.start.startHour : null;
            vm.events.endTime = (vm.filter.timeslots && vm.filter.timeslots.end) ? vm.filter.timeslots.end.endHour : null;

            EventsUtils.setStudentToSync(vm.events, vm.filter);
            EventsUtils.setClassToSync(vm.events, vm.filter);
            vm.events.page = 0;
            vm.filter.page = vm.events.page;
            $scope.$broadcast(INFINITE_SCROLL_EVENTER.UPDATE);
            $scope.safeApply();
        };

        vm.updateDate = async (): Promise<void> => {
            if (vm.filter.startDate && vm.filter.endDate &&
                ((vm.events.startDate !== vm.filter.startDate.toDateString()) ||
                    (vm.events.endDate !== vm.filter.endDate.toDateString()))) {
                getEvents();
                $scope.safeApply();
            }
        };

        vm.onScroll = (): void => {
            vm.filter.page++;
            let filter: EventRequest = {
                structureId: vm.events.structureId,
                startDate: vm.events.startDate,
                endDate: vm.events.endDate,
                startTime: vm.events.startTime,
                endTime: vm.events.endTime,
                noReason: vm.events.noReason,
                eventType: vm.events.eventType,
                // If neither absences nor lateness are selected, the list of reasons is empty
                listReasonIds: (vm.filter.regularized || vm.filter.notRegularized || vm.filter.late) ? vm.filter.reasonIds.toString() : "",
                followed: vm.events.followed,
                notFollowed: vm.events.notFollowed,
                userId: vm.events.userId,
                classes: vm.events.classes,
                page: vm.filter.page
            };
            filter.regularized = (!(<any> vm.eventType).includes(1)) ? null : vm.filter.regularized;
            eventService
                .get(filter)
                .then((events: { pageCount: number, events: Array<EventResponse>, all: Array<EventResponse> }) => {
                    if (events.all.length !== 0) {
                        vm.events.pageCount = events.pageCount;
                        vm.events.events = events.events;
                        vm.events.all = vm.events.all.concat(events.all);
                    }
                    $scope.safeApply();
                });
        };

        vm.export = (exportType: ExportType): void => {
            const filter: EventRequest = {
                structureId: vm.events.structureId,
                startDate: vm.events.startDate,
                endDate: vm.events.endDate,
                startTime: vm.events.startTime,
                endTime: vm.events.endTime,
                noReason: vm.events.noReason,
                eventType: vm.events.eventType,
                listReasonIds: vm.events.listReasonIds,
                regularized: vm.events.regularized,
                userId: vm.events.userId,
                classes: vm.events.classes,
            };
            window.open(eventService.export(filter, exportType));
        };

        vm.getAbsenceReasons = (reasonArray: Array<Reason>): Array<Reason> => {
            if (!reasonArray) {
                return [];
            }
            return reasonArray.filter(reason => reason.reason_type_id === REASON_TYPE_ID.ABSENCE);
        };

        vm.getAbsenceReasonsWithMultipleSelection = (reasonArray: Array<Reason>): Array<Reason> => {
            if (!reasonArray) {
                return [];
            }
            return [vm.multipleSelect].concat(vm.getAbsenceReasons(reasonArray));
        };

        vm.getLatenessReasons = (reasonArray: Array<Reason>): Array<Reason> => {
            if (!reasonArray) {
                return [];
            }
            return reasonArray.filter(reason => reason.reason_type_id === REASON_TYPE_ID.LATENESS);
        };

        vm.getLatenessReasonsWithMultipleSelection = (reasonArray: Array<Reason>): Array<Reason> => {
            if (!reasonArray) {
                return [];
            }
            return [vm.multipleSelect].concat(vm.getLatenessReasons(reasonArray));
        };

        vm.getLatenessReasonsWithNoReason = (reasonArray: Array<Reason>): Array<Reason> => {
            if (!reasonArray) {
                return [];
            }
            return [vm.noReason].concat(vm.getLatenessReasons(reasonArray));
        };

        vm.hideGlobalCheckbox = (event: EventResponse): boolean => {
            return event.reason === null || event.reason.id === null || vm.provingReasonsMap[event.reason.id];
        };

        vm.validForm = async (formFilter: EventsFormFilter) => {
            let form: EventsFormFilter = {
                absences: formFilter.absences,
                late: formFilter.late,
                departure: formFilter.departure,
                noReasons: formFilter.noReasons,
                noReasonsLateness: formFilter.noReasonsLateness,
                notRegularized: formFilter.notRegularized,
                regularized: formFilter.regularized,
                followed: formFilter.followed,
                notFollowed: formFilter.notFollowed,
                allAbsenceReasons: formFilter.allAbsenceReasons,
                allLatenessReasons: formFilter.allLatenessReasons,
                timeslots: formFilter.timeslots,
                reasonIds: formFilter.reasonIds,
                students: formFilter.students,
                classes: formFilter.classes,
            };
            const {startDate, endDate} = vm.filter;
            vm.filter = {...form, startDate, endDate};
            vm.formFilter = {};
            vm.updateFilter();
            vm.lightbox.filter = false;
        };

        /* Form action */
        vm.validActionForm = (): void => {
            vm.lightbox.action = false;
            vm.createAction();
        };

        /* ----------------------------
                Handler events
         ---------------------------- */

        /* on (watch) */
        $scope.$watch(() => window.structure, async () => {
            if (window.structure) {
                $scope.$emit(EVENTS_DATE.EVENT_LIST_REQUEST);
                await loadReasonTypes().then(async () => {
                    await loadFormFilter();
                });
                await Promise.all([
                    getEvents(),
                    getActions(),
                    getStructureTimeSlots()
                ]);
            }
        });

        /* Destroy directive and scope */
        $scope.$on("$destroy", () => {
            /* Remove directive/ghost div that remains on the view before changing route */
            if (angular !== undefined) {
                angular.element(document.querySelectorAll(".datepicker")).remove();
                angular.element(document.querySelectorAll(".tooltip")).remove();
            }
        });


        $scope.$on(EVENTS_DATE.EVENT_LIST_SEND, (evt: IAngularEvent, dates: {startDate: Date, endDate: Date}) => {
            if (dates.startDate !== null && dates.endDate !== null) {
                vm.filter.startDate = dates.startDate;
                vm.filter.endDate = dates.endDate;
            }
        });

        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.FILTER, async () => await getEvents());
        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.EDIT, async () => await getEvents());
        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.DELETE, async () => await getEvents());
        $scope.$on(EVENTS_SEARCH.STUDENT, (event: IAngularEvent, students: Array<Student>) => {
            this.filter.students = students;
            vm.updateFilter();
        });
        $scope.$on(EVENTS_SEARCH.GROUP, (event: IAngularEvent, groups: Array<Group>) => {
            this.filter.classes = groups;
            vm.updateFilter();
        });

        $scope.$on(EVENTS_FORM.SUBMIT, async (event: IAngularEvent, filter: EventsFilter) => {
            vm.validForm(filter);
        });
    }]);