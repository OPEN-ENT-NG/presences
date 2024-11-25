import {Student} from "@common/model/Student";
import {Group} from "@common/services";

export type EventListCalendarFilter = {
    startDate: Date;
    endDate: Date;
    students: Student[];
    classes: Group[];
}