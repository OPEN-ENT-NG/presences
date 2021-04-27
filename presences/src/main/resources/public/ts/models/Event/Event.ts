import http, {AxiosResponse} from 'axios';
import {_, moment} from 'entcore';
import {DateUtils} from '@common/utils';
import {EventType, LoadingCollection} from '@common/model';
import {Reason} from '@presences/models/Reason';
import {EventsUtils} from '../../utilities';
import {User} from '@common/model/User';
import {Course} from '@presences/services';
import {IAbsence} from '@presences/models';

export interface Event {
    id: number;
    start_date?: string;
    end_date?: string;
    start_date_time: Date;
    end_date_time: Date;
    comment?: string;
    counsellor_input: boolean;
    counsellor_regularisation: boolean;
    followed?: boolean;
    reason?: Reason;
    reason_id?: number;
    student_id: string;
    register_id: number;
    type_id: number;
    owner?: User;
    course?: Course;
    event_type?: { id: number, label: string };
    type?: string;
    events?: any[];
    exclude?: boolean;
    reasonId: number;
}

export interface IEvent {
    id: number;
    start_date?: string;
    end_date?: string;
    comment?: string;
    counsellor_input?: boolean;
    counsellor_regularisation: boolean;
    followed?: boolean;
    reason_id?: number;
    student_id: string;
    register_id?: number;
    type_id?: number;
    owner?: User;
    course?: Course;
    event_type?: { id: number, label: string };
    student?: {id: string, name: string, className: string};
    type?: string;
    exclude?: boolean;
    actionAbbreviation?: string;
    massmailed?: boolean;
}

export interface EventResponse {
    id: number;
    reason_id: number;
    studentId: string;
    displayName: string;
    className: string;
    date: string;
    dayHistory: Array<IEventSlot>;
    events: any[];
    globalReason?: number;
    globalCounsellorRegularisation?: boolean;
    globalMassmailed?: boolean;
    isGlobalAction?: boolean;
    exclude?: boolean;
    type?: { event: string, event_type: string, id: number };
    actionAbbreviation?: string;
    isAbsence?: boolean;
    isEventAbsence?: any;
    page?: number;
}

export interface IEventBody {
    id?: number;
    start_date: string;
    end_date: string;
    comment?: string;
    counsellor_input?: boolean;
    counsellor_regularisation?: boolean;
    followed?: boolean;
    massmailed?: boolean;
    type_id?: number;
    reason_id: number;
    register_id: number;
    student_id: string;
    type?: string;
}

export interface IEventFormBody {
    id?: number;
    startDate: Date;
    endDate: Date;
    startTime: Date;
    endTime: Date;
    comment?: string;
    studentId: string;
    eventType: string;
    absences?: IAbsence[] | IEvent[];
    reason_id?: number;
    absenceId?: number;
    counsellor_regularisation?: boolean;
    followed?: boolean;
}

export interface IEventSlot {
    events?: Array<IEvent>;
    start?: string;
    end?: string;
    name?: string;
}

export class Event {

    constructor(register_id: number, student_id: string, start_date: string, end_date: string) {
        this.register_id = register_id;
        this.student_id = student_id;
        this.start_date = start_date;
        this.end_date = end_date;
    }

    toJson() {
        return {
            register_id: parseInt(this.register_id.toString()),
            counsellor_regularisation: this.counsellor_regularisation,
            followed: this.followed,
            type_id: this.type_id,
            ...(this.student_id ? {student_id: this.student_id} : {}),
            ...(this.start_date ? {start_date: this.start_date} : {}),
            ...(this.end_date ? {end_date: this.end_date} : {}),
            ...(this.comment ? {comment: this.comment} : {})
        }
    }

    async save(): Promise<AxiosResponse> {
        if (this.id) {
            return this.update();
        } else {
            return this.create();
        }
    }

    async update(): Promise<AxiosResponse> {
        try {
            if (this.type_id === EventType.DEPARTURE) {
                this.start_date = moment(this.start_date_time).format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
            } else if (this.type_id === EventType.LATENESS) {
                this.end_date = moment(this.end_date_time).format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
            }
            await http.put(`/presences/events/${this.id}`, this.toJson());
        } catch (err) {
            throw err;
        }
        return;
    }

    async create(): Promise<AxiosResponse> {
        try {
            const {data} = await http.post('/presences/events', this.toJson());
            this.id = data.id;
            this.counsellor_input = data.counsellor_input;
        } catch (err) {
            throw err;
        }
        return;
    }

    async delete(): Promise<AxiosResponse> {
        try {
            await http.delete(`/presences/events/${this.id}`);
        } catch (err) {
            throw err;
        }
        return;
    }
}

export class Events extends LoadingCollection {
    all: EventResponse[];
    events: EventResponse[];

