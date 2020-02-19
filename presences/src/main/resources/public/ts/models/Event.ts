import http, {AxiosResponse} from 'axios';
import {_, moment} from 'entcore';
import {DateUtils} from '@common/utils'
import {EventType} from "./EventType";
import {LoadingCollection} from "@common/model";
import {Reason} from "@presences/models/Reason";

export interface Event {
    id: number;
    start_date?: string;
    end_date?: string;
    start_date_time: Date;
    end_date_time: Date;
    comment?: string;
    counsellor_input: boolean;
    counsellor_regularisation: boolean;
    reason?: Reason;
    reason_id?: number;
    student_id: string;
    register_id: number;
    type_id: number;
    event_type?: { id: number, label: string };
    type?: string;
    events?: any[];
    exclude?: boolean;
    reasonId: number;
}

export interface EventResponse {
    id: number;
    reason_id: number;
    studentId: string;
    displayName: string;
    className: string;
    date: string;
    dayHistory: any[];
    events: any[];
    globalReason?: number;
    globalCounsellorRegularisation?: boolean;
    exclude?: boolean;
    type?: { event: string, id: number };
    actionAbbreviation?: string;
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
    regularizedNotregularized: boolean;
    noReason: boolean;
    noFilter: boolean;

    constructor() {
        super();
        this.events = [];
    }

    static buildEventResponse(data: any[]): EventResponse[] {
        let dataModel = [];

        data.forEach(item => {
            /* check if not duplicate dataModel */
            if (!dataModel.some(e => (JSON.stringify(e.dayHistory) == JSON.stringify(item.student.dayHistory)))) {
                /* new dataModel */
                let eventsResponse = [];

                /* array declared and used for managing global Reasons and CounsellorRegularization */
                let reasonIds = [];
                let regularizedEvents = [];

                /* build our eventsResponse array to affect our attribute "events" (see below dataModel.push) */
                item.student.dayHistory.forEach(eventsHistory => {
                    eventsHistory.events.forEach(event => {
                        if (!eventsResponse.some(element => element.id == event.id)) {
                            eventsResponse.push(event);
                        }
                        if ("reason_id" in event) {
                            reasonIds.push(event.reason_id);
                        }
                        if ("counsellor_regularisation" in event) {
                            regularizedEvents.push(event.counsellor_regularisation);
                        }
                    });
                });

                /* store all reason id in an array and check if they are all
                the same to display multiple select or unique */
                let globalReason: number;
                if (!reasonIds.every((val, i, arr) => val === arr[0])) {
                    // all events will have different reason id
                    globalReason = 0;
                } else {
                    globalReason = parseInt(_.uniq(reasonIds));
                    if (isNaN(globalReason)) {
                        globalReason = null;
                    }
                }

                /* check all events regularized in this event to display the global regularized value */
                let globalCounsellorRegularisation = regularizedEvents.length === 0 ? false
                    : regularizedEvents.reduce((accumulator, currentValue) => accumulator && currentValue);
                let type = {event: item.type, id: item.id};

                /* We build our event based on information stored above */
                dataModel.push({
                    studentId: item.student.id,
                    displayName: item.student.name,
                    className: item.student.className,
                    classId: item.student.classId,
                    date: moment(item.startDate).format(DateUtils.FORMAT["YEAR-MONTH-DAY"]),
                    dayHistory: item.student.dayHistory,
                    events: eventsResponse,
                    // courses: _.uniq(item.student.courses, "_id"),
                    exclude: item.exclude ? item.exclude : false,
                    globalReason: globalReason,
                    globalCounsellorRegularisation: globalCounsellorRegularisation,
                    type: type,
                    actionAbbreviation: item.actionAbbreviation
                });
            }
        });
        return dataModel;
    };

    async syncPagination() {
        this.loading = true;
        let dateFormat = DateUtils.FORMAT['YEAR-MONTH-DAY'];
        try {
            let url =
                `/presences/events?structureId=${this.structureId}` +
                `&startDate=${DateUtils.format(this.startDate, dateFormat)}` +
                `&endDate=${DateUtils.format(this.endDate, dateFormat)}`;

            if (this.noFilter) {
                if (this.listReasonIds) {
                    url += `&reasonIds=${this.listReasonIds}`;
                }

                if (this.noReason) {
                    url += `&noReason=${this.noReason}`;
                }

                if (!this.regularizedNotregularized) {
                    if (this.regularized) {
                        url += `&regularized=${!this.regularized}`;
                    } else if (!this.regularized) {
                        url += `&regularized=${!this.regularized}`;
                    }
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
            this.all = Events.buildEventResponse(data.all);
        } catch (err) {
            throw err;
        } finally {
            this.loading = false;
        }
    }

    async updateReason(arrayEventsId, reasonId): Promise<void> {
        if (arrayEventsId.length === 0) return;
        try {
            await http.put(`/presences/events/reason`, {ids: arrayEventsId, reasonId: reasonId});
        } catch (err) {
            throw err;
        }
    }

    async updateRegularized(eventsId, regularized): Promise<void> {
        if (eventsId.length === 0) return;
        try {
            await http.put(`/presences/events/regularized`, {ids: eventsId, regularized: regularized});
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
        }
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