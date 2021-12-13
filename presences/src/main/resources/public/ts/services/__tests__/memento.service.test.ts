import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import {MementoService, IEventSummary, IPeriodSummary} from '../MementoService';
describe('MementoService', () => {
    it('returns data when retrieve request is correctly called whit no type', done => {

        const student: string = 'student';
        const structure: string = 'structure';
        const start: string = 'start';
        const end: string = 'end';
        const mock = new MockAdapter(axios);
        const eventSummary: IEventSummary = {month: 0, types: {
                UNREGULARIZED: 0,
                REGULARIZED: 1,
                NO_REASON: 2,
                LATENESS: 3,
                DEPARTURE: 4
            }};
        const data: IPeriodSummary = {absence_rate: 0, months: [eventSummary]};

        mock.onGet(`/presences/memento/students/${student}/absences/summary?structure=${structure}&start=${start}&end=${end}`)
            .reply(200, data);


        MementoService.getStudentEventsSummary("student", "structure","start","end", []).then(response => {
            expect(response).toEqual({absence_rate: 0, months: []});
            done();
        });
    });

    it('returns data when retrieve request is correctly called whit all type', done => {

        const student: string = 'student';
        const structure: string = 'structure';
        const start: string = 'start';
        const end: string = 'end';
        const typeArray = ['UNREGULARIZED', 'REGULARIZED', 'NO_REASON', 'LATENESS', 'DEPARTURE'];
        const typeUrlFilter = '&type=UNREGULARIZED&type=REGULARIZED&type=NO_REASON&type=LATENESS&type=DEPARTURE';
        const mock = new MockAdapter(axios);
        const eventSummary: IEventSummary = {month: 0, types: {
                UNREGULARIZED: 0,
                REGULARIZED: 1,
                NO_REASON: 2,
                LATENESS: 3,
                DEPARTURE: 4
            }};
        const data: IPeriodSummary = {absence_rate: 0, months: [eventSummary]};

        mock.onGet(`/presences/memento/students/${student}/absences/summary?structure=${structure}&start=${start}&end=${end}${typeUrlFilter}`)
            .reply(200, data);


        MementoService.getStudentEventsSummary("student", "structure","start","end", typeArray).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when retrieve request is correctly called whit some type', done => {

        const student: string = 'student';
        const structure: string = 'structure';
        const start: string = 'start';
        const end: string = 'end';
        const typeArray = ['UNREGULARIZED', 'REGULARIZED', 'DEPARTURE'];
        const typeUrlFilter = '&type=UNREGULARIZED&type=REGULARIZED&type=DEPARTURE';
        const mock = new MockAdapter(axios);
        const eventSummary: IEventSummary = {month: 0, types: {
                UNREGULARIZED: 0,
                REGULARIZED: 1,
                NO_REASON: 2,
                LATENESS: 3,
                DEPARTURE: 4
            }};
        const data: IPeriodSummary = {absence_rate: 0, months: [eventSummary]};

        mock.onGet(`/presences/memento/students/${student}/absences/summary?structure=${structure}&start=${start}&end=${end}${typeUrlFilter}`)
            .reply(200, data);

        data.months[0].types = {
            UNREGULARIZED: 0,
            REGULARIZED: 1,
            DEPARTURE: 4
        };
        MementoService.getStudentEventsSummary("student", "structure","start","end", typeArray).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });
});
