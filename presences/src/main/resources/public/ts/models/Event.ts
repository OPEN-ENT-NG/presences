import http from 'axios';
import {moment} from 'entcore';
import {DateUtils} from '../utilities'
import {EventType} from "./EventType";

export interface Event {
    id: number;
    start_date?: string;
    end_date?: string;
    start_date_time: Date;
    end_date_time: Date;
    comment?: string;
    counsellor_input: boolean;
    student_id: string;
    register_id: number;
    type_id: number;
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

export class Events {
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