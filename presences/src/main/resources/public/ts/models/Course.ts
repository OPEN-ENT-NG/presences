import http, {AxiosResponse} from 'axios';
import {_, moment} from 'entcore';
import {Mix} from 'entcore-toolkit';
import {LoadingCollection} from '@common/model/LoadingCollection'
import {DateUtils} from '@common/utils'

export interface Course {
    id: string;
    structureId: string;
    subjectId: string;
    subjectName: string;
    classes: string[];
    groups: string[];
    roomLabels: string[];
    dayOfWeek: number;
    startDate: string;
    endDate: string;
    registerId?: number;
    timestamp?: number;
    teachers: { id: string, displayName: string }[];
    notified: boolean;
    splitSlot: boolean;
}

export class Course {
}

export class Courses extends LoadingCollection {
    all: Course[];
    map: Map<number, Course[]>;
    keysOrder: number[];

    constructor() {
        super();
        this.all = [];
        this.map = new Map<number, Course[]>();
        this.keysOrder = [];
    }


    async sync(teachers: string[] = null, groups: string[], structure: string, start: string, end: string,
               forgottenRegisters: boolean = false, multipleSlot: boolean = false, limit?: number, offset?: number) {
        if (this.loading) return;
        this.loading = true;
        try {
            let teacherFilter = '';
            let groupFilter = '';
            if (teachers && teachers.length > 0) {
                teachers.map((teacher: string) => teacherFilter += `teacher=${teacher}&`);
            }
            if (groups && groups.length > 0) {
                groups.map((group: string) => groupFilter += `group=${group}&`);
            }

            const time: string = `&_t=${moment().format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])}`;
            const forgottenRegisterParam: string = `&forgotten_registers=${forgottenRegisters}`;
            const multipleSlotParam: string = `&multiple_slot=${multipleSlot}`;
            const limitParam: string = limit || limit === 0 ? `&limit=${limit}` : '';
            const offsetParam: string = offset || offset === 0 ? `&offset=${(limit ? limit : 0) * offset}` : '';
            const orderParam: string = `&descendingDate=true`;
            const urlParams: string = `${forgottenRegisterParam}${multipleSlotParam}${time}${limitParam}${offsetParam}`;

            const {data}: AxiosResponse = await http.get(
                `/presences/courses?${teacherFilter}${groupFilter}structure=${structure}&start=${start}&end=${end}${urlParams}${orderParam}`
            );
            this.all = Mix.castArrayAs(Course, data);
            this.all.map((course: Course) => course.timestamp = moment(course.startDate).valueOf());
            this.all = _.sortBy(this.all, 'timestamp');
            this.map = this.groupByDate();
            this.keysOrder = Array.from(this.map.keys()).reverse();
        } catch (err) {
            throw err;
        } finally {
            this.loading = false;
        }
    }

    export(teachers: string[] = null, groups: string[], structure: string, start: string, end: string, forgottenRegisters: boolean = false) {
        let teacherFilter = '';
        let groupFilter = '';
        if (teachers && teachers.length > 0) {
            teachers.map((teacher) => teacherFilter += `teacher=${teacher}&`);
        }
        if (groups && groups.length > 0) {
            groups.map((group) => groupFilter += `group=${group}&`);
        }

        window.open(`/presences/courses/export?${teacherFilter}${groupFilter}structure=${structure}&start=${start}&end=${end}&forgotten_registers=${forgottenRegisters}&_t=${moment().format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])}`);
    }

    groupByDate() {
        const map: Map<number, Course[]> = new Map<number, Course[]>();
        for (let i = 0; i < this.all.length; i++) {
            const course: Course = this.all[i];
            const start: number = moment(course.startDate).startOf('day').valueOf();
            if (!map.has(start)) {
                map.set(start, []);
            }
            map.get(start).push(course);
        }
        return map;
    }

    clear(): void {
        this.all = [];
        this.map = new Map<number, Course[]>();
        this.keysOrder = [];
    }
}