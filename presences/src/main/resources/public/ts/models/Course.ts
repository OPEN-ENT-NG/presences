import http from 'axios';
import {_, moment} from 'entcore';
import {Mix} from 'entcore-toolkit';
import {LoadingCollection} from '@common/model/LoadingCollection'

export interface Course {
    _id: string;
    structureId: string;
    subjectId: string;
    subjectName: string;
    classes: string[];
    groups: string[];
    roomLabels: string[];
    dayOfWeek: number;
    startDate: string;
    endDate: string;
    register_id?: number;
    timestamp?: number;
    teachers: { id: string, displayName: string }[];
}

export class Course {
}

export class Courses extends LoadingCollection {
    all: Course[];
    map: any;
    keysOrder: string[];

    constructor() {
        super();
        this.all = [];
        this.map = {};
        this.keysOrder = [];
    }


    async sync(teachers: string[] = null, groups: string[], structure: string, start: string, end: string, forgottenRegisters: boolean = false) {
        this.loading = true;
        try {
            let teacherFilter = '';
            let groupFilter = '';
            if (teachers && teachers.length > 0) {
                teachers.map((teacher) => teacherFilter += `teacher=${teacher}&`);
            }
            if (groups && groups.length > 0) {
                groups.map((group) => groupFilter += `group=${group}&`);
            }

            const {data} = await http.get(`/presences/courses?${teacherFilter}${groupFilter}structure=${structure}&start=${start}&end=${end}&forgotten_registers=${forgottenRegisters}`);
            this.all = Mix.castArrayAs(Course, data);
            this.all.map((course: Course) => course.timestamp = moment(course.startDate).valueOf());
            this.all = _.sortBy(this.all, 'timestamp');
            this.map = this.groupByDate();
            this.keysOrder = Object.keys(this.map).reverse();
        } catch (err) {
            throw err;
        }
        this.loading = false;
    }

    groupByDate() {
        const map = {};
        for (let i = 0; i < this.all.length; i++) {
            const course = this.all[i];
            const start = moment(course.startDate).startOf('day').valueOf();
            if (!(start in map)) {
                map[start] = [];
            }

            map[start].push(course);
        }

        return map;
    }

    clear(): void {
        this.all = [];
    }
}