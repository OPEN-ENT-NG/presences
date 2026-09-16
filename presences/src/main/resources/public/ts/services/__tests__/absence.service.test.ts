jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));

import {http} from 'entcore-toolkit';
import {mockHttpResponse} from '@test-utils/httpMock';
import {absenceService} from "../AbsenceService";

describe('AbsenceService', () => {
    it('returns data when getAbsenceMarkers request is correctly called', done => {
        const structureId = 'structure';
        const start = 'start';
        const end = 'end';
        const data = {response: true};
        const url = `/presences/structures/${structureId}/absences/markers?startAt=${start}&endAt=${end}`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        absenceService.getAbsenceMarkers(structureId, start, end).then(response => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(response).toEqual(data);
            done();
        });
    });
});
