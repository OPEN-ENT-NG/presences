import http, {AxiosResponse} from 'axios';
import {moment, toasts} from 'entcore';
import {DateUtils} from '@common/utils';
import {EventType, LoadingCollection} from '@common/model';
import {Reason} from '@presences/models/Reason';
import {EventsUtils} from '../../utilities';
import {User} from '@common/model/User';
import {Course} from '@presences/services';
import {IAbsence} from '@presences/models';
import {IAudience} from "@common/model/Audience";

export type StudentEvent = {
    id?: string;
    audiences?: Array<IAudience>;
    classId: string;
    className?: string;
    classeName?: string;
    day_history: Array<IEventSlot>;
    displayName: string;
    email?: string;
    firstName: string;
    lastName: string;
    name?: string;
}

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
    student?: { id: string, name: string, className: string };
    type?: string;
    exclude?: boolean;
    actionAbbreviation?: string;
    massmailed?: boolean;
    page?: number;
}

export interface EventResponse {
    action_abbreviation: string;
    counsellor_regularisation: boolean;
    created: string;
    date: string;
    massmailed: boolean
    reason: Reason;
    student: StudentEvent;
    events?: Array<IEvent>;
    page: number;
    type: string; // ('event' | 'absence')
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

    toJsonPut() {
        return {
            register_id: parseInt(this.register_id.toString()),
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
            await http.put(`/presences/events/${this.id}`, this.toJsonPut());
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
    startTime?: string;
    endTime?: string;
    userId: string;
    classes: string;

    eventType: string;
    listReasonIds: string;
    regularized: boolean;
    followed: boolean;
    notFollowed: boolean;
    noReason: boolean;
    noReasonLateness: boolean;

    constructor() {
        super();
        this.events = [];
    }

    static buildEventResponse(data: Array<EventResponse>, page: number): EventResponse[] {
        return data.map((eventResponse: EventResponse) => {
            eventResponse.page = page;
            eventResponse.events = [];

            eventResponse.student.day_history.forEach((eventsHistory: IEventSlot) => {
                eventsHistory.events.forEach((event: IEvent) => {
                    // filter on 'event' data and check potential duplicate event (by checking its id's index)
                    if (event.type === EventsUtils.ALL_EVENTS.event && !eventResponse.events.some(element => element.id === event.id)) {
                        event.page = page;
                        eventResponse.events.push(event);
                    }
                });
            });
            return eventResponse;
        });
    }

    async syncPagination(): Promise<void> {
        this.loading = true;
        let dateFormat: string = DateUtils.FORMAT['YEAR-MONTH-DAY'];
        try {
            let url: string =
                `/presences/events?structureId=${this.structureId}` +
                `&startDate=${DateUtils.format(this.startDate, dateFormat)}` +
                `&endDate=${DateUtils.format(this.endDate, dateFormat)}`;


            if (this.startTime) {
                url += `&startTime=${this.startTime}`;
            }
            if (this.endTime) {
                url += `&endTime=${this.endTime}`;
            }

            if (this.listReasonIds) {
                url += `&reasonIds=${this.listReasonIds}`;
            }

            if (this.noReason) {
                url += `&noReason=${this.noReason}`;
            }

            if (this.noReasonLateness) {
                url += `&noReasonLateness=${this.noReasonLateness}`;
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

    async updateRegularized(events, regularized, studentId, structureId): Promise<boolean> {
        if (events.length === 0) return;
        try {
            events.forEach(e => delete e.$$hashKey);
            await http.put(`/presences/events/regularized`, {events: events, regularized: regularized, student_id: studentId, structure_id: structureId});
            toasts.confirm('presences.absences.update_regularized');
            return true
        } catch (err) {
            console.error(err);
            toasts.warning('presences.absences.update_regularized.error');
            this.events[0].counsellor_regularisation = !this.events[0].counsellor_regularisation;
            return false;
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