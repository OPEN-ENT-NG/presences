import {Template} from '../services/SettingsService';
import http from 'axios';
import {DateUtils} from "@common/utils";

interface MassmailingStudent {
    id: string,
    selected: boolean,
    opened: boolean,
    displayName: string,
    className: string,
    events: {
        JUSTIFIED?: number,
        UNJUSTIFIED?: number,
        LATENESS?: number,
        PUNISHMENT?: number,
        SANCTION?: number
    },
    relative: {
        id: string,
        displayName: string,
        contact: string,
        selected: boolean
    }[]
}

declare const window: any;

export class Massmailing {
    type: string;
    counts: {
        massmailing: number,
        students: number,
        anomalies: number
    };
    students: MassmailingStudent[];
    filter: any;
    template: Template;

    constructor(type, counts, students) {
        this.type = type;
        this.counts = counts;
        this.students = students;
        this.students.forEach((student) => {
            student.relative.forEach(relative => {
                relative.selected = (relative.contact !== null && relative.contact.trim() !== '');
                if (!relative.selected) this.counts.massmailing--;
            });
            let shouldSelect = false;
            student.relative.map(relative => (shouldSelect = (shouldSelect || relative.selected)));
            if (student.relative.length > 0 && shouldSelect) {
                student.selected = true;
            }
            student.opened = false;
        });
    }

    private getStudents() {
        const students = {};
        this.students.forEach(function ({id, relative}) {
            students[id] = [];
            relative.forEach(function (relative) {
                if (relative.selected) students[id].push(relative.id);
            });
        });

        Object.keys(students).forEach(id => {
            if (students[id].length === 0) delete students[id];
        });

        return students;
    }

    private massmailed() {
        const {waiting, mailed} = this.filter.massmailing_status;
        if (mailed && waiting) {
            return null;
        }

        return mailed;
    }

    toJson() {
        const event_types = Object.keys(this.filter.status).filter(type => this.filter.status[type]);
        const reasons = [];
        Object.keys(this.filter.reasons).forEach((reason) => {
            if (this.filter.reasons[reason]) reasons.push(reason)
        });


        return {
            event_types,
            template: this.template.id,
            structure: window.structure.id,
            no_reason: this.filter.noReasons,
            start_at: this.filter.start_at,
            reasons,
            start: DateUtils.format(this.filter.start_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
            end: DateUtils.format(this.filter.end_date, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
            students: this.getStudents(),
            massmailed: this.massmailed()
        }
    }

    async process(): Promise<void> {
        await http.post(`/massmailing/massmailings/${this.type}`, this.toJson());
    }

    getRelativeCheckedCount(student: MassmailingStudent): number {
        let count = 0;
        student.relative.forEach(relative => (count += relative.selected ? 1 : 0));
        return count;
    }
}