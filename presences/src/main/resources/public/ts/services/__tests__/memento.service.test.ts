jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));

import {http} from 'entcore-toolkit';
import {mockHttpResponse} from '@test-utils/httpMock';
import {MementoService} from '../MementoService';
import {IndicatorBody} from "@statistics/model/Indicator";
import {GlobalResponse} from "@statistics/model/Global";
import {IMonthlyGraph, MonthlyStats} from "@statistics/model/Monthly";

describe('MementoService', () => {
    it('returns data when retrieve request is correctly called', done => {
        const student: string = 'student';
        const structure: string = 'structure';
        const start: string = 'start';
        const end: string = 'end';
        const body: IndicatorBody = {
            start: start,
            end: end,
            audiences: [],
            filters: {},
            punishmentTypes: [],
            reasons: [30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, -2],
            sanctionTypes: [],
            types: [],
            users: [student]
        }
        const dataGlobal: GlobalResponse = {
            count: undefined,
            data: undefined,
            rate: {ABSENCE_TOTAL: 10},
            slots: undefined
        };
        const url = `/presences/statistics/structures/${structure}/student/${student}`;
        (http.post as jest.Mock).mockResolvedValueOnce(mockHttpResponse(dataGlobal, {url, method: 'post'}));

        MementoService.getStudentEventsSummary('structure', 'student', body).then(response => {
            expect(http.post).toHaveBeenCalledWith(url, body);
            expect(response).toEqual(dataGlobal);
            done();
        });
    });

    it('returns data when retrieve request is correctly called', done => {
        const student: string = 'student';
        const structure: string = 'structure';
        const start: string = 'start';
        const end: string = 'end';
        const body: IndicatorBody = {
            start: start,
            end: end,
            audiences: [],
            filters: {},
            punishmentTypes: [],
            reasons: [30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, -2],
            sanctionTypes: [],
            types: [],
            users: [student]
        }
        const regularized: Array<MonthlyStats> = [{"2020-01": {count: 1}}];
        regularized.push({"2020-04": {count: 0}});
        const no_reason: Array<MonthlyStats> = [{"2020-01": {count: 2}}];
        no_reason.push({"2020-04": {count: 20}});
        const lateness: Array<MonthlyStats> = [{"2020-01": {count: 3}}];
        lateness.push({"2020-04": {count: 0}});
        const departure: Array<MonthlyStats> = [{"2020-01": {count: 4}}];
        departure.push({"2020-04": {count: 0}});
        const dataGraph: IMonthlyGraph = {
            data: {
                REGULARIZED: regularized,
                NO_REASON: no_reason,
                LATENESS: lateness,
                DEPARTURE: departure
            },
            months: ["2020-01", "2020-04"]
        };
        const url = `/presences/statistics/structures/${structure}/student/${student}/graph`;
        (http.post as jest.Mock).mockResolvedValueOnce(mockHttpResponse(dataGraph, {url, method: 'post'}));

        MementoService.getStudentEventsSummaryGraph('structure', 'student', body).then(response => {
            expect(http.post).toHaveBeenCalledWith(url, body);
            expect(response).toEqual(dataGraph);
            done();
        });
    });
});
