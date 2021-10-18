import {_, angular, idiom as lang, Me, model, moment, ng} from 'entcore';
import {
    Action,
    ActionBody,
    Event,
    EventListCalendarFilter,
    EventResponse,
    Events,
    EventType,
    IEvent,
    IEventFormBody, IStructureSlot, ITimeSlot,
    Student,
    Students,
    TimeSlotHourPeriod
} from '../models';
import {DateUtils, PreferencesUtils, PresencesPreferenceUtils} from '@common/utils';
import {GroupService} from '@common/services/GroupService';
import {actionService, EventRequest, EventService, ReasonService, ViescolaireService} from '../services';
import {EventsFilter, EventsFormFilter, EventsUtils} from '../utilities';
import {Reason} from '@presences/models/Reason';
import {INFINITE_SCROLL_EVENTER} from '@common/core/enum/infinite-scroll-eventer';
import {ABSENCE_FORM_EVENTS, LATENESS_FORM_EVENTS} from '@common/core/enum/presences-event';
import {SNIPLET_FORM_EMIT_EVENTS} from '@common/model';
import {IEventSlot} from '@presences/models/Event';

declare let window: any;

interface ViewModel {
    filter: EventsFilter;
    formFilter: EventsFormFilter;

    /* Get reasons type */
    eventReasonsType: Reason[];
    eventReasonsTypeDescription: Reason[];
    eventReasonsId: number[];

    /* Events */
    eventType: number[];
    event: Event;
    interactedEvent: EventResponse;
    events: Events;
    multipleSelect: Reason;
    provingReasonsMap: any;
    isScroll: boolean;

    /* Filters and actions lightbox*/
    lightbox: {
        filter: boolean;
        action: boolean;
    };

    /* Get actions type */
    actionType: Action[];
    action_abbreviation: string;
    actionEvent: ActionBody[];
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

    eventTypeState(periods: IEventSlot, index: number, indexSlot: number): string;

    editPeriod($event, event): Promise<void>;

    reasonSelect($event): void;

    getRegularizedValue(event): boolean;

    filterSelect(options: Reason[], event): Reason[];

    updateFilterSlot(hourPeriod: TimeSlotHourPeriod): void;

    downloadFile($event): void;

    stopAbsencePropagation($event): void;

    regularizedChecked(event: EventResponse): boolean;

    changeAllReason(event: EventResponse, studentId: string): Promise<void>;

    changeReason(history: Event, event: EventResponse, studentId: string): Promise<void>;

    toggleAllEventsRegularised(event: EventResponse, studentId: string): Promise<void>;

    toggleEventRegularised(history: Event, event: EventResponse, studentId: string): Promise<void>;

    hasEventsAllRegularized(event: Event[]): boolean;

    hideGlobalCheckbox(event): boolean;

    formatMainDate(date: string): string;

    formatDate(date: string): string;

    /* tooltip */
    formatHourTooltip(date: string): string;

    findEvent(event: Array<Event>): Event;

    isEachEventAbsence(event: EventResponse): boolean;

    /* Action */
    doAction($event: MouseEvent, event: any): void;

    getLastAction(): void;

    createAction(): Promise<void>;

    showHistory(): void;

    /* Open filter lightbox */
    openForm(): void;

    validForm(): void;

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

    /* Students */
    studentSearchInput: string;
    students: Students;

    /* Students lightbox */
    studentSearchInputLightbox: string;
    studentsLightbox: Students;

    searchByStudent(string: string): void;

    searchByStudentFromLightbox(string: string): void;

    selectStudent(model: Student, option: Student): void;

    selectStudentFromLightbox(value: string, student: Student): void;

    selectStudentFromDashboard(model: Student, option: Student): void;

    excludeStudentFromFilter(audience): void;

    excludeStudentFromFilterLightbox(audience): void;

    /* Classes */
    classesSearchInput: string;
    classes: any;
    classesFiltered: any[];

    /* Classes Lightbox */
    classesSearchInputLightbox: string;
    classesFilteredLightbox: any[];

    searchByClass(value: string): Promise<void>;

