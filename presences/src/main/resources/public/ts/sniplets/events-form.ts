import {IStructureSlot, ITimeSlot, SNIPLET_FORM_EMIT_EVENTS, SNIPLET_FORM_EVENTS} from '@common/model';
import {
    Absence,
    AbsenceEventResponse,
    EventType,
    IAbsence,
    IEventBody,
    IEventFormBody,
    Lateness,
    Reason,
    TimeSlotHourPeriod
} from '../models';
import {idiom as lang, model, moment, toasts} from 'entcore';
import {absenceService, eventService, reasonService, ViescolaireService} from '../services';
import {IAngularEvent} from 'angular';
import {DateUtils} from '@common/utils';
import {ABSENCE_FORM_EVENTS, LATENESS_FORM_EVENTS} from '@common/core/enum/presences-event';
import {EventsUtils} from '../utilities';
import {AxiosError, AxiosResponse} from 'axios';

console.log('eventFormSniplets');

declare let window: any;

type TEventType = 'ABSENCE' | 'LATENESS';

interface IFormData {
    id?: number;
    startDate: Date;
    endDate: Date;
    startDateTime: Date;
    endDateTime: Date;
    reason_id: number;
    register_id: number;
    student_id: string;
    absences?: Array<IAbsence>;
    comment?: string;
    type_id?: number;
    type?: string;
    eventType?: TEventType;
    counsellor_regularisation?: boolean;
    followed?: boolean;

    timeSlotTimePeriod?: {
        start: ITimeSlot;
        end: ITimeSlot;
    };
    startSlot?: string;
    endSlot?: string;
}

interface ViewModel {
    structureTimeSlot: IStructureSlot;
    createEventLightBox: boolean;
    eventsTypes: Array<TEventType>;

    selectedEventType: TEventType;

    isEventEditable: boolean;

    /* Form used for our sniplet to display/interact */
    form: IFormData;

    /* eventBody used for our sniplet to send JsonData to our API */
    eventBody: IEventBody;

    /* event model used to consume our API */
    event: Absence | Lateness;

    /* Absence part variables */

    updateAbsenceRegularisation: boolean;
    display: { isFreeSchedule: boolean };
    reasons: Array<Reason>;
    selectedReason: Reason;
    canRegularize: boolean;

    /* TimeSlots part variables */

    timeSlotHourPeriod: typeof TimeSlotHourPeriod;

    timeSlotTimePeriod?: {
        start: ITimeSlot;
        end: ITimeSlot;
    };

    isButtonAllowed: boolean;

    /* interact lightbox part methods */

    switchEventTypeForm(eventType: TEventType): void;

    getEventTypeLabel(eventType: TEventType): string;

    openEventLightbox(eventType: TEventType, event?: IAngularEvent, args?: IEventFormBody): void;

    closeEventLightbox(): void;

    setFormParams(obj): void;

    setFormDateParams(start_date, end_date): void;

    setFormEventType(eventType: TEventType): void;

    /* Event global methods */

    submitEvent(eventType: TEventType): void;

    editEvent(eventType: TEventType): void;

    canSwitchEventTypeForm(eventType: TEventType): boolean;

    deleteEvent(eventType: TEventType, canReload: boolean): void;

    prepareAbsenceBody(): void;

    setEventModel(event: Absence | Lateness): void;

    isFormValid(eventType: TEventType): boolean;

    /* Absence part methods */

    editAbsenceForm(eventType: TEventType, obj: IEventFormBody): void;

    editEventAbsenceForm(eventType: TEventType, obj: IEventFormBody): void;

    toUpdateRegularisation(): void;

    updateRegularisation(id?: number): Promise<void>;

    updateFollowed(id?: number): Promise<void>;

    selectReason(): void;

    createAbsence(): Promise<void>;

    updateAbsence(): Promise<void>;

    deleteAbsence(canReload: boolean): Promise<void>;

    /* Lateness part methods */

    prepareLatenessBody(): void;

    createLateness(): Promise<void>;

