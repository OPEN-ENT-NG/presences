import http, {AxiosError, AxiosResponse} from 'axios';
import {_, moment, toasts} from 'entcore';
import {Mix} from 'entcore-toolkit';
import {Event, EventType, RegisterStatus, Remark} from './index';
import {LoadingCollection} from '@common/model';
import {registerService} from '../services';

export interface RegisterStudent {
    id: string;
    name: string;
    group: string;
    events: any[];
    day_history: any[];
    group_name: string;
    absence?: Event;
    departure?: Event;
    lateness?: Event;
    remark?: Event;
    exempted?: boolean;
    exemption_attendance?: boolean;
    exempted_subjectId?: string;
    exemption_recursive_id?: number;
    birth_date: string;
}

interface Teacher {
    id: string;
    displayName: string;
    functions: string[];
}

export interface Register {
    id: number;
    structure_id: string;
    groups?: string[];
    classes: string[];
    personnel_id: string;
    course_id: string;
    state_id: number;
    counsellor_input: boolean;
    subject_id: string;
    start_date: string;
    end_date: string;
    proof_id?: number;
    absenceCounter: number;
    groupMap?: any;
    notified: boolean;
    splitSlot: boolean;
    students: RegisterStudent[];
    teachers: Teacher[];
    teacherIds?: Array<string>;
}

export class Register extends LoadingCollection {
    constructor() {
        super();
    }

    toJson() {
        return {
            course_id: this.course_id,
            structure_id: this.structure_id,
            start_date: this.start_date,
            end_date: this.end_date,
            subject_id: this.subject_id,
            groups: this.groups,
            split_slot: this.splitSlot,
            classes: this.classes,
            teacherIds: this.teacherIds
        };
    }

    async create() {
        this.loading = true;
        try {
            const {data} = await http.post('/presences/registers', this.toJson());
            this.id = data.id;
            this.state_id = data.state_id;
            this.counsellor_input = data.counsellor_input;
        } catch (err) {
            throw err;
        }
        this.loading = false;

    }

    private formatStudents() {
        this.students.map((student) => {
            let absence = _.findWhere(student.events, {type_id: EventType.ABSENCE});
            let lateness = _.findWhere(student.events, {type_id: EventType.LATENESS});
            let departure = _.findWhere(student.events, {type_id: EventType.DEPARTURE});
            let remark = _.findWhere(student.events, {type_id: EventType.REMARK});
            if (absence) absence = Mix.castAs(Event, absence);
            if (lateness) {
                lateness = Mix.castAs(Event, lateness);
                lateness.end_date_time = moment(lateness.end_date).toDate();
            }
            if (departure) {
                departure = Mix.castAs(Event, departure);
                departure.start_date_time = moment(departure.start_date).toDate();
            }
            student.remark = remark ? Mix.castAs(Event, remark) : new Remark(this.id, student.id, this.start_date, this.end_date);
            student.absence = absence;
            student.lateness = lateness;
            student.departure = departure;
        });
    }

    async sync() {
        this.loading = true;
        try {
            const {data} = await http.get(`/presences/registers/${this.id}`);
            this.personnel_id = data.personnel_id;
            this.proof_id = data.proof_id;
            this.course_id = data.course_id;
            this.subject_id = data.subject_id;
            this.start_date = data.start_date;
            this.end_date = data.end_date;
            this.counsellor_input = data.counsellor_input;
            this.students = data.students;
            this.state_id = data.state_id;
            this.teachers = data.teachers;
            this.notified = data.notified;
            this.formatStudents();
            this.absenceCounter = 0;
            this.students.map((student) => {
                if (student.absence) {
                    this.absenceCounter++;
                }
            });
            if (data.groups.length > 1) {
                this.groupMap = {};
                this.students.map((student) => {
                    const {group_name} = student;
                    if (!(group_name in this.groupMap)) {
                        this.groupMap[group_name] = [];
                    }
                    this.groupMap[group_name].push(student);
                });
            }
        } catch (err) {
            // Weird trick to fix replication postgresql production cluster. Do not remove
            if (err.response.status === 404) {
                const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));
                await sleep(1000);
                return this.sync();
            } else {
                throw err;
            }
        }
        this.loading = false;
    }

    async setStatus(state_id: number): Promise<void> {
        registerService.setStatus(this.id, state_id)
            .then((response: AxiosResponse) => {
                // Prevent displaying toasts when selecting students in register.
                if (state_id !== RegisterStatus.DONE) {
                    return;
                }
                if (response.status === 200 || response.status === 201 || response.status === 204) {
                    toasts.confirm('presences.register.validation.success');
                } else {
                    toasts.warning('presences.register.validation.error');
                }
            })
            .catch((_: AxiosError) => {
                toasts.warning('presences.register.validation.error');
            });
    }
}

export class Registers extends LoadingCollection {
    constructor() {
        super();
    }
}