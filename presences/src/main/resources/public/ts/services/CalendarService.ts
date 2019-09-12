import {moment, ng} from 'entcore'
import http from 'axios';
import {EventType} from '../models';
import {User} from '@common/model/User'

export interface CourseEvent {
    id: number;
    start_date: string;
    end_date: string;
    type_id: number;
    reason_id?: number;
}

export interface Course {
    _id: string;
    structureId: string;
    subjectId: string;
    classes: string[];
    groups: string[];
    roomLabels: string[];
    dayOfWeek: number;
    startDate: string;
    endDate: string;
    is_recurrent: boolean;
    subject_name: string;
    events: CourseEvent[];
    hash?: string;
    absence?: boolean;
    absenceId?: string;
    absenceReason?: number;
}

export interface CalendarService {
    getCourses(structureId: string, user: string, start: string, end: string): Promise<Array<Course>>;

    getStudentsGroup(id: string): Promise<Array<User>>;
}

export const CalendarService = ng.service('CalendarService', (): CalendarService => ({
    getCourses: async (structureId: string, user: string, start: string, end: string) => {
        function containsAbsence(course: Course) {
            let contains = false;
            course.events.map((event) => contains = contains ||
                (event.type_id === EventType.ABSENCE && (event.reason_id === null || event.reason_id === 0)));
            return contains;
        }

        function containsReasonAbsence(course: Course) {
            let contains = false;
            course.events.map((event) => contains = contains ||
                (event.type_id === EventType.ABSENCE && (event.reason_id !== null || event.reason_id > 0)));
            return contains;
        }

        try {
            const {data} = await http.get(`/presences/calendar/courses?structure=${structureId}&start=${start}&end=${end}&user=${user}`);
            data.map((course) => {
                course.startMoment = moment(course.startDate);
                course.endMoment = moment(course.endDate);
                course.containsAbsence = containsAbsence(course);
                course.containsReasonAbsence = containsReasonAbsence(course);
                this.locked = true;

                // create hash to fetch in html in order to recognize "absence" course
                if (course.absence) course.hash = Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
            });
            return data;
        } catch (err) {
            throw err;
        }
    },
    getStudentsGroup: async (id) => {
        try {
            const {data} = await http.get(`/presences/calendar/groups/${id}/students`);
            data.forEach((user) => user.toString = () => user.displayName);
            return data;
        } catch (err) {
            throw err;
        }
    }
}));

