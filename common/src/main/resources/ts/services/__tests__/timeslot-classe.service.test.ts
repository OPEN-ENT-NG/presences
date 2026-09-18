jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));

import {http} from 'entcore-toolkit';
import {mockHttpResponse} from '@test-utils/httpMock';
import {timeslotClasseService, TimeslotClasseService} from "@common/services/TimeslotClasseService";
import {IStructureSlot} from "@common/model";

describe('TimeslotClasseService', () => {
    it('returns data when API request is correctly called for getAudienceTimeslot method', done => {
        const audienceId = "audienceId";
        const data: IStructureSlot = {_id: "", name: "", slots: []};
        const url = `/viescolaire/timeslot/audience/${audienceId}`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        timeslotClasseService.getAudienceTimeslot("audienceId").then(response => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when API request is correctly called for getAllClassFromTimeslot method', done => {
        const timeslotId = "timeslotId";
        const data = ["ok"];
        const url = `/viescolaire/timeslot/${timeslotId}`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        timeslotClasseService.getAllClassFromTimeslot("timeslotId").then(response => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when API request is correctly called for createOrUpdateClassTimeslot method', done => {
        const timeslotId = "timeslotId";
        const classId = "classId";
        const data = {response: true};
        const url = `/viescolaire/timeslot/audience`;
        const body = {timeslot_id: timeslotId, class_id: classId};
        (http.post as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {
            url,
            method: 'post',
            data: JSON.stringify(body)
        }));

        timeslotClasseService.createOrUpdateClassTimeslot("timeslotId", "classId").then(response => {
            expect(http.post).toHaveBeenCalledWith(url, body);
            expect(response.data).toEqual(data);
            expect((response.config as any).url).toEqual(`/viescolaire/timeslot/audience`);
            expect((response.config as any).data).toEqual(JSON.stringify({timeslot_id: timeslotId, class_id: classId}));
            done();
        });
    });

    it('returns data when API request is correctly called for deleteAllAudienceFromTimeslot method', done => {
        const timeslotId = "timeslotId";
        const data = {response: true};
        const url = `/viescolaire/timeslot/${timeslotId}`;
        (http.delete as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url, method: 'delete'}));

        timeslotClasseService.deleteAllAudienceFromTimeslot("timeslotId").then(response => {
            expect(http.delete).toHaveBeenCalledWith(url);
            expect(response.data).toEqual(data);
            expect((response.config as any).url).toEqual(`/viescolaire/timeslot/${timeslotId}`);
            done();
        });
    });

    it('returns data when API request is correctly called for getAudienceTimeslot method', done => {
        const classId = "classId";
        const data = {response: true};
        const url = `/viescolaire/timeslot/audience/${classId}`;
        (http.delete as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url, method: 'delete'}));

        timeslotClasseService.deleteClassTimeslot("classId").then(response => {
            expect(http.delete).toHaveBeenCalledWith(url);
            expect(response.data).toEqual(data);
            expect((response.config as any).url).toEqual(`/viescolaire/timeslot/audience/${classId}`);
            done();
        });
    });
});