    pageCount: number;
    structureId: string;
    startDate: string;
    endDate: string;
    userId: string;
    classes: string;

    eventType: string;
    listReasonIds: string;
    regularized: boolean;
    followed: boolean;
    notFollowed: boolean;
    noReason: boolean;

    constructor() {
        super();
        this.events = [];
    }

    static buildEventResponse(data: any[], page: number): EventResponse[] {
        let builtEvents = [];

        data.forEach(item => {
            /* check if not duplicate dataModel */
            if (!builtEvents.some(e =>
                (e.classId === item.student.classId) &&
                (e.date === DateUtils.format(item.startDate, DateUtils.FORMAT['YEAR-MONTH-DAY'])) &&
                (e.studentId === item.student.id))) {
                /* new dataModel */
                let eventsFetchedFromHistory = [];

                /* array declared and used for managing global Reasons and CounsellorRegularization */
                let reasonIds: number[] = [];
                let regularizedEvents: boolean[] = [];
                let followedEvents: boolean[] = [];
                let actions: string[] = [];
                let massmailedEvents: boolean[] = [];

                /* build our eventsResponse array to affect our attribute "events" (see below dataModel.push) */
                item.student.dayHistory.forEach((eventsHistory: IEventSlot) => {
                    eventsHistory.events.forEach((event: IEvent) => {
                        if (event.type === EventsUtils.ALL_EVENTS.event) {
                            if (!eventsFetchedFromHistory.some(element => element.id === event.id)) {
                                eventsFetchedFromHistory.push(event);
                            }
                            if ('reason_id' in event && event.type_id === 1) {
                                reasonIds.push(event.reason_id);
                            }
                            if ('counsellor_regularisation' in event && event.type_id === 1) {
                                regularizedEvents.push(event.counsellor_regularisation);
                            }
                            if ('followed' in event && event.type_id === 1) {
                                followedEvents.push(event.followed);
                            }
                            if ('actionAbbreviation' in event) {
                                actions.push(event.actionAbbreviation);
                            }

                            if ('massmailed' in event) {
                                massmailedEvents.push(event.massmailed);
                            }
                        }
                    });
                });

                /* store all reason id in an array and check if they are all
                the same to display multiple select or unique */
                let globalReason: number;
                if (!reasonIds.every((val, i: number, arr: any[]) => val === arr[0])) {
                    /* all events will have different reason id */
                    globalReason = 0;
                } else {
                    globalReason = parseInt(_.uniq(reasonIds));
                    if (isNaN(globalReason)) {
                        globalReason = null;
                    }
                }
                /* check all events regularized in this event to display the global regularized value */
                let globalCounsellorRegularisation: boolean = regularizedEvents.length === 0 ? false
                    : regularizedEvents.reduce((accumulator: boolean, currentValue: boolean) => accumulator && currentValue);

                let globalFollowed: boolean = followedEvents.length === 0 ? false
                    : followedEvents.reduce((accumulator: boolean, currentValue: boolean) => accumulator && currentValue);

                /* check all events massmailed in this event to display the global massmailed value */
                let globalMassmailed: boolean = massmailedEvents.length === 0 ? false
                    : massmailedEvents.reduce((accumulator: boolean, currentValue: boolean) => accumulator && currentValue);

                let type = {event: item.type, event_type: item.eventType.label, id: item.id};

                /* check if there are differents actions */
                let isGlobalAction = new Set(actions).size === 1;

                let isAbsence: boolean = item.eventType.id === 1;

                /* We build our event based on information stored above */
                if (item.type === EventsUtils.ALL_EVENTS.event) {
                    builtEvents.push({
                        studentId: item.student.id,
                        displayName: item.student.name,
                        className: item.student.className,
                        classId: item.student.classId,
                        date: moment(item.startDate).format(DateUtils.FORMAT['YEAR-MONTH-DAY']),
                        dayHistory: item.student.dayHistory,
                        events: eventsFetchedFromHistory,
                        exclude: item.exclude ? item.exclude : false,
                        globalReason: globalReason,
                        globalCounsellorRegularisation: globalCounsellorRegularisation,
                        globalFollowed: globalFollowed,
                        globalMassmailed: globalMassmailed,
                        isGlobalAction: isGlobalAction,
                        type: type,
                        actionAbbreviation: item.actionAbbreviation,
                        isAbsence: isAbsence,
                        page: page
                    });
                }
            }
        });

        return builtEvents;
    }