    updateLateness(): Promise<void>;

    deleteLateness(canReload: boolean): Promise<void>;

    editEventForm(eventType: TEventType, obj: IEventFormBody): void;

    /* Date / Time slots part methods */
    selectDatePicker(startDate: Date, eventType: TEventType): void;

    selectTimeSlot(hourPeriod: TimeSlotHourPeriod, eventType: TEventType): void;

    setStartSlotFromSelectTimeSlot(eventType: TEventType, timeSlotTimePeriod: { start: ITimeSlot, end: ITimeSlot });

    setEndSlotFromSelectTimeSlot(eventType: TEventType, timeSlotTimePeriod: { start: ITimeSlot, end: ITimeSlot });

    setTimeSlot(): void;

    defaultTimeSlot(): void;

    safeApply(fn?: () => void): void;
}

const vm: ViewModel = {
    safeApply: null,
    createEventLightBox: false,
    reasons: null,
    form: null,
    eventBody: null,
    event: null,
    eventsTypes: ['ABSENCE', 'LATENESS'],
    selectedEventType: null,
    isEventEditable: true,
    selectedReason: null,
    canRegularize: false,
    updateAbsenceRegularisation: false,
    timeSlotHourPeriod: TimeSlotHourPeriod,
    display: {isFreeSchedule: false},
    structureTimeSlot: {} as IStructureSlot,
    isButtonAllowed: true,

    switchEventTypeForm: (eventType: TEventType): void => {
        switch (eventType) {
            case 'ABSENCE':
                vm.selectedEventType = 'ABSENCE';
                break;
            case 'LATENESS':
                vm.selectedEventType = 'LATENESS';
                vm.form.endDateTime = moment(new Date()).set({second: 0, millisecond: 0}).toDate();
                break;
        }
        vm.setFormEventType(eventType);
    },

    getEventTypeLabel: (eventType: TEventType): string => {
        let i18n: string;
        switch (eventType) {
            case 'ABSENCE':
                i18n = 'presences.register.event_type.absences';
                break;
            case 'LATENESS':
                i18n = 'presences.register.event_type.lateness';
                break;
            default:
                i18n = '';
        }
        return i18n;
    },

    openEventLightbox(eventType: TEventType, event?: IAngularEvent, args?: IEventFormBody): void {
        vm.createEventLightBox = true;
        vm.display.isFreeSchedule = false;
        vm.form = {} as IFormData;
        vm.setFormEventType(eventType);
        vm.switchEventTypeForm(eventType);
        eventsForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.CREATION);
        if (event) {
            vm.form.startDate = args.startDate;
            vm.form.endDate = args.endDate;
            vm.form.startDateTime = args.startTime;
            vm.form.endDateTime = args.endTime;
            if (!vm.form.student_id) {
                vm.form.student_id = args.studentId;
            }
            vm.setTimeSlot();
        }
        vm.form.reason_id = null;
        vm.safeApply();
    },

    closeEventLightbox(): void {
        vm.createEventLightBox = false;
        if (vm.updateAbsenceRegularisation) {
            vm.form.absences[0].counsellor_regularisation = !vm.form.absences[0].counsellor_regularisation;
            vm.updateAbsenceRegularisation = false;
        }

        vm.timeSlotTimePeriod = {
            start: null,
            end: null
        };

        vm.form = null;
        vm.eventBody = null;
        vm.event = null;
        eventsForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.CANCEL);
    },

    setFormParams: ({student, start_date, end_date}): void => {
        if (vm.form) {
            vm.form.student_id = student.id;
            vm.setFormDateParams(start_date, end_date);
            vm.safeApply();
        }
    },

    setFormDateParams: (start_date, end_date): void => {
        vm.form.startDate = moment(start_date).toDate();
        vm.form.endDate = moment(end_date).toDate();
        if (vm.form.id) {
            vm.form.startDateTime = moment(vm.form.startDate).set({second: 0, millisecond: 0}).toDate();
            vm.form.endDateTime = moment(vm.form.endDate).set({second: 0, millisecond: 0}).toDate();
            vm.setTimeSlot();
            return;
        } else {
            vm.form.startDateTime = moment(new Date()).set({second: 0, millisecond: 0}).toDate();
        }
        vm.form.endDateTime = moment(new Date()).set({second: 0, millisecond: 0}).toDate();
    },

    setFormEventType: (eventType: TEventType): void => {
        switch (eventType) {
            case 'ABSENCE':
                (<Absence> vm.event) = new Absence(null, null, null, null);
                break;
            case 'LATENESS':
                (<Lateness> vm.event) = new Lateness(null, null, null, null);
                break;
        }
    },

    async editAbsenceForm(eventType: TEventType, obj: IEventFormBody): Promise<void> {
        vm.createEventLightBox = true;
        vm.form = {} as IFormData;
        vm.event = new Absence(null, null, null, null);
        vm.switchEventTypeForm(eventType);
        let response: AxiosResponse = await (<Absence> vm.event).getAbsence(obj.absenceId);
        if (response.status === 200 || response.status === 201) {
            /* Assign response data to form edited */
            vm.form.absences = [response.data];
            vm.form.absences.forEach((absence: IAbsence) => absence.type = EventsUtils.ALL_EVENTS.absence);
            vm.form.id = response.data.id;
            vm.form.startDate = response.data.start_date;
            vm.form.endDate = response.data.end_date;
            vm.form.student_id = response.data.student_id;
            vm.form.reason_id = response.data.reason_id;
            vm.form.followed = response.data.followed;
            vm.form.counsellor_regularisation = response.data.counsellor_regularisation;
            vm.form.type_id = EventType.ABSENCE;
            vm.form.eventType = 'ABSENCE';
            vm.setFormDateParams(vm.form.startDate, vm.form.endDate);
            vm.setTimeSlot();
        } else {
            toasts.warning(response.data.toString());
        }
        vm.safeApply();
    },

    editEventForm(eventType: TEventType, data: IEventFormBody): void {
        vm.createEventLightBox = true;
        vm.form = {} as IFormData;
        vm.switchEventTypeForm(eventType);
        vm.form.id = data.id;
        vm.form.student_id = data.studentId;
        vm.form.comment = data.comment;
        switch (eventType) {
            case 'LATENESS':
                vm.form.startDate = data.startDate;
                vm.form.endDate = data.endDate;
                vm.form.type_id = EventType.LATENESS;
                vm.form.eventType = 'LATENESS';
                vm.setFormDateParams(vm.form.startDate, vm.form.endDate);
                break;
        }
        vm.safeApply();
    },

    toUpdateRegularisation(): void {
        vm.updateAbsenceRegularisation = !vm.updateAbsenceRegularisation;
    },

    editEventAbsenceForm(eventType: TEventType, data: IEventFormBody): void {
        vm.createEventLightBox = true;
        vm.form = {} as IFormData;
        vm.event = new Absence(null, null, null, null);
        vm.switchEventTypeForm(eventType);
        vm.form.absences = <IAbsence[]> data.absences;
        vm.form.id = 1; // tricks to force our form want to update an "absence" whereas is it actually an event
        vm.form.startDate = data.startDate;
        vm.form.endDate = data.endDate;
        vm.form.student_id = data.studentId;
        vm.form.type_id = EventType.ABSENCE;
        vm.form.eventType = 'ABSENCE';
        vm.form.reason_id = data.reason_id ? data.reason_id : vm.form.absences
            .find(a => 'type' in a || 'type_id' in a).reason_id;

        vm.selectedReason = vm.reasons.find(reason => reason.id === vm.form.reason_id);
        vm.form.counsellor_regularisation = data.counsellor_regularisation;
        vm.form.followed = data.followed;

        vm.canRegularize = (vm.selectedReason) ? (!vm.selectedReason.proving) : false;

        vm.form.type = data.eventType;
        vm.setFormDateParams(vm.form.startDate, vm.form.endDate);
        vm.safeApply();
    },

    /**
     * Check validity of absence/lateness form data.
     */
    isFormValid: (eventType: TEventType): boolean => {
        switch (eventType) {
            case 'ABSENCE':
                if (vm.form && vm.form.startDate && vm.form.startDateTime && vm.form.endDate && vm.form.endDateTime) {
                    return (DateUtils.getDateFormat(vm.form.startDate, vm.form.startDateTime) <=
                        DateUtils.getDateFormat(vm.form.endDate, vm.form.endDateTime));
                }
                return false;
            case 'LATENESS':
                if (vm.form && vm.form.startDate && vm.form.endDate && vm.timeSlotTimePeriod
                    && vm.timeSlotTimePeriod.start && vm.timeSlotTimePeriod.start.startHour
                    && vm.timeSlotTimePeriod.end && vm.timeSlotTimePeriod.end.endHour && vm.form.endDateTime) {
                    return (DateUtils.getDateFormat(vm.form.startDate, DateUtils.getTimeFormatDate(vm.timeSlotTimePeriod.start.startHour)) <=
                        DateUtils.getDateFormat(vm.form.endDate, DateUtils.getTimeFormatDate(vm.timeSlotTimePeriod.start.startHour))) &&
                        (DateUtils.getDateFormat(vm.form.endDate, vm.form.endDateTime) >=
                            DateUtils.getDateFormat(vm.form.startDate, DateUtils.getTimeFormatDate(vm.timeSlotTimePeriod.start.startHour))) &&
                        (DateUtils.getDateFormat(vm.form.endDate, vm.form.endDateTime) <=
                            DateUtils.getDateFormat(vm.form.endDate, DateUtils.getTimeFormatDate(vm.timeSlotTimePeriod.end.endHour)));
                }
                return false;
        }
    },

    selectReason(): void {
        vm.selectedReason = vm.reasons.find(reason => reason.id === vm.form.reason_id);
        vm.canRegularize = (vm.selectedReason) ? (!vm.selectedReason.proving) : false;
        vm.updateAbsenceRegularisation = vm.selectedReason ? vm.selectedReason.proving : false;
        vm.form.counsellor_regularisation = vm.selectedReason ? vm.selectedReason.proving : false;
        if (vm.form.absences) {
            vm.form.absences.forEach((absence: IAbsence) => absence.counsellor_regularisation = vm.selectedReason ? vm.selectedReason.proving : false);
        }
    },

    submitEvent(eventType: TEventType): void {
        switch (eventType) {
            case 'ABSENCE':
                vm.createAbsence();
                break;
            case 'LATENESS':
                vm.createLateness();
                break;
        }
    },

    editEvent(eventType: TEventType): void {
        switch (eventType) {
            case 'ABSENCE':
                if (vm.form.eventType !== 'ABSENCE') {
                    vm.deleteEvent(vm.form.eventType, false);
                    vm.createAbsence();
                } else {
                    vm.updateAbsence();
                }
                break;
            case 'LATENESS':
                if (vm.form.eventType !== 'LATENESS') {
                    if (vm.form.eventType === 'ABSENCE') {
                        vm.event = new Absence(null, null, null, null);
                    }
                    vm.deleteEvent(vm.form.eventType, false);
                    vm.createLateness();
                } else {
                    vm.updateLateness();
                }
                break;
        }
    },

    canSwitchEventTypeForm(eventType: TEventType): boolean {
        switch (eventType) {
            case 'ABSENCE':
                return true;
            case 'LATENESS':
                if (vm.form && vm.form.eventType === 'ABSENCE') {
                    let startDate: string = DateUtils.format(vm.form.startDate.toDateString(), DateUtils.FORMAT["YEAR-MONTH-DAY"]);
                    let endDate: string = DateUtils.format(vm.form.endDate.toDateString(), DateUtils.FORMAT["YEAR-MONTH-DAY"])
                    return startDate === endDate
                        && vm.timeSlotTimePeriod.start.name === vm.timeSlotTimePeriod.end.name
                        && !vm.display.isFreeSchedule;
                }
                return true;
        }
        return false;
    },

    deleteEvent(eventType: TEventType, canReload: boolean): void {
        switch (eventType) {
            case 'ABSENCE':
                vm.deleteAbsence(canReload);
                break;
            case 'LATENESS':
                vm.deleteLateness(canReload);
                break;
        }
    },

    prepareAbsenceBody(): void {
        vm.eventBody = {} as IEventBody;
        vm.eventBody.start_date = vm.display.isFreeSchedule ?
            DateUtils.getDateFormat(vm.form.startDate, vm.form.startDateTime) :
            DateUtils.getDateFormat(moment(vm.form.startDate), DateUtils.getTimeFormatDate(vm.timeSlotTimePeriod.start.startHour));
        vm.eventBody.end_date = vm.display.isFreeSchedule ?
            DateUtils.getDateFormat(vm.form.endDate, vm.form.endDateTime) :
            DateUtils.getDateFormat(moment(vm.form.endDate), DateUtils.getTimeFormatDate(vm.timeSlotTimePeriod.end.endHour));
        vm.eventBody.reason_id = vm.form.reason_id ? vm.form.reason_id : null;
        vm.eventBody.register_id = vm.form.register_id ? vm.form.register_id : null;
        vm.eventBody.student_id = vm.form.student_id ? vm.form.student_id : null;
        vm.eventBody.type = vm.form.type ? vm.form.type : null;
        vm.eventBody.counsellor_regularisation = vm.form.counsellor_regularisation ? vm.form.counsellor_regularisation : false;
        vm.eventBody.followed = vm.form.followed ? vm.form.followed : false;
    },

    setEventModel(event: Absence | Lateness): void {
        event.student_id = vm.eventBody.student_id;
        event.start_date = vm.eventBody.start_date;
        event.end_date = vm.eventBody.end_date;
    },

    async createAbsence(): Promise<void> {
        vm.prepareAbsenceBody();
        vm.setEventModel(vm.event);
        let response: AxiosResponse = await (<Absence> vm.event).createAbsence(window.structure.id, vm.eventBody.reason_id, model.me.userId);
        if (response.status === 200 || response.status === 201) {
            await vm.updateFollowed(response.data.events.id);
            await vm.updateRegularisation(response.data.events.id);
            vm.closeEventLightbox();
            toasts.confirm(lang.translate('presences.absence.form.create.succeed'));
        } else {
            toasts.warning(response.data.toString());
        }
        eventsForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.FILTER);
        vm.safeApply();
    },

    async updateAbsence(): Promise<void> {
        vm.prepareAbsenceBody();
        vm.setEventModel(vm.event);
        let responses: AxiosResponse[] = [];
        // In this const, we consider vm.form.absences without field "type" can be an event (on calendar view logical)
        // but sometimes we might have type field equal to events (seen on event list) so we double check
        const isEventTypeToInteract: boolean = !vm.form.absences.find((absence: IAbsence) => 'type' in absence) ||
            vm.form.absences.find((absence: IAbsence) => absence.type === EventsUtils.ALL_EVENTS.event) !== undefined;
        const containsAbsenceEvent: boolean = vm.form.absences.find((absence: IAbsence) => absence.type === EventsUtils.ALL_EVENTS.absence) !== undefined;
        if (vm.eventBody.type === EventsUtils.ALL_EVENTS.event && isEventTypeToInteract && !containsAbsenceEvent) {
            responses = [await (<Absence>vm.event).createAbsence(window.structure.id, vm.eventBody.reason_id, model.me.userId)];
        } else {
            for (const absence of vm.form.absences) {
                if (absence.type === EventsUtils.ALL_EVENTS.absence) {
                    responses.push(await (<Absence>vm.event).updateAbsence(absence.id, window.structure.id, vm.eventBody.reason_id, model.me.userId));
                }
            }
        }

        // we check if dataResponse contain 'events' field that represents the `createAbsence`'s API response
        // else we use the dataResponse itself as it is the `updateAbsence`'s API response
        const dataResponse: any = responses
            .find((response: AxiosResponse) => response.status === 200 || response.status === 201).data;

        let dataAbsenceEventResponse: AbsenceEventResponse;
        // we remain undefined if dataResponse has not found any response
        if (dataResponse) {
            dataAbsenceEventResponse = 'events' in dataResponse ? dataResponse.events : dataResponse;
        }

        // Follow then regularize (with event updating)
        await vm.updateFollowed(dataAbsenceEventResponse ? dataAbsenceEventResponse.id : undefined);
        await vm.updateRegularisation(dataAbsenceEventResponse ? dataAbsenceEventResponse.id : undefined);

        vm.closeEventLightbox();
        toasts.confirm(lang.translate('presences.absence.form.edit.succeed'));

        eventsForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.EDIT);
        vm.safeApply();
    },


    /**
     * Update the regularisation state of the absence.
     */
    async updateRegularisation(id?: number): Promise<void> {
        if (vm.eventBody && vm.form) {
            let absence: Absence = new Absence(vm.eventBody.register_id, vm.eventBody.student_id, null, null);

            // case id parameter is not assigned in this method
            const absenceFound: IAbsence = vm.form.absences ? vm.form.absences.find(
                (absence: IAbsence) => (absence.type === EventsUtils.ALL_EVENTS.absence)) : undefined;
            const absenceFoundId: number = absenceFound !== undefined ? absenceFound.id : null;
            absence.id = id ? id : absenceFoundId;
            absence.counsellor_regularisation = vm.eventBody.counsellor_regularisation;
            if (absence.id) {
                absence.updateAbsenceRegularized([absence.id], absence.counsellor_regularisation);
            }
            vm.updateAbsenceRegularisation = false;
        }
    },

    /**
     * Update the followed state of the absence.
     */
    async updateFollowed(id?: number): Promise<void> {
        if (vm.eventBody && vm.form) {
            let absenceIds: Array<number> = [];

            if (vm.form.absences) {
                for (const absence of vm.form.absences) {
                    if ('type' in absence && absence.type === EventsUtils.ALL_EVENTS.absence) {
                        absenceIds.push(absence.id);
                    }
                }
            }
            await absenceService.updateFollowed(id ? [id] : absenceIds, vm.eventBody.followed);
        }
    },

    async deleteAbsence(canReload: boolean): Promise<void> {
        let responses: Array<AxiosResponse> = [];
        for (const absence of vm.form.absences) {
            if ('type' in absence && absence.type === EventsUtils.ALL_EVENTS.absence) {
                responses.push(await (<Absence>vm.event).deleteAbsence(absence.id));
            } else {
                responses.push(await (<Absence>vm.event).deleteEventAbsence(absence.id));
            }
        }

        let failedResponse: AxiosResponse = responses.find((response: AxiosResponse) => response.status != 200 && response.status != 201);
        if (failedResponse) {
            toasts.warning(failedResponse.data.toString());
        } else {
            vm.closeEventLightbox();
            toasts.confirm(lang.translate('presences.absence.form.delete.succeed'));
        }
        if (canReload) {
            eventsForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.DELETE);
        }
        vm.safeApply();
    },

    prepareLatenessBody(): void {
        vm.eventBody = {} as IEventBody;
        vm.eventBody.start_date = DateUtils.getDateFormat(moment(vm.form.startDate), DateUtils.getTimeFormatDate(vm.timeSlotTimePeriod.start.startHour));
        vm.eventBody.end_date = DateUtils.getDateFormat(vm.form.endDate, vm.form.endDateTime);
        vm.eventBody.student_id = vm.form.student_id ? vm.form.student_id : null;
        vm.eventBody.comment = vm.form.comment ? vm.form.comment : "";
        vm.eventBody.register_id = vm.form.register_id ? vm.form.register_id : -1;
        vm.eventBody.type_id = EventType.LATENESS;
    },

    async createLateness(): Promise<void> {
        vm.prepareLatenessBody();
        eventService.createLatenessEvent(vm.eventBody, window.structure.id)
            .then((response: AxiosResponse) => {
                if (response.status === 200 || response.status === 201) {
                    vm.closeEventLightbox();
                    toasts.confirm(lang.translate('presences.lateness.form.create.succeed'));
                    eventsForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.FILTER);
                    vm.safeApply();
                } else {
                    toasts.warning(lang.translate('presences.lateness.form.create.no.register'));
                }
            })
            .catch((_: AxiosError) => {
                toasts.warning(lang.translate('presences.lateness.form.create.no.register'));
            });
    },

    async updateLateness(): Promise<void> {
        vm.prepareLatenessBody();
        eventService.updateEvent(vm.form.id, vm.eventBody)
            .then((response: AxiosResponse) => {
                if (response.status === 200 || response.status === 201) {
                    vm.closeEventLightbox();
                    toasts.confirm(lang.translate('presences.lateness.form.edit.succeed'));
                    eventsForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.EDIT);
                    vm.safeApply();
                } else {
                    toasts.warning(lang.translate('presences.lateness.form.edit.error'));
                }
            })
            .catch((_: AxiosError) => {
                toasts.warning(lang.translate('presences.lateness.form.edit.error'));
            });
    },

    async deleteLateness(canReload: boolean): Promise<void> {
        eventService.deleteEvent(vm.form.id)
            .then((response: AxiosResponse) => {
                if (response.status === 200 || response.status === 201) {
                    vm.closeEventLightbox();
                    toasts.confirm(lang.translate('presences.lateness.form.delete.succeed'));
                    if (canReload) {
                        eventsForm.that.$emit(SNIPLET_FORM_EMIT_EVENTS.DELETE);
                    }
                    vm.safeApply();
                } else {
                    toasts.warning(lang.translate('presences.lateness.form.delete.error'));
                }
            })
            .catch((_: AxiosError) => {
                toasts.warning(lang.translate('presences.lateness.form.delete.error'));
            });
    },

    selectDatePicker: (date: Date, eventType: TEventType): void => {
        switch (eventType) {
            case "LATENESS":
                vm.form.startDate = date;
                vm.form.endDate = date;
                break;
        }
    },

    selectTimeSlot: (hourPeriod: TimeSlotHourPeriod, eventType: TEventType): void => {
        switch (hourPeriod) {
            case TimeSlotHourPeriod.START_HOUR:
                vm.setStartSlotFromSelectTimeSlot(eventType, vm.timeSlotTimePeriod);
                break;
            case TimeSlotHourPeriod.END_HOUR:
                vm.setEndSlotFromSelectTimeSlot(eventType, vm.timeSlotTimePeriod);
                break;
            default:
                return;
        }
    },

    setStartSlotFromSelectTimeSlot: (eventType: TEventType, timeSlotTimePeriod: { start: ITimeSlot, end: ITimeSlot }): void => {
        switch (eventType) {
            case 'ABSENCE': {
                vm.form.startSlot = vm.form.timeSlotTimePeriod.start != null ? DateUtils.getDateFormat(new Date(vm.form.startDate),
                    DateUtils.getTimeFormatDate(timeSlotTimePeriod.start.startHour)) : null;
                break;
            }
            case 'LATENESS': {
                vm.timeSlotTimePeriod.end = timeSlotTimePeriod.start;
                vm.form.endSlot = DateUtils.getDateFormat(new Date(vm.form.startDate), DateUtils.getTimeFormatDate(timeSlotTimePeriod.start.startHour));
                break;
            }
        }
    },

    setEndSlotFromSelectTimeSlot: (eventType: TEventType, timeSlotTimePeriod: { start: ITimeSlot, end: ITimeSlot }): void => {
        switch (eventType) {
            case 'ABSENCE': {
                vm.form.endSlot = vm.form.timeSlotTimePeriod.end != null ? DateUtils.getDateFormat(new Date(vm.form.endDate),
                    DateUtils.getTimeFormatDate(timeSlotTimePeriod.end.endHour)) : null;
                break;
            }
            case 'LATENESS': {
                vm.timeSlotTimePeriod.start = timeSlotTimePeriod.end;
                vm.form.startSlot = DateUtils.getDateFormat(new Date(vm.form.endDate), DateUtils.getTimeFormatDate(timeSlotTimePeriod.start.endHour));
                break;
            }
        }
    },

    setTimeSlot: (): void => {
        let start: string = DateUtils.format(vm.form.startDate, DateUtils.FORMAT['HOUR-MINUTES']);
        let end: string = DateUtils.format(vm.form.endDate, DateUtils.FORMAT['HOUR-MINUTES']);
        vm.timeSlotTimePeriod = {
            start: {endHour: '', id: '', name: '', startHour: ''},
            end: {endHour: '', id: '', name: '', startHour: ''}
        };
        vm.structureTimeSlot.slots.forEach((slot: ITimeSlot) => {
            if (slot.startHour === start) {
                vm.timeSlotTimePeriod.start = slot;
            }
            if (slot.endHour === end) {
                vm.timeSlotTimePeriod.end = slot;
            }
        });

        // defaultTime case structureTimeSlot failed to assign our timeSlotTimePeriod start or end
        vm.defaultTimeSlot();

        vm.display.isFreeSchedule = !(vm.timeSlotTimePeriod.start.startHour !== '' && vm.timeSlotTimePeriod.end.endHour !== '');
    },

    defaultTimeSlot: (): void => {
        if (!vm.timeSlotTimePeriod.start.startHour && vm.timeSlotTimePeriod.end.endHour) {
            vm.timeSlotTimePeriod.start = vm.timeSlotTimePeriod.end;
        }
        if (!vm.timeSlotTimePeriod.end.endHour && vm.timeSlotTimePeriod.start.startHour) {
            vm.timeSlotTimePeriod.end = vm.timeSlotTimePeriod.start;
        }
    },


};

