import MockAdapter from "axios-mock-adapter";
import axios from "axios";
import {massmailingService} from "@massmailing/services";
import {MassmailingStudent} from "@massmailing/model";

describe('SettingsService', () => {
    it('test getStatus method', done => {
        const mock = new MockAdapter(axios);
        const data = {response: true};
        const structure: string = "structureId";
        const massmailed: boolean = false;
        const reasons: number[] = [1, 2];
        const punishmentTypes: Array<number> = [3, 4];
        const sanctionsTypes: Array<number> = [5, 6];
        const start_at: number = 7;
        const end_date: Date = new Date("2022-04-13");
        const start_date: Date = new Date("2022-04-12");
        const groups: string[] = ["group1", "group2"];
        const students: string[] = ["student1", "student2"];
        const types: Array<String> = ["type1", "type2"];
        const noReasons: boolean = false;
        const noLatenessReasons: boolean = true;

        mock.onGet('/massmailing/massmailings/status?structure=structureId' +
            '&start_at=7' +
            '&start_date=2022-04-12' +
            '&end_date=2022-04-13' +
            '&no_reasons=false' +
            '&no_lateness_reasons=true' +
            '&reason=1' +
            '&reason=2' +
            '&punishmentType=3' +
            '&punishmentType=4' +
            '&sanctionType=5' +
            '&sanctionType=6' +
            '&group=group1' +
            '&group=group2' +
            '&student=student1' +
            '&student=student2' +
            '&type=type1' +
            '&type=type2' +
            '&massmailed=false')
            .reply(200, data);
        massmailingService.getStatus(structure, massmailed, reasons, punishmentTypes,
            sanctionsTypes, start_at, start_date, end_date,
            groups, students, types, noReasons, noLatenessReasons).then(response => {
            expect(response).toEqual(data);
            done();
        })
    });

    it('test getAnomalies method', done => {
        const mock = new MockAdapter(axios);
        const data = {response: true};
        const structure: string = "structureId";
        const massmailed: boolean = false;
        const reasons: number[] = [1, 2];
        const punishmentTypes: Array<number> = [3, 4];
        const sanctionsTypes: Array<number> = [5, 6];
        const start_at: number = 7;
        const end_date: Date = new Date("2022-04-13");
        const start_date: Date = new Date("2022-04-12");
        const groups: string[] = ["group1", "group2"];
        const students: string[] = ["student1", "student2"];
        const types: Array<String> = ["type1", "type2"];
        const noReasons: boolean = false;
        const noLatenessReasons: boolean = true;

        mock.onGet('/massmailing/massmailings/anomalies?structure=structureId' +
            '&start_at=7' +
            '&start_date=2022-04-12' +
            '&end_date=2022-04-13' +
            '&no_reasons=false' +
            '&no_lateness_reasons=true' +
            '&reason=1' +
            '&reason=2' +
            '&punishmentType=3' +
            '&punishmentType=4' +
            '&sanctionType=5' +
            '&sanctionType=6' +
            '&group=group1' +
            '&group=group2' +
            '&student=student1' +
            '&student=student2' +
            '&type=type1' +
            '&type=type2' +
            '&massmailed=false')
            .reply(200, data);
        massmailingService.getAnomalies(structure, massmailed, reasons, punishmentTypes,
            sanctionsTypes, start_at, start_date, end_date,
            groups, students, types, noReasons, noLatenessReasons).then(response => {
            expect(response).toEqual(data);
            done();
        })
    });

    it('test prefetch method', done => {
        const mock = new MockAdapter(axios);
        const student1: MassmailingStudent = {
            className: "",
            displayName: "",
            events: {},
            id: "",
            opened: false,
            relative: [],
            selected: false
        };
        const data = {type: "type", counts: 2, students: [student1]};
        const mailType = "mailType";
        const structure: string = "structureId";
        const massmailed: boolean = false;
        const reasons: number[] = [1, 2];
        const punishmentTypes: Array<number> = [3, 4];
        const sanctionsTypes: Array<number> = [5, 6];
        const start_at: number = 7;
        const end_date: Date = new Date("2022-04-13");
        const start_date: Date = new Date("2022-04-12");
        const groups: string[] = ["group1", "group2"];
        const students: string[] = ["student1", "student2"];
        const types: Array<String> = ["type1", "type2"];
        const noReasons: boolean = false;
        const noLatenessReasons: boolean = true;

        mock.onGet('/massmailing/massmailings/prefetch/mailType?structure=structureId' +
            '&start_at=7' +
            '&start_date=2022-04-12' +
            '&end_date=2022-04-13' +
            '&no_reasons=false' +
            '&no_lateness_reasons=true' +
            '&reason=1' +
            '&reason=2' +
            '&punishmentType=3' +
            '&punishmentType=4' +
            '&sanctionType=5' +
            '&sanctionType=6' +
            '&group=group1' +
            '&group=group2' +
            '&student=student1' +
            '&student=student2' +
            '&type=type1' +
            '&type=type2' +
            '&massmailed=false')
            .reply(200, data);
        massmailingService.prefetch(mailType, structure, massmailed, reasons, punishmentTypes,
            sanctionsTypes, start_at, start_date, end_date,
            groups, students, types, noReasons, noLatenessReasons).then(response => {
            expect(response).toEqual(data);
            done();
        })
    });
});