    searchByClassFromLightbox(value: string): Promise<void>;

    selectClass(model: any, option: any): void;

    selectClassFromLightbox(value: string, classes: any): void;

    excludeClassFromFilter(audience): void;

    excludeClassFromFilterLightbox(audience): void;

    /* update filter */
    updateFilter(student?, audience?): void;

    updateDate(): void;

    onScroll(): void;

    /*  switch event type */
    switchAbsencesFilter(): void;

    switchLateFilter(): void;

    switchDepartureFilter(): void;

    switchFollowedFilter(): void;

    switchNotFollowedFilter(): void;

    /*  switch reasons */
    switchReason(reason: Reason): void;

    switchAllReasons(): void;

    /*  switch state */
    switchUnjustifiedFilter(): void;

    switchjustifiedNotRegularizedFilter(): void;

    switchjustifiedRegularizedFilter(): void;

    adaptEvent(): void;

    adaptReason(): void;

    /* Export*/
    exportPdf(): void;

    exportCsv(): void;
}

export const eventsController = ng.controller('EventsController', ['$scope', '$route', '$location', '$timeout',
    'GroupService', 'ReasonService', 'EventService',
    function ($scope, $route, $location, $timeout, GroupService: GroupService, ReasonService: ReasonService, eventService: EventService) {
        const isWidget = $route.current.action === 'dashboard';
        const vm: ViewModel = this;
        vm.filter = {
            startDate: isWidget ? DateUtils.add(new Date(), -5, 'd') : DateUtils.add(new Date(), -7, 'd'),
            endDate: isWidget ? DateUtils.add(new Date(), -1, 'd') : moment().endOf('day').toDate(),
            timeslots: {
                start: {name: '', startHour: '', endHour: '', id: ''},
                end: {name: '', startHour: '', endHour: '', id: ''}
            },
            students: [],
            classes: [],
            absences: true,
            departure: true,
            late: $route.current.action !== 'dashboard',
            regularized: true,
            regularizedNotregularized: false,
            allReasons: true,
            noReasons: true,
            reasons: {} as Reason,
            unjustified: true,
            justifiedNotRegularized: true,
            justifiedRegularized: false,
            followed: true,
            notFollowed: true,
            noFilter: true,
            page: 0
        };
        vm.provingReasonsMap = {};
        vm.isScroll = false;
        vm.eventType = [];
        vm.multipleSelect = {
            id: 0,
            label: lang.translate('presences.absence.select.multiple'),
            structure_id: '',
            comment: '',
            default: false,
            proving: false,
            group: false
        } as Reason;
        vm.studentSearchInput = '';
        vm.classesSearchInput = '';
        vm.students = new Students();
        vm.studentsLightbox = new Students();
        vm.classesFiltered = undefined;
        vm.classesFilteredLightbox = undefined;

        vm.event = new Event(0, "", "", "");
        vm.interactedEvent = {} as EventResponse;
        vm.events = new Events();
        vm.events.regularized = isWidget ? vm.filter.regularized : null;
        vm.events.eventer.on('loading::true', () => $scope.safeApply());
        vm.events.eventer.on('loading::false', () => {
            vm.eventId = null;
            $scope.safeApply();
        });
        vm.lightbox = {
            filter: false,
            action: false
        };
        vm.eventReasonsId = [];
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

        const loadFormFilter = async (): Promise<void> => {
            let formFilters = await Me.preference(PresencesPreferenceUtils.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_FILTER);
            formFilters = formFilters ? formFilters[window.structure.id] : null;
            if (formFilters) {
                let {reasonIds, ...toMergeFilters} = formFilters;
                vm.filter = {...vm.filter, ...toMergeFilters};
                vm.eventReasonsType.forEach((r) => {
                    r.isSelected = reasonIds.includes(r.id)
                });
                vm.eventType = [];
            } else {
                vm.eventType = [];
                vm.filter = {
                    ...vm.filter, ...{
                        absences: true,
                        departure: true,
                        late: $route.current.action !== 'dashboard',
                        allReasons: true,
                        unjustified: true,
                        justifiedNotRegularized: true,
                        justifiedRegularized: false,
                        followed: true,
                        notFollowed: true
                    }
                };
            }
        };

        const loadReasonTypes = async (): Promise<void> => {
            vm.eventReasonsType = await ReasonService.getReasons(window.structure.id);
            vm.eventReasonsTypeDescription = _.clone(vm.eventReasonsType);
            vm.eventReasonsType.map((reason: Reason) => {
                reason.isSelected = true;
                vm.provingReasonsMap[reason.id] = reason.proving;
            });

            if (!isWidget) vm.eventReasonsType.push(vm.multipleSelect);
        };

        const getEvents = async (actionMode?: boolean): Promise<void> => {
            vm.events.structureId = window.structure.id;
            vm.events.startDate = vm.filter.startDate.toDateString();
            vm.events.endDate = vm.filter.endDate.toDateString();
            vm.events.startTime = vm.filter.timeslots.start ? vm.filter.timeslots.start.startHour : null;
            vm.events.endTime = vm.filter.timeslots.end ? vm.filter.timeslots.end.endHour : null;
            vm.events.regularized = vm.filter.regularized;
            if (vm.filter.absences) {
                if (!vm.eventType.some(e => e == EventType.ABSENCE)) {
                    vm.eventType.push(EventType.ABSENCE);
                }
            }
            if (vm.filter.late) {
                if (!vm.eventType.some(e => e == EventType.LATENESS)) {
                    vm.eventType.push(EventType.LATENESS);
                }
            }
            if (vm.filter.departure) {
                if (!vm.eventType.some(e => e == EventType.DEPARTURE)) {
                    vm.eventType.push(EventType.DEPARTURE);
                }
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
            const calendarFilterKey: string = PresencesPreferenceUtils.PREFERENCE_KEYS.PRESENCE_EVENT_LIST_CALENDAR_FILTER
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
        }

        // Events actions
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
                eventType: vm.events.eventType,
                listReasonIds: (vm.filter.justifiedRegularized || vm.filter.justifiedNotRegularized) ? vm.eventReasonsId.toString() : '',
                userId: vm.events.userId,
                classes: vm.events.classes,
                page: vm.interactedEvent.page
            };
            filter.regularized = (!(<any>vm.eventType).includes(1)) ? null : vm.filter.regularized;
            filter.followed = (!(<any>vm.eventType).includes(1)) ? null : vm.filter.followed;
            filter.notFollowed = (!(<any>vm.eventType).includes(1)) ? null : vm.filter.notFollowed;
            let events = await eventService.get(filter);

            vm.events.pageCount = events.pageCount;
            vm.events.events = events.events;

            // replace events list by the event we fetched based on their page for each event
            vm.events.all = EventsUtils.interactiveConcat(vm.events.all, events.all, vm.interactedEvent.page);

            $scope.safeApply();
        };

        const getStructureTimeSlots = async () : Promise<void> => {
            vm.structureTimeSlot = await ViescolaireService.getSlotProfile(window.structure.id);
        };

        vm.formatMainDate = (date: string): string => DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-YEAR"]);

        vm.formatDate = function (date: string) {
            return DateUtils.format(date, DateUtils.FORMAT["DAY-MONTH-HALFYEAR"]);
        };

        vm.formatHourTooltip = function (date: string): string {
            return DateUtils.format(date, DateUtils.FORMAT["HOUR-MINUTES"]);
        };

        vm.findEvent = (events: Array<Event>): Event => {
            return events.find(event => event.type === EventsUtils.ALL_EVENTS.event);
        };

        vm.createAction = async function () {
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
            }
            PresencesPreferenceUtils.updatePresencesEventListCalendarFilter(filter);
            $location.path(`/calendar/${student.id}?date=${date}`);
            $scope.safeApply();
        };

        /* Change CSS class depending on their event_type id */
        vm.eventTypeState = (periods: IEventSlot, index: number, indexSlot: number): string => {
            const className: string[] = ['empty', 'remark', 'departure', 'late', 'absent', 'absent-no-regularized',
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
        vm.filterSelect = function (options: Reason[], event): Reason[] {
            let reasonIds = EventsUtils.getReasonIds(event.events);
            if (reasonIds.every((val: number, i: number, arr: number[]) => val === arr[0])) {
                return options.filter(option => option.id !== 0);
            }
            return options;
        };

        vm.updateFilterSlot = (hourPeriod: TimeSlotHourPeriod): void => {
            if (!vm.formFilter.timeslots.start || !vm.formFilter.timeslots.end) {
                switch (hourPeriod) {
                    case TimeSlotHourPeriod.START_HOUR:
                        vm.formFilter.timeslots.end = vm.formFilter.timeslots.start;
                        break;
                    case TimeSlotHourPeriod.END_HOUR:
                        vm.formFilter.timeslots.start = vm.formFilter.timeslots.end;
                        break;
                    default:
                        return;
                }
            }
        };

        vm.downloadFile = ($event): void => {
            $event.stopPropagation();
            console.log("downloading File");
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
                    if (vm.actionForm.eventId.indexOf(event.id) === -1) vm.actionForm.eventId.push(event.id)
                });
            }
            vm.actionForm.actionId = null;
            vm.actionForm.comment = '';
            getEventActions();
        };

        vm.showHistory = function () {
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

        /* Add global reason_id to all events that exist */
        vm.changeAllReason = async (event: EventResponse, studentId: string): Promise<void> => {
            let initialReasonId: number = event.reason.id;
            vm.interactedEvent = event;
            let fetchedEvent: Event | EventResponse[] = [];
            if (isWidget) {
                fetchedEvent.push(event);
            } else {
                EventsUtils.fetchEvents(event, fetchedEvent);
            }
            await vm.events.updateReason(fetchedEvent, initialReasonId, studentId, window.structure.id)
                .then(() => {
                    if (isWidget) vm.events.page = 0;
                    getEvents(true);
                    if (vm.filter.justifiedRegularized) {
                        vm.eventId = null;
                    }
                });
            $scope.safeApply();
        };

        /* Change its description reason id */
        vm.changeReason = async (history: Event, event: EventResponse, studentId: string): Promise<void> => {
            let initialReasonId = history.reason ? history.reason.id : history.reason_id;
            vm.interactedEvent = event;
            let fetchedEvent: Array<Event | EventResponse> = [];
            history.counsellor_regularisation = vm.provingReasonsMap[history.reason_id];
            fetchedEvent.push(history);
            event.reason.id = EventsUtils.initGlobalReason(event);
            await vm.events.updateReason(fetchedEvent, initialReasonId, studentId, window.structure.id)
                .then(() => {

                    if (EventsUtils.isEachEventsCounsellorRegularized(event.events) &&
                        EventsUtils.hasSameEventsReason(event.events)) {
                        if (!vm.filter.justifiedRegularized) {
                            getEvents(true);
                            vm.eventId = null;
                        }
                    }
                });
            $scope.safeApply();
        };

        vm.toggleAllEventsRegularised = async (event: EventResponse, studentId: string): Promise<void> => {
            let initialCounsellorRegularisation = event.counsellor_regularisation;
            vm.interactedEvent = event;
            let fetchedEvent: IEvent | Array<EventResponse> = [];
            EventsUtils.fetchEvents(event, fetchedEvent);
            vm.events.updateRegularized(fetchedEvent, initialCounsellorRegularisation, studentId, window.structure.id)
                .then(async () => {
                    $timeout(async () => {
                        // we use $timeout trick since we figured that updateRegularized was too fast to handle our data for our getEvent
                        // some data might not be updated at this time then we use timeout
                        await getEvents(true);
                        vm.eventId = null;
                        $scope.safeApply();
                    }, 500)
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
                    if (!isWidget) {

                        if (EventsUtils.hasSameEventsCounsellor(event.events)) {
                            getEvents(true);
                            if (!vm.filter.justifiedRegularized) {
                                vm.eventId = null;
                            }
                        }
                    } else {
                        getEvents(true);
                        $scope.safeApply();
                    }
                });
            $scope.safeApply();
        };

        /* Toggle Collapse */
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
          Student methods
        ---------------------------- */
        vm.searchByStudent = async (searchText: string) => {
            await vm.students.search(window.structure.id, searchText);
            $scope.safeApply();
        };

        vm.selectStudent = function (model: Student, option: Student) {
            vm.updateFilter(option);
            vm.studentSearchInput = '';
        };

        vm.selectStudentFromDashboard = function (model: Student, option: Student) {
            vm.filter.students = [];
            vm.selectStudent(model, option);
        };

        vm.excludeStudentFromFilter = (student) => {
            vm.filter.students = _.without(vm.filter.students, _.findWhere(vm.filter.students, student));
            vm.updateFilter();
        };

        /* ----------------------------
            Student lightbox methods
        ---------------------------- */

        vm.searchByStudentFromLightbox = async (searchText: string) => {
            await vm.studentsLightbox.search(window.structure.id, searchText);
            $scope.safeApply();
        };

        vm.selectStudentFromLightbox = function (value: string, student: Student) {
            if (!_.find(vm.formFilter.students, student)) {
                vm.formFilter.students.push(student);
            }
            vm.studentSearchInputLightbox = '';
            vm.studentsLightbox.all = [];
        };

        vm.excludeStudentFromFilterLightbox = (student) => {
            vm.formFilter.students = _.without(vm.formFilter.students, _.findWhere(vm.formFilter.students, student));
        };

        /* ----------------------------
          Classes methods
        ---------------------------- */
        vm.searchByClass = async function (value: string) {
            const structureId = window.structure.id;
            try {
                vm.classesFiltered = await GroupService.search(structureId, value);
                vm.classesFiltered.map((obj) => obj.toString = () => obj.name);
                $scope.safeApply();
            } catch (err) {
                vm.classesFiltered = [];
                throw err;
            }
            return;
        };

        vm.selectClass = (model: Student, option: Student): void => {
            vm.updateFilter(null, option);
            vm.classesSearchInput = '';
        };

        vm.excludeClassFromFilter = (audience) => {
            vm.filter.classes = _.without(vm.filter.classes, _.findWhere(vm.filter.classes, audience));
            vm.updateFilter();
        };

        /* ----------------------------
         Classes lightbox methods
       ---------------------------- */
        vm.searchByClassFromLightbox = async function (value: string) {
            const structureId = window.structure.id;
            try {
                vm.classesFilteredLightbox = await GroupService.search(structureId, value);
                vm.classesFilteredLightbox.map((obj) => obj.toString = () => obj.name);
                $scope.safeApply();
            } catch (err) {
                vm.classesFilteredLightbox = [];
                throw err;
            }
            return;
        };

        vm.selectClassFromLightbox = (value: string, classe: any): void => {
            if (!_.find(vm.formFilter.classes, classe)) {
                vm.formFilter.classes.push(classe);
            }
            vm.classesSearchInputLightbox = '';
            vm.classesFilteredLightbox = [];
        };

        vm.excludeClassFromFilterLightbox = (audience) => {
            vm.formFilter.classes = _.without(vm.formFilter.classes, _.findWhere(vm.formFilter.classes, audience));
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

            /* Fetch reason Id */
            vm.eventReasonsId = [];

            if (vm.eventReasonsType && vm.eventReasonsType.length > 0) {
                vm.eventReasonsType.forEach(r => {
                    if (r.isSelected && r.id) {
                        vm.eventReasonsId.push(r.id);
                    }
                });
            }

            /* Manage state unjustified */
            vm.filter.noReasons = vm.filter.unjustified;

            /* Manage state regularized */
            vm.filter.regularized = (vm.filter.unjustified && !vm.filter.justifiedRegularized && !vm.filter.justifiedNotRegularized) ||
            (vm.filter.justifiedRegularized && vm.filter.justifiedNotRegularized) ? null : vm.filter.justifiedRegularized;

            vm.events.eventType = vm.eventType.toString();
            vm.events.listReasonIds = (vm.filter.justifiedRegularized || vm.filter.justifiedNotRegularized) ? vm.eventReasonsId.toString() : "";
            vm.events.noReason = vm.filter.noReasons;
            vm.events.followed = vm.filter.followed;
            vm.events.notFollowed = vm.filter.notFollowed;
            vm.events.regularized = (!(<any>vm.eventType).includes(1)) ? null : vm.filter.regularized;
            vm.events.startDate = vm.filter.startDate.toDateString();
            vm.events.endDate = vm.filter.endDate.toDateString();
            vm.events.startTime = vm.filter.timeslots.start ? vm.filter.timeslots.start.startHour : null;
            vm.events.endTime = vm.filter.timeslots.end ? vm.filter.timeslots.end.endHour : null;

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
                listReasonIds: (vm.filter.justifiedRegularized || vm.filter.justifiedNotRegularized) ? vm.eventReasonsId.toString() : "",
                followed: vm.events.followed,
                notFollowed: vm.events.notFollowed,
                userId: vm.events.userId,
                classes: vm.events.classes,
                page: vm.filter.page
            };
            filter.regularized = (!(<any>vm.eventType).includes(1)) ? null : vm.filter.regularized;
            eventService
                .get(filter)
                .then((events: { pageCount: number, events: EventResponse[], all: EventResponse[] }) => {
                    if (events.all.length !== 0) {
                        vm.events.pageCount = events.pageCount;
                        vm.events.events = events.events;
                        vm.events.all = vm.events.all.concat(events.all);
                    }
                    $scope.safeApply();
                });
        };

        /* ----------------------------
          Export methods
        ---------------------------- */
        vm.exportPdf = function () {
            console.log("exporting Pdf");
        };

        vm.exportCsv = (): void => {
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
            window.open(eventService.exportCSV(filter));
        };

        /* ----------------------------
         Switch type methods
        ---------------------------- */

        vm.switchAbsencesFilter = (): void => {
            vm.formFilter.absences = !vm.formFilter.absences;
            if (vm.formFilter.absences) {
                if (!vm.eventType.some(e => e == EventType.ABSENCE)) {
                    vm.eventType.push(EventType.ABSENCE);
                }
            } else {
                vm.eventType = _.without(vm.eventType, EventType.ABSENCE);
                vm.formFilter.unjustified = false;
                vm.formFilter.justifiedNotRegularized = false;
                vm.formFilter.justifiedRegularized = false;
                vm.formFilter.followed = false;
                vm.formFilter.notFollowed = false;
            }
            vm.adaptReason();
        };


        vm.switchLateFilter = (): void => {
            vm.formFilter.late = !vm.formFilter.late;
            if (vm.formFilter.late) {
                if (!vm.eventType.some(e => e == EventType.LATENESS)) {
                    vm.eventType.push(EventType.LATENESS);
                }
            } else {
                vm.eventType = _.without(vm.eventType, EventType.LATENESS);
            }
        };

        vm.switchDepartureFilter = (): void => {
            vm.formFilter.departure = !vm.formFilter.departure;
            if (vm.formFilter.departure) {
                if (!vm.eventType.some(e => e == EventType.DEPARTURE)) {
                    vm.eventType.push(EventType.DEPARTURE);
                }
            } else {
                vm.eventType = _.without(vm.eventType, EventType.DEPARTURE);
            }
        };

        vm.switchUnjustifiedFilter = (): void => {
            vm.formFilter.unjustified = !vm.formFilter.unjustified;
        };

        vm.switchjustifiedNotRegularizedFilter = (): void => {
            vm.formFilter.justifiedNotRegularized = !vm.formFilter.justifiedNotRegularized;

        };

        vm.switchjustifiedRegularizedFilter = (): void => {
            vm.formFilter.justifiedRegularized = !vm.formFilter.justifiedRegularized;
        };

        vm.switchFollowedFilter = (): void => {
            if (vm.formFilter.followed === true) {
                vm.formFilter.notFollowed = true;
            }
            vm.formFilter.followed = !vm.formFilter.followed;
        };

        vm.switchNotFollowedFilter = (): void => {
            if (vm.formFilter.notFollowed === true) {
                vm.formFilter.followed = true;
            }
            vm.formFilter.notFollowed = !vm.formFilter.notFollowed;
        };

        vm.switchReason = async function (reason: Reason) {
            reason.isSelected = !reason.isSelected;
        };

        vm.switchAllReasons = function () {
            vm.formFilter.allReasons = !vm.formFilter.allReasons;
            vm.formFilter.noReasons = vm.formFilter.allReasons;
            vm.eventReasonsType.forEach(reason => reason.isSelected = vm.formFilter.allReasons);
        };

        vm.adaptEvent = function () {
            if (!vm.formFilter.unjustified && !vm.formFilter.justifiedNotRegularized
                && !vm.formFilter.justifiedRegularized) {
                vm.switchAbsencesFilter();
            }
        };

        vm.adaptReason = (): void => {
            if (!vm.formFilter.absences) {
                vm.eventReasonsId = [];
                vm.eventType = _.without(vm.eventType, EventType.ABSENCE);
            } else {
                vm.formFilter.unjustified = true;
                vm.formFilter.justifiedNotRegularized = true;
                vm.formFilter.justifiedRegularized = true;
                vm.formFilter.followed = true;
                vm.formFilter.notFollowed = true;
            }
        };

        vm.hideGlobalCheckbox = (event: EventResponse): boolean => {
            return event.reason === null || event.reason.id === null || vm.provingReasonsMap[event.reason.id];
        };

        /* Form filter */
        vm.openForm = function () {
            vm.lightbox.filter = true;
            vm.formFilter = JSON.parse(JSON.stringify(vm.filter));
            if (vm.formFilter.timeslots && vm.formFilter.timeslots.start && vm.formFilter.timeslots.end) {
                vm.formFilter.timeslots = {
                    start: vm.structureTimeSlot.slots.find((slot: ITimeSlot) =>
                        slot._id === vm.formFilter.timeslots.start._id),
                    end: vm.structureTimeSlot.slots.find((slot: ITimeSlot) =>
                        slot._id === vm.formFilter.timeslots.end._id)
                };
            }
        };

        vm.validForm = async function () {
            let formFilter: EventsFormFilter = {
                absences: vm.formFilter.absences,
                late: vm.formFilter.late,
                departure: vm.formFilter.departure,
                unjustified: vm.formFilter.unjustified,
                justifiedNotRegularized: vm.formFilter.justifiedNotRegularized,
                justifiedRegularized: vm.formFilter.justifiedRegularized,
                followed: vm.formFilter.followed,
                notFollowed: vm.formFilter.notFollowed,
                allReasons: vm.formFilter.allReasons,
                timeslots: vm.formFilter.timeslots,
                reasonIds: []
            };
            let selectedReasons: Reason[] = vm.eventReasonsType.filter((r: Reason) => r.isSelected);
            formFilter.reasonIds = selectedReasons.map((r: Reason) => r.id);
            await PresencesPreferenceUtils.updatePresencesEventListFilter(formFilter, window.structure.id);
            const {startDate, endDate} = vm.filter;
            vm.filter = {...vm.formFilter, startDate, endDate};
            vm.formFilter = {};
            vm.updateFilter();
            vm.lightbox.filter = false;
        };

        /* Form action */
        vm.validActionForm = function () {
            vm.lightbox.action = false;
            vm.createAction();
        };

        /* ----------------------------
                Handler events
         ---------------------------- */

        /* on (watch) */
        $scope.$watch(() => window.structure, async () => {
            if (window.structure) {
                this.filter.students = [];
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
            angular.element(document.querySelectorAll(".datepicker")).remove();
            angular.element(document.querySelectorAll(".tooltip")).remove();
        });

        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.FILTER, async () => await getEvents());
        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.EDIT, async () => await getEvents());
        $scope.$on(SNIPLET_FORM_EMIT_EVENTS.DELETE, async () => await getEvents());
    }]);