export const eventsForm = {
    title: 'presences.register.event_type.absences',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            eventsForm.that = this;
            this.setButton();
            vm.safeApply = this.safeApply;
        },
        async getStructureTimeSlot(): Promise<void> {
            vm.structureTimeSlot = await ViescolaireService.getSlotProfile(window.structure.id);
        },
        setHandler: async function () {

            /* Absence event */
            this.$on(ABSENCE_FORM_EVENTS.OPEN, (event: IAngularEvent, args: IEventFormBody) => vm.openEventLightbox('ABSENCE', event, args));
            this.$on(ABSENCE_FORM_EVENTS.EDIT, (event: IAngularEvent, args: IEventFormBody) => vm.editAbsenceForm('ABSENCE', args));
            this.$on(ABSENCE_FORM_EVENTS.EDIT_EVENT, (event: IAngularEvent, args: IEventFormBody) => vm.editEventAbsenceForm('ABSENCE', args));

            /* Lateness event */
            this.$on(LATENESS_FORM_EVENTS.OPEN, (event: IAngularEvent, args: IEventFormBody) => vm.openEventLightbox('LATENESS', event, args));
            this.$on(LATENESS_FORM_EVENTS.EDIT, (event: IAngularEvent, args: IEventFormBody) => vm.editEventForm('LATENESS', args));


            /* setFormData FROM calendar view (event sent from SNIPLET_FORM_EMIT_EVENTS.CREATION) */
            this.$on(SNIPLET_FORM_EVENTS.SET_PARAMS, (event: IAngularEvent, arg) => vm.setFormParams(arg));

            this.$watch(() => window.structure, async () => {
                vm.reasons = await reasonService.getReasons(window.structure.id);
                this.getStructureTimeSlot();
                vm.safeApply();
            });
        },
        setButton: function () {
            switch (window.location.hash) {
                case '#/events': {
                    vm.isButtonAllowed = false;
                    break;
                }
                default:
                    vm.isButtonAllowed = true;
                    break;
            }
        }
    }
};