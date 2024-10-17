import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import {alertService} from "@presences/services";
import {DeleteAlertRequest, StudentAlert} from "@presences/models/Alert";

describe('AlertService', () => {
    it('returns data when reset request is correctly called', done => {
        const mock = new MockAdapter(axios);
        const deleted_alert: Array<StudentAlert> = [{
            student_id: "student_id",
            type: "type",
            count: 3,
            name: "name",
            audience: "audience",
            selected: false}]
        const data = {response: true};
        const body : DeleteAlertRequest = {
            start_at: "start_at",
            end_at: "end_at",
            deleted_alert: deleted_alert
        };
        const structure_id: string = "structure_id";
        mock.onDelete(`/presences/structures/structure_id/alerts`, {data: body})
            .reply(200, data);

        alertService.reset(structure_id, body).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when getAlerts request is correctly called', done => {
        const mock = new MockAdapter(axios);
        const structureId: string = "structure_id";
        const data = {response: true};
        mock.onGet(`/presences/structures/${structureId}/alerts/summary`)
            .reply(200, data);

        alertService.getAlerts(structureId).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when getStudentsAlerts request is correctly called', done => {
        const mock = new MockAdapter(axios);
        const structureId: string = "structure_id";
        const types: string[] = ["type1", "type2"];
        const students: string[] = ["student1", "student2"];
        const groups: string[] = ["group1", "group2"];
        const start_at: string = "start_at";
        const end_at: string = "end_at";
        const data = {response: true};
        mock.onGet(`/presences/structures/structure_id/alerts?student=student1&student=student2&class=group1&class=group2&start_at=start_at&end_at=end_at&&type=type1&type=type2`)
            .reply(200, data);

        alertService.getStudentsAlerts(structureId, types, students, groups, start_at, end_at).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when getStudentAlerts request is correctly called', done => {
        const mock = new MockAdapter(axios);
        const structureId: string = "structure_id";
        const studentId: string = "studentId";
        const type: string = "type";
        const data = {response: true};
        mock.onGet(`/presences/structures/${structureId}/students/${studentId}/alerts?type=${type}`)
            .reply(200, data);

        alertService.getStudentAlerts(structureId, studentId, type).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when resetStudentAlertsCount request is correctly called', done => {
        const mock = new MockAdapter(axios);
        const structureId: string = "structure_id";
        const studentId: string = "studentId";
        const type: string = "type";
        const data = {response: true};
        mock.onDelete(`/presences/structures/${structureId}/students/${studentId}/alerts/reset?type=${type}`)
            .reply(200, data);

        alertService.resetStudentAlertsCount(structureId, studentId, type).then(response => {
            expect(response.data).toEqual(data);
            done();
        });
    });
});
