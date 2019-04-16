import {model, notify} from 'entcore';
import http from 'axios';

export class Subject {
    id: string;
    label: string;
    code: string;
    teacherId: string;

    constructor(subjectId?: string, subjectLabel?: string, subjectCode?: string, teacherId?: string) {
        this.id = subjectId;
        this.label = subjectLabel;
        this.code = subjectCode;
        this.teacherId = teacherId;
    }
}

export class Subjects {
    all: Subject[];
    mapping: any;

    constructor() {
        this.all = [];
        this.mapping = {};
    }

    /**
     * Synchronize subjects provides by the structure
     * @param structureId structure id
     * @param teacherId
     * @returns {Promise<void>}
     */
    async sync(structureId: string): Promise<void> {
        if (typeof structureId !== 'string') {
            return;
        }
        try {
            let url = `/directory/timetable/subjects/${structureId}`;
            let subjects = await http.get(url);
            subjects.data.forEach((subject) => {
                this.all.push(new Subject(subject.subjectId, subject.subjectLabel, subject.subjectCode, subject.teacherId));
                this.mapping[subject.subjectId] = subject.subjectLabel;
            });
            return;
        } catch (e) {
            notify.error('app.notify.e500');
        }
    }
}