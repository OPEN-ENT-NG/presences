import http from 'axios';
import {moment} from 'entcore';
import {DateUtils} from '@common/utils'
import {EventType} from "./EventType";
import {Mix} from "entcore-toolkit";
import {LoadingCollection} from "@common/model";

export interface Event {
    id: number;
    start_date?: string;
    end_date?: string;
    start_date_time: Date;
    end_date_time: Date;
    comment?: string;
    counsellor_input: boolean;
    counsellor_regularisation: boolean;
    student_id: string;
    register_id: number;
    type_id: number;
    event_type?: { id: number, label: string };
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
            register_id: this.register_id,
            counsellor_regularisation: this.counsellor_regularisation,
            type_id: this.type_id,
            ...(this.student_id ? {student_id: this.student_id} : {}),
            ...(this.start_date ? {start_date: this.start_date} : {}),
            ...(this.end_date ? {end_date: this.end_date} : {}),
            ...(this.comment ? {comment: this.comment} : {})
        }
    }

    async save(): Promise<void> {
        if (this.id) {
            return this.update();
        } else {
            return this.create();
        }
    }

    async update(): Promise<void> {
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
    }

    async create(): Promise<void> {
        try {
            const {data} = await http.post('/presences/events', this.toJson());
            this.id = data.id;
            this.counsellor_input = data.counsellor_input;
        } catch (err) {
            throw err;
        }
    }

    async delete(): Promise<void> {
        try {
            await http.delete(`/presences/events/${this.id}`);
        } catch (err) {
            throw err;
        }
    }
}

export class Events extends LoadingCollection {
    all: Event[];

    pageCount: number;
    structureId: string;
    startDate: string;
    endDate: string;
    eventType: string;
    userId: string;
    classes: string;
    unjustified: boolean;
    regularized: boolean;

    // extract duplicated events
    extractEvents(): void {
        this.all.forEach((storedItem, storedIndex) => {
            this.all.map((item, index) => {
                if (storedIndex != index) {
                    if (storedItem.event_type.id === item.event_type.id &&
                        storedItem.student_id === item.student_id &&
                        moment(storedItem.start_date).format('YYYY-MM-DD') === moment(item.start_date).format('YYYY-MM-DD')) {
                        this.all.splice(storedIndex, 1);
                    }
                }
            })
        })

    }

    async syncPagination() {
        this.loading = true;
        let dateFormat = DateUtils.FORMAT['YEAR-MONTH-DAY'];
        try {
            let url =
                `/presences/events?structureId=${this.structureId}` +
                `&startDate=${DateUtils.format(this.startDate, dateFormat)}` +
                `&endDate=${DateUtils.format(this.endDate, dateFormat)}`;

            if (this.eventType) {
                url += `&eventType=${this.eventType}`;
            }

            if (this.userId) {
                url += `&userId=${this.userId}`;
            }

            if (this.classes) {
                url += `&classes=${this.classes}`;
            }

            if (this.unjustified) {
                url += `&unjustified=${this.unjustified}`;
            }

            if (this.regularized) {
                url += `&regularized=${this.regularized}`;
            }

            url += `&page=${this.page}`;
            const {data} = await http.get(url);
            this.pageCount = data.page_count;
            this.all = Mix.castArrayAs(Event, data.all);
            this.extractEvents();
        } catch (err) {
            throw err;
        } finally {
            this.loading = false;
        }
    }

    async updateReason(arrayEventsId, reasonId): Promise<void> {
        try {
            await http.put(`/presences/events/reason`, {ids: arrayEventsId, reasonId: reasonId});
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