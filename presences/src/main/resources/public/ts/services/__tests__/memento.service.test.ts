import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
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
        const mock = new MockAdapter(axios);
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
        mock.onPost(`/presences/statistics/structures/${structure}/student/${student}`, body)
            .reply(200, dataGlobal);

        MementoService.getStudentEventsSummary('structure', 'student', body).then(response => {
            expect(response).toEqual(dataGlobal);
            done();
        });
    });

    it('returns data when retrieve request is correctly called', done => {
        const student: string = 'student';
        const structure: string = 'structure';
        const start: string = 'start';
        const end: string = 'end';
        const mock = new MockAdapter(axios);
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

        mock.onPost(`/presences/statistics/structures/${structure}/student/${student}/graph`, body)
            .reply(200, dataGraph);

        MementoService.getStudentEventsSummaryGraph('structure', 'student', body).then(response => {
            expect(response).toEqual(dataGraph);
            done();
        });
    });
});
