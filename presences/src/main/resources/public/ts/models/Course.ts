import http from 'axios';
import {_, moment} from 'entcore';
import {Mix} from 'entcore-toolkit';
import {LoadingCollection} from './LoadingCollection'

export interface Course {
    _id: string;
    structureId: string;
    subjectId: string;
    subjectName: string;
    teacherIds: string[];
    classes: string[];
    groups: string[];
    roomLabels: string[];
    dayOfWeek: number;
    startDate: string;
    endDate: string;
    register_id?: number;
    timestamp?: number;
}

export class Course {
}

export class Courses extends LoadingCollection {
    all: Course[];

    constructor() {
        super();
        this.all = [];
    }


    async sync(teacher: string, structure: string, start: string, end: string) {
        this.loading = true;
        try {
            const {data} = await http.get(`/presences/courses?teacher=${teacher}&structure=${structure}&start=${start}&end=${end}`);
            this.all = Mix.castArrayAs(Course, data);
            this.all.map((course: Course) => course.timestamp = moment(course.startDate).unix())
            this.all = _.sortBy(this.all, 'timestamp');
        } catch (err) {
            throw err;
        }
        this.loading = false;
    }

    clear(): void {
        this.all = [];
    }
}