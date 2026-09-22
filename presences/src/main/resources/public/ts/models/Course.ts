import http, {AxiosResponse} from 'axios';
import {_, moment} from 'entcore';
import {Mix} from 'entcore-toolkit';
import {LoadingCollection} from '@common/model/LoadingCollection'
import {DateUtils} from '@common/utils'
import {ISubject} from "../models/Subject";
import {RegisterStatus} from "./RegisterStatus";

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
    register_state_id?: RegisterStatus;
    timestamp?: number;
    subject?: ISubject;
    teachers: { id: string, displayName: string }[];
    notified: boolean;
    splitSlot: boolean;
    isOpenedByPersonnel: boolean;
    allowRegister?: boolean;
}

export class Course {
}

/**
 * A course is considered "forgotten" if its register is not DONE and its
 * start time (+ grace period) has already passed, regardless of whether a
 * register row exists yet (a course with no register at all is treated the
 * same as a TODO register).
 */
export function isCourseForgotten(course: Course): boolean {
    return course.register_state_id !== RegisterStatus.DONE
        && moment().isAfter(moment(course.startDate).add(15, 'm'));
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
            // The backend "forgotten_registers" filter only returns courses that already have a
            // register row: a course whose register was never created (e.g. not yet opened by the
            // teacher) is silently excluded, even when it is genuinely forgotten (cf. SUPPORT-5143).
            // We always fetch the full (unfiltered) course list and apply the "forgotten" predicate
            // ourselves below, which correctly covers courses without any register.
            const forgottenRegisterParam: string = `&forgotten_registers=false`;
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
            const fetchedCourses = Mix.castArrayAs(Course, data);
            fetchedCourses.forEach((course: Course) => course.subject.label = course.subject.name);
            // Infinite scroll relies on this flag to know whether more (raw) pages remain to fetch:
            // it must reflect the unfiltered page size, not the post-filter count, otherwise scroll
            // stops as soon as one page happens to contain no forgotten course.
            this.hasCourses = fetchedCourses.length > 0;
            const newCourses = forgottenRegisters ? fetchedCourses.filter(isCourseForgotten) : fetchedCourses;
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