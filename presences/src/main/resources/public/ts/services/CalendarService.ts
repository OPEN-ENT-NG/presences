import {moment, ng} from 'entcore'
import http from 'axios';
import {Absence, EventType, PresenceRequest, Presences} from '../models';
import {IPunishment} from '@incidents/models';
import {User} from '@common/model/User'
import {DateUtils} from "@common/utils";
import {presenceService} from "../services/PresenceService";
import {absenceService} from "../services/AbsenceService";
import {EventsUtils} from "../utilities";

export interface CourseEvent {
    id: number;
    start_date: string;
    end_date: string;
    type_id: number;
    reason_id?: number;
    counsellor_input?: boolean;
    counsellor_regularisation?: boolean;
    followed?: boolean;
    comment?: string;
}

export interface CourseIncident {
    description: string;
    date: Date;
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
    startMomentTime?: string;
    endDate: string;
    endMomentTime?: string;
    subject_name: string;
    subjectName?: string;
    teachers?: Array<{ id: string, displayName: string }>;
    events: CourseEvent[];
    splitCourses: Course[];
    hash?: string;
    absence?: boolean;
    absences?: Array<Absence>;
    incident?: CourseIncident;
    punishments?: IPunishment[];
    absenceId?: string;
    absenceReason?: number;
    eventId?: number;
    containsAbsence?: Boolean;
    containsReasonAbsence?: Boolean;
}

export interface CalendarService {
    getStudentsGroup(id: string): Promise<Array<User>>;

    loadCourses(student: User, startWeekDate: string, structureId: string): Promise<Array<Course>>;

    loadPresences(student: User, startWeekDate: string, structureId: string): Promise<Presences>;

    loadAbsences(student: User, startWeekDate: string, structureId: string): Promise<Array<Absence>>;
}

export const calendarService = ng.service('CalendarService', (): CalendarService => ({
    loadCourses: async (student: User, startWeekDate: string, structureId): Promise<Array<Course>> => {
        const start = DateUtils.format(startWeekDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]);
        const end = DateUtils.format(DateUtils.add(startWeekDate, 1, 'w'), DateUtils.FORMAT["YEAR-MONTH-DAY"]);

        function containsAbsence(course: Course): boolean {
            let contains = false;
            course.events.map((event) => contains = contains ||
                (event.type_id === EventType.ABSENCE && (event.reason_id === null || event.reason_id === 0)));
            return contains;
        }

        function containsReasonAbsence(course: Course): boolean {
            let contains = false;
            course.events.map((event) => contains = contains ||
                (event.type_id === EventType.ABSENCE && (event.reason_id !== null || event.reason_id > 0)));
            return contains;
        }

        function buildCalendarCourses(data): Array<Course> {
            let dataModel = data;
            dataModel.forEach(itemData => {
                for (const k in itemData) {
                    if (itemData.hasOwnProperty(k) && !itemData.hasOwnProperty("_id")) {
                        itemData._id = itemData.id;
                        itemData.is_periodic = itemData.periodic;
                        itemData.subject_name = itemData.subjectName;
                    }
                }
            });
            return dataModel;
        }

        try {
            const {data} = await http.get(`/presences/calendar/courses?structure=${structureId}&start=${start}&end=${end}&user=${student.id}&_t=${moment().format(DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"])}`);
            data.map((course) => {
                course.absences = [];
                course.startMoment = moment(course.startDate);
                course.endMoment = moment(course.endDate);
                course.containsAbsence = containsAbsence(course);
                course.containsReasonAbsence = containsReasonAbsence(course);
                course.events = course.events.sort((a, b) => {
                    return moment(a.start_date) - moment(b.start_date);
                });
                this.locked = true;
            });
            return buildCalendarCourses(data);
        } catch (err) {
            throw err;
        }
    },

    loadPresences: async (student: User, startWeekDate: string, structureId): Promise<Presences> => {
        let presences = new Presences(structureId);
        let presencesRequest: PresenceRequest = {
            structureId: structureId,
            startDate: DateUtils.format(startWeekDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
            endDate: DateUtils.format(DateUtils.add(startWeekDate, 1, 'w'), DateUtils.FORMAT["YEAR-MONTH-DAY"]),
            studentIds: [student.id]
        } as PresenceRequest;
        await presences.build(await presenceService.get(presencesRequest));
        return presences
    },

    loadAbsences: async (student: User, startWeekDate: string, structureId): Promise<Array<Absence>> => {
        let absences = await absenceService.getAbsence(
            structureId,
            [student.id],
            DateUtils.format(startWeekDate, DateUtils.FORMAT["YEAR-MONTH-DAY"]),
            DateUtils.format(DateUtils.add(startWeekDate, 1, 'w'), DateUtils.FORMAT["YEAR-MONTH-DAY"]),
            null,
            null,
            null,
        );
        return absences.map((absence) => {
            absence.start_date = DateUtils.format(moment(absence.start_date), DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
            absence.end_date = DateUtils.format(moment(absence.end_date), DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]);
            absence.type = EventsUtils.ALL_EVENTS.absence;
            return absence;
        });
    },

    getStudentsGroup: async (id) => {
        try {
            const {data} = await http.get(`/presences/calendar/groups/${id}/students`);
            data.forEach((user) => user.toString = () => user.displayName);
            return data;
        } catch (err) {
            throw err;
        }
    },

}));

