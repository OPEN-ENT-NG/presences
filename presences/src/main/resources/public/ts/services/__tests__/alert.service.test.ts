jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));

import {http} from 'entcore-toolkit';
import {mockHttpResponse} from '@test-utils/httpMock';
import {alertService} from "@presences/services";
import {DeleteAlertRequest, StudentAlert} from "@presences/models/Alert";

describe('AlertService', () => {
    it('returns data when reset request is correctly called', done => {
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
        const url = `/presences/structures/structure_id/alerts`;
        (http.delete as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url, method: 'delete'}));

        alertService.reset(structure_id, body).then(response => {
            expect(http.delete).toHaveBeenCalledWith(url, {data: body});
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when getAlerts request is correctly called', done => {
        const structureId: string = "structure_id";
        const data = {response: true};
        const url = `/presences/structures/${structureId}/alerts/summary`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        alertService.getAlerts(structureId).then(response => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when getStudentsAlerts request is correctly called', done => {
        const structureId: string = "structure_id";
        const types: string[] = ["type1", "type2"];
        const students: string[] = ["student1", "student2"];
        const groups: string[] = ["group1", "group2"];
        const start_at: string = "start_at";
        const end_at: string = "end_at";
        const data = {response: true};
        const url = `/presences/structures/structure_id/alerts?student=student1&student=student2&class=group1&class=group2&start_at=start_at&end_at=end_at&&type=type1&type=type2`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        alertService.getStudentsAlerts(structureId, types, students, groups, start_at, end_at).then(response => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when getStudentAlerts request is correctly called', done => {
        const structureId: string = "structure_id";
        const studentId: string = "studentId";
        const type: string = "type";
        const data = {response: true};
        const url = `/presences/structures/${structureId}/students/${studentId}/alerts?type=${type}`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        alertService.getStudentAlerts(structureId, studentId, type).then(response => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when resetStudentAlertsCount request is correctly called', done => {
        const structureId: string = "structure_id";
        const studentId: string = "studentId";
        const type: string = "type";
        const data = {response: true};
        const url = `/presences/structures/${structureId}/students/${studentId}/alerts/reset?type=${type}`;
        (http.delete as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url, method: 'delete'}));

        alertService.resetStudentAlertsCount(structureId, studentId, type).then(response => {
            expect(http.delete).toHaveBeenCalledWith(url);
            expect(response.data).toEqual(data);
            done();
        });
    });
});
