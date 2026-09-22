jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));

import {http} from 'entcore-toolkit';
import {mockHttpResponse} from '@test-utils/httpMock';
import {punishmentService} from '@incidents/services';
import {IPunishmentAbsenceRequest, IPunishmentBody, IPunishmentRequest} from "@incidents/models";
import {User} from "@common/model/User";

let windowSpy;

describe('PunishmentService', () => {
    it('checks if the answers of the get with several types are correct', done => {
        const punishmentRequest: IPunishmentRequest = {
            end_at: "end_at",
            groups_ids: [],
            massmailed: false,
            page: 0,
            process_state: [],
            start_at: "start_at",
            structure_id: "structure_id",
            students_ids: [],
            type_ids: [1, 2]
        }

        const dataGraph = {
            all: [{type: {punishment_category_id: undefined}}]
        };
        const structureUrl: string = `?structure_id=structure_id`;
        const dateUrl: string = `&start_at=start_at&end_at=end_at`;
        const urlParams: string = `&type_id=1&type_id=2`;
        const pageUrl: string = `&page=0`;
        const url = `/incidents/punishments${structureUrl}${dateUrl}${urlParams}${pageUrl}`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(dataGraph, {url}));

        punishmentService.get(punishmentRequest)
            .then(response => {
                expect(http.get).toHaveBeenCalledWith(url);
                expect(response).toEqual(dataGraph);
                done();
            });
    });

    it('checks if the answers of the get with several students are correct', done => {
        const punishmentRequest: IPunishmentRequest = {
            end_at: "end_at",
            groups_ids: [],
            massmailed: false,
            page: 0,
            process_state: [],
            start_at: "start_at",
            structure_id: "structure_id",
            students_ids: ["students_id_1", "students_id_2"],
            type_ids: []
        }

        const dataGraph = {
            all: [{type: {punishment_category_id: undefined}}]
        };
        const structureUrl: string = `?structure_id=structure_id`;
        const dateUrl: string = `&start_at=start_at&end_at=end_at`;
        const urlParams: string = `&student_id=students_id_1&student_id=students_id_2`;
        const pageUrl: string = `&page=0`;
        const url = `/incidents/punishments${structureUrl}${dateUrl}${urlParams}${pageUrl}`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(dataGraph, {url}));

        punishmentService.get(punishmentRequest)
            .then(response => {
                expect(http.get).toHaveBeenCalledWith(url);
                expect(response).toEqual(dataGraph);
                done();
            });
    });

    it('checks if the answers of the get with several groups are correct', done => {
        const punishmentRequest: IPunishmentRequest = {
            end_at: "end_at",
            groups_ids: ["groups_id_1", "groups_id_2"],
            massmailed: false,
            page: 0,
            process_state: [],
            start_at: "start_at",
            structure_id: "structure_id",
            students_ids: [],
            type_ids: []
        }

        const dataGraph = {
            all: [{type: {punishment_category_id: undefined}}]
        };
        const structureUrl: string = `?structure_id=structure_id`;
        const dateUrl: string = `&start_at=start_at&end_at=end_at`;
        const urlParams: string = `&group_id=groups_id_1&group_id=groups_id_2`;
        const pageUrl: string = `&page=0`;
        const url = `/incidents/punishments${structureUrl}${dateUrl}${urlParams}${pageUrl}`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(dataGraph, {url}));

        punishmentService.get(punishmentRequest)
            .then(response => {
                expect(http.get).toHaveBeenCalledWith(url);
                expect(response).toEqual(dataGraph);
                done();
            });
    });

    it('checks if the answers of the get with several process are correct', done => {
        const punishmentRequest: IPunishmentRequest = {
            end_at: "end_at",
            groups_ids: [],
            massmailed: false,
            page: 0,
            process_state: [
                {label: "label1", isSelected: true, value: "value1"},
                {label: "label2", isSelected: false, value: "value2"},
                {label: "label3", isSelected: true, value: "value3"}],
            start_at: "start_at",
            structure_id: "structure_id",
            students_ids: [],
            type_ids: []
        }

        const dataGraph = {
            all: [{type: {punishment_category_id: undefined}}]
        };
        const structureUrl: string = `?structure_id=structure_id`;
        const dateUrl: string = `&start_at=start_at&end_at=end_at`;
        const urlParams: string = `&process=value1&process=value3`;
        const pageUrl: string = `&page=0`;
        const url = `/incidents/punishments${structureUrl}${dateUrl}${urlParams}${pageUrl}`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(dataGraph, {url}));

        punishmentService.get(punishmentRequest)
            .then(response => {
                expect(http.get).toHaveBeenCalledWith(url);
                expect(response).toEqual(dataGraph);
                done();
            });
    });

    it('checks if the answers of the get with several option are correct', done => {
        const punishmentRequest: IPunishmentRequest = {
            end_at: "end_at",
            groups_ids: ["groups_id_1", "groups_id_2"],
            massmailed: false,
            page: 0,
            process_state: [
                {label: "label1", isSelected: true, value: "value1"},
                {label: "label2", isSelected: false, value: "value2"},
                {label: "label3", isSelected: true, value: "value3"}],
            start_at: "start_at",
            structure_id: "structure_id",
            students_ids: ["students_id_1", "students_id_2"],
            type_ids: [1, 2],
            order: 'date',
            reverse: true
        };

        const dataGraph = {
            all: [{type: {punishment_category_id: undefined}}]
        };

        const structureUrl: string = `?structure_id=structure_id`;
        const dateUrl: string = `&start_at=start_at&end_at=end_at`;
        const urlParams: string = `&type_id=1&type_id=2&student_id=students_id_1&student_id=students_id_2&group_id=groups_id_1&group_id=groups_id_2&process=value1&process=value3`;
        const orderParams: string = `&order=date&reverse=true`;
        const pageUrl: string = `&page=0`;
        const url = `/incidents/punishments${structureUrl}${dateUrl}${urlParams}${orderParams}${pageUrl}`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(dataGraph, {url}));

        punishmentService.get(punishmentRequest)
            .then(response => {
                expect(http.get).toHaveBeenCalledWith(url);
                expect(response).toEqual(dataGraph);
                done();
            });
    });

    it('check create response is correct', done => {
        const punishmentBody: IPunishmentBody = {
            student_ids: []
        }

        const dataGraph = {
            dataGraph: {
                dataExample: "Example"
            }
        };
        const url = `/incidents/punishments`;
        (http.post as jest.Mock).mockResolvedValueOnce(mockHttpResponse(dataGraph, {
            url,
            method: 'post',
            data: JSON.stringify(punishmentBody)
        }));

        punishmentService.create(punishmentBody)
            .then(response => {
                expect(http.post).toHaveBeenCalledWith(url, punishmentBody);
                expect((response.config as any).data).toEqual(JSON.stringify(punishmentBody));
                expect(response.data).toEqual(dataGraph);
                expect(response.status).toEqual(200);
                expect((response.config as any).url).toEqual(`/incidents/punishments`);
                expect((response.config as any).method).toEqual(`post`);
                done();
            });
    });

    it('check update response is correct', done => {
        const punishmentBody: IPunishmentBody = {
            student_ids: []
        }

        const dataGraph = {
            dataGraph: {
                dataExample: "Example"
            }
        };
        const url = `/incidents/punishments`;
        (http.put as jest.Mock).mockResolvedValueOnce(mockHttpResponse(dataGraph, {
            url,
            method: 'put',
            data: JSON.stringify(punishmentBody)
        }));

        punishmentService.update(punishmentBody)
            .then(response => {
                expect(http.put).toHaveBeenCalledWith(url, punishmentBody);
                expect((response.config as any).data).toEqual(JSON.stringify(punishmentBody));
                expect(response.data).toEqual(dataGraph);
                expect(response.status).toEqual(200);
                expect((response.config as any).url).toEqual(`/incidents/punishments`);
                expect((response.config as any).method).toEqual(`put`);
                done();
            });
    });

    it('check delete response is correct', done => {
        const punishmentId: string = 'punishmentId';
        const structureId: string = 'structureId';

        let dataGraph = {
            dataGraph: {
                dataExample: "Example"
            }
        };
        const url = `/incidents/punishments?id=${punishmentId}&structureId=${structureId}`;
        (http.delete as jest.Mock).mockResolvedValueOnce(mockHttpResponse(dataGraph, {url, method: 'delete'}));

        punishmentService.delete(punishmentId, structureId)
            .then(response => {
                expect(http.delete).toHaveBeenCalledWith(url);
                expect(response.status).toEqual(200);
                expect(response.data).toEqual(dataGraph);
                expect((response.config as any).url).toEqual(`/incidents/punishments?id=${punishmentId}&structureId=${structureId}`);
                expect((response.config as any).method).toEqual(`delete`);
                done();
            });
    });

    it('check deleteGroupedPunishment response is correct', done => {
        const groupPunishmentId: string = 'groupPunishmentId';
        const structureId: string = 'structureId';

        let dataGraph = {
            dataGraph: {
                dataExample: "Example"
            }
        };
        const url = `/incidents/punishments?grouped_punishment_id=${groupPunishmentId}&structureId=${structureId}`;
        (http.delete as jest.Mock).mockResolvedValueOnce(mockHttpResponse(dataGraph, {url, method: 'delete'}));

        punishmentService.deleteGroupedPunishment(groupPunishmentId, structureId)
            .then(response => {
                expect(http.delete).toHaveBeenCalledWith(url);
                expect(response.status).toEqual(200);
                expect(response.data).toEqual(dataGraph);
                expect((response.config as any).url).toEqual(`/incidents/punishments?grouped_punishment_id=${groupPunishmentId}&structureId=${structureId}`);
                expect((response.config as any).method).toEqual(`delete`);
                done();
            });
    });

    it('check exportCSV response is correct', done => {
        windowSpy = jest.spyOn(window, "open");
        windowSpy.mockImplementation(() => ({
            open: jest.fn()
        }));

        const punishmentRequest: IPunishmentRequest = {
            end_at: "end_at",
            groups_ids: ["groups_id_1", "groups_id_2"],
            massmailed: false,
            page: 0,
            process_state: [
                {label: "label1", isSelected: true, value: "value1"},
                {label: "label2", isSelected: false, value: "value2"},
                {label: "label3", isSelected: true, value: "value3"}],
            start_at: "start_at",
            structure_id: "structure_id",
            students_ids: ["students_id_1", "students_id_2"],
            type_ids: [1, 2],
            order: 'date',
            reverse: false
        };

        punishmentService.exportCSV(punishmentRequest);
        const structureUrl: string = `?structure_id=structure_id`;
        const dateUrl: string = `&start_at=start_at&end_at=end_at`;
        const orderParams: string = `&order=date`;
        const urlParams: string = `&type_id=1&type_id=2&student_id=students_id_1&student_id=students_id_2&group_id=groups_id_1&group_id=groups_id_2`;
        expect(window.open).toHaveBeenCalledWith(`/incidents/punishments/export${structureUrl}${dateUrl}${urlParams}${orderParams}`);
        windowSpy.mockRestore();
        done();
    });

    it('check getStudentsAbsences response is correct', done => {
        const start_at = "start_at";
        const end_at = "end_at";
        const student_ids: Array<User> = [{id: "id_1"}, {id: "id_2"}, {id: "id_3"}];

        const punishment : IPunishmentAbsenceRequest = {
            studentIds: ["id_1", "id_2", "id_3"],
                startAt: start_at,
            endAt: end_at
        } as IPunishmentAbsenceRequest

        const data = {
            all: ["id_1", "id_2"]
        };
        const url = `/incidents/punishments/students/absences`;
        (http.post as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url, method: 'post'}));

        punishmentService.getStudentsAbsences(student_ids, start_at, end_at)
            .then(response => {
                expect(http.post).toHaveBeenCalledWith(url, punishment);
                expect(response).toEqual(data.all);
                done();
            });
    });
});