    async syncPagination(): Promise<void> {
        this.loading = true;
        let dateFormat: string = DateUtils.FORMAT['YEAR-MONTH-DAY'];
        try {
            let url: string =
                `/presences/events?structureId=${this.structureId}` +
                `&startDate=${DateUtils.format(this.startDate, dateFormat)}` +
                `&endDate=${DateUtils.format(this.endDate, dateFormat)}`;

            if (this.listReasonIds) {
                url += `&reasonIds=${this.listReasonIds}`;
            }

            if (this.noReason) {
                url += `&noReason=${this.noReason}`;
            }

            if (this.regularized != null) {
                url += `&regularized=${this.regularized}`;
            }


            // Add followed filter only if filter values are opposite
            if (this.followed != null || this.notFollowed != null) {
                if (this.followed === !this.notFollowed) {
                    url += `&followed=${this.followed}`
                }
            }

            if (this.eventType) {
                url += `&eventType=${this.eventType}`;
            }

            if (this.userId) {
                url += `&userId=${this.userId}`;
            }

            if (this.classes) {
                url += `&classes=${this.classes}`;
            }

            url += `&page=${this.page}`;
            const {data} = await http.get(url);
            this.pageCount = data.page_count;
            this.events = data.all;
            this.all = Events.buildEventResponse(data.all, this.page);
        } catch (err) {
            throw err;
        } finally {
            this.loading = false;
        }
    }

    async updateReason(arrayEvents, reasonId, studentId, structureId): Promise<void> {
        if (arrayEvents.length === 0) return;
        try {
            arrayEvents.forEach(e => delete e.$$hashKey);
            await http.put(`/presences/events/reason`, {events: arrayEvents, reasonId: reasonId, student_id: studentId, structure_id: structureId});
        } catch (err) {
            throw err;
        }
    }

    async updateRegularized(events, regularized, studentId, structureId): Promise<void> {
        if (events.length === 0) return;
        try {
            events.forEach(e => delete e.$$hashKey);
            await http.put(`/presences/events/regularized`, {events: events, regularized: regularized, student_id: studentId, structure_id: structureId});
        } catch (err) {
            throw err;
        }
    }
}

export class Absence extends Event {
    constructor(register_id: number, student_id: string, start_date: string, end_date: string) {
        super(register_id, student_id, start_date, end_date);
        this.type_id = EventType.ABSENCE;
    }

    toAbsenceJson(structureId: string, reasonId: number, ownerId: string): Object {
        return {
            structure_id: structureId,
            owner: ownerId,
            reason_id: reasonId ? reasonId : null,
            ...(this.student_id ? {student_id: this.student_id} : {}),
            ...(this.start_date ? {start_date: this.start_date} : {}),
            ...(this.end_date ? {end_date: this.end_date} : {})
        };
    }

    async getAbsence(absenceId: number): Promise<AxiosResponse> {
        try {
            return await http.get(`/presences/absence/${absenceId}`);
        } catch (err) {
            throw err;
        }
    }

    async createAbsence(structureId?: string, reasonId?: number, ownerId?: string): Promise<AxiosResponse> {
        try {
            return await http.post('/presences/absence', this.toAbsenceJson(structureId, reasonId, ownerId));
        } catch (err) {
            throw err;
        }
    }

    async updateAbsence(absenceId?: number, structureId?: string, reasonId?: number, ownerId?: string): Promise<AxiosResponse> {
        try {
            return await http.put(`/presences/absence/${absenceId}`, this.toAbsenceJson(structureId, reasonId, ownerId));
        } catch (err) {
            throw err;
        }
    }

    async updateAbsenceReason(arrayAbsencesId, reasonId): Promise<void> {
        if (arrayAbsencesId.length === 0) return;
        try {
            await http.put(`/presences/absence/reason`, {ids: arrayAbsencesId, reasonId: reasonId});
        } catch (err) {
            throw err;
        }
    }

    async updateAbsenceRegularized(arrayAbsencesId, regularized): Promise<void> {
        if (arrayAbsencesId.length === 0) return;
        try {
            await http.put(`/presences/absence/regularized`, {ids: arrayAbsencesId, regularized: regularized});
        } catch (err) {
            throw err;
        }
    }

    async deleteAbsence(absenceId?: number): Promise<AxiosResponse> {
        try {
            return await http.delete(`/presences/absence/${absenceId}`);
        } catch (err) {
            throw err;
        }
    }

    async deleteEventAbsence(eventId?: number): Promise<AxiosResponse> {
        try {
            return await http.delete(`/presences/events/${eventId}`);
        } catch (err) {
            throw err;
        }
    }
}

export class Lateness extends Event {
    constructor(register_id: number, student_id: string, start_date: string, end_date: string) {
        super(register_id, student_id, start_date, end_date);
        this.type_id = EventType.LATENESS;
    }
}

export class Departure extends Event {
    constructor(register_id: number, student_id: string, start_date: string, end_date: string) {
        super(register_id, student_id, start_date, end_date);
        this.type_id = EventType.DEPARTURE;
    }
}

export class Remark extends Event {
    constructor(register_id, student_id, start_date, end_date) {
        super(register_id, student_id, start_date, end_date);
        this.type_id = EventType.REMARK;
        this.comment = '';
    }
}