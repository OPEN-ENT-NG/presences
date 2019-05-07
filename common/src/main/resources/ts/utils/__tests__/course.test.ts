import {moment} from 'entcore';
import {CourseUtils, DateUtils} from '../index'

const course = {
    "_id": "d28ab187-dffa-4cdc-bee4-37c5aa1f484b",
    "structureId": "5c04e497-cb43-4589-8332-16cc8a873920",
    "subjectId": "a494d671-77f2-4fd6-9a04-2035dfe736b7",
    "teacherIds": [
        "a25cd679-b30b-4701-8c60-231cdc30cdf2"
    ],
    "classes": [
        "5E 4"
    ],
    "groups": [],
    "roomLabels": [
        ""
    ],
    "dayOfWeek": 3,
    "startDate": DateUtils.format(moment().startOf('hour'), DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
    "endDate": DateUtils.format(moment().endOf('hour'), DateUtils.FORMAT["YEAR-MONTH-DAY-HOUR-MIN-SEC"]),
    "subjectName": "MATHS"
};

const wrongCourse = {...course, startDate: '1999-04-16 18:15:00', endDate: '1999-04-16 19:15:00'};

describe('isCurrentCourse', () => {
    test('It should be the current course', () => {
        expect(CourseUtils.isCurrentCourse(course)).toBeTruthy();
    });

    test('It should not be the current course', () => {
        expect(CourseUtils.isCurrentCourse(wrongCourse)).toBeFalsy();
    });
});