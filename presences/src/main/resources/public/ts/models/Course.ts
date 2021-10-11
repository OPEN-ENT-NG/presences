import http, {AxiosResponse} from 'axios';
import {_, moment} from 'entcore';
import {Mix} from 'entcore-toolkit';
import {LoadingCollection} from '@common/model/LoadingCollection'
import {DateUtils} from '@common/utils'
import {ISubject} from "../models/Subject";

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
    subject?: ISubject;
    teachers: { id: string, displayName: string }[];
    notified: boolean;
    splitSlot: boolean;
    isOpenedByPersonnel: boolean;
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
               startTime: string, endTime: string,
               forgottenRegisters: boolean = false, multipleSlot: boolean = false, limit?: number, offset?: number,
               descendingDate?: Boolean, disableLoading?: boolean, searchTeacher?: boolean): Promise<void> {
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

            const startTimeParam: string = (startTime !== null && startTime !== undefined) ? `&startTime=${startTime}` : ``;
            const endTimeParam: string = (endTime !== null && endTime !== undefined) ? `&endTime=${endTime}` : ``;
            const forgottenRegisterParam: string = `&forgotten_registers=${forgottenRegisters}`;
            const multipleSlotParam: string = `&multiple_slot=${multipleSlot}`;
            const limitParam: string = limit || limit === 0 ? `&limit=${limit}` : '';
            const offsetParam: string = offset || offset === 0 ? `&offset=${offset}` : '';
            const orderParam: string = (descendingDate !== null && descendingDate !== undefined) ? `&descendingDate=${descendingDate}` : '';
            const isSearchTeacher: string = (searchTeacher != null) ? `&searchTeacher=${searchTeacher}` : '';
            const urlParams: string = `${forgottenRegisterParam}${multipleSlotParam}${startTimeParam}${endTimeParam}`
                +`${limitParam}${offsetParam}${orderParam}${isSearchTeacher}`;
            const {data}: AxiosResponse = await http.get(
                `/presences/courses?${teacherFilter}${groupFilter}structure=${structure}&start=${start}&end=${end}${urlParams}`
            );
            const newCourses = Mix.castArrayAs(Course, data);
            newCourses.forEach((course: Course) => course.subject.label = course.subject.name);
            this.hasCourses = newCourses.length > 0;
            this.all = [...this.all, ...newCourses];
            this.all.map((course: Course) => course.timestamp = moment(course.startDate).valueOf());
            this.all = _.sortBy(this.all, 'timestamp');
            this.groupByDate(newCourses);
            this.keysOrder = Array.from(this.map.keys())
                .sort((timestamp1: number, timestamp2: number) => timestamp1 - timestamp2);
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
        this.map.forEach((courses: Course[]) => {
            courses.sort((course1: Course, course2: Course) => course1.timestamp - course2.timestamp)
        });
    }

    clear(): void {
        this.all = [];
        this.map = new Map<number, Course[]>();
        this.keysOrder = [];
    }
}