import {Discipline, ITimeSlot} from "../models";
import {User} from "@common/model/User";
import {Student} from "@common/model/Student";
import {LoadingCollection} from "@common/model";
import {_, moment} from "entcore";

export interface MarkedStudent {
    presenceId: number;
    comment: string;
    student: Student;
    isCommentEditable?: boolean;
}

export interface MarkedStudentRequest {
    comment: string;
    studentId: string;
}

export interface PresenceRequest {
    structureId: string;
    startDate: string;
    endDate: string;
    studentIds?: Array<String>;
    ownerIds?: Array<String>;
}

export interface PresenceBody {
    id?: number;
    structureId: string;
    startDate: string;
    endDate: string;
    disciplineId: number;
    markedStudents: Array<MarkedStudentRequest>;
    timeSlotTimePeriod?: {
        start: ITimeSlot;
        end: ITimeSlot;
    };
}

export interface Presence {
    id: number;
    structureId: string;
    startDate: string;
    endDate: string;
    discipline: Discipline;
    owner: User;
    markedStudents: Array<MarkedStudent>;
    timestamp?: number;
}

export class Presences extends LoadingCollection {
    structureId: string;
    all: Array<Presence>;
    map: any;
    keysOrder: Array<String>;

    constructor(structureId) {
        super();
        this.structureId = structureId;
        this.all = [];
        this.map = {};
        this.keysOrder = [];
    }

    async build(data: Presence[]): Promise<void> {
        this.all = [];
        data.forEach((presence: Presence) => {
            this.all.push(presence);
        });
        this.all.map((presence: Presence) => presence.timestamp = moment(presence.startDate).valueOf());
        this.all = _.sortBy(this.all, 'timestamp');
        this.map = this.groupByDate();
        this.keysOrder = Object.keys(this.map).reverse();
    }

    groupByDate(): {} {
        const map = {};
        this.all.forEach(presence => {
            const start = moment(presence.startDate).startOf('day').valueOf();
            if (!(start in map)) {
                map[start] = [];
            }
            map[start].push(presence);
        });
        return map;
    }
}