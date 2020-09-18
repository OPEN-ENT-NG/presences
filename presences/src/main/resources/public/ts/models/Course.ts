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
    pageSize: number;
    hasCourses: boolean;

    constructor() {
        super();
        this.all = [];
        this.map = new Map<number, Course[]>();
        this.keysOrder = [];
        this.pageSize = 100;
        this.hasCourses = true;
    }


    async sync(teachers: string[] = null, groups: string[], structure: string, start: string, end: string,
               forgottenRegisters: boolean = false, multipleSlot: boolean = false, limit?: number, offset?: number,
               descendingDate?: Boolean, disableLoading?: boolean) {
        if (!disableLoading) {
            if (this.loading) return;
        }
        if (!disableLoading) {
            this.loading = true;
        }
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
            const offsetParam: string = offset || offset === 0 ? `&offset=${offset}` : '';
            // const orderParam: string = (descendingDate !== null && descendingDate !== undefined) ? `&descendingDate=${descendingDate}` : '';
            const orderParam: string = `&descendingDate=false`;
            const urlParams: string = `${forgottenRegisterParam}${multipleSlotParam}${time}${limitParam}${offsetParam}${orderParam}`;

            const {data}: AxiosResponse = await http.get(
                `/presences/courses?${teacherFilter}${groupFilter}structure=${structure}&start=${start}&end=${end}${urlParams}`
            );
            const newCourses = Mix.castArrayAs(Course, data);
            this.hasCourses = newCourses.length > 0;
            this.all = [...this.all, ...newCourses];
            this.all.map((course: Course) => course.timestamp = moment(course.startDate).valueOf());
            this.all = _.sortBy(this.all, 'timestamp');
            this.groupByDate(newCourses);
            this.keysOrder = Array.from(this.map.keys())
                .sort((timestamp1: number, timestamp2: number) => timestamp2 - timestamp1);
        } catch (err) {
            throw err;
        } finally {
            if (!disableLoading) {
                this.loading = false;
            }
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

    groupByDate(courses: Course[]): void {
        for (let i = 0; i < courses.length; i++) {
            const course: Course = courses[i];
            const start: number = moment(course.startDate).startOf('day').valueOf();
            if (!this.map.has(start)) {
                this.map.set(start, []);
            }
            this.map.get(start).push(course);
        }
    }

    clear(): void {
        this.all = [];
        this.map = new Map<number, Course[]>();
        this.keysOrder = [];
    }
}