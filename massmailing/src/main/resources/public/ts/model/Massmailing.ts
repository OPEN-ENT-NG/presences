import {Template} from '../services/SettingsService'

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
                relative.selected = (relative.contact !== null);
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

    getRelativeCheckedCount(student: MassmailingStudent): number {
        let count = 0;
        student.relative.forEach(relative => (count += relative.selected ? 1 : 0));
        return count;
    }
}