import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import {absenceService} from "../AbsenceService";

describe('AbsenceService', () => {
    it('returns data when getAbsenceMarkers request is correctly called', done => {
        const mock = new MockAdapter(axios);
        const structureId = 'structure';
        const start = 'start';
        const end = 'end';
        const data = {response: true};
        mock.onGet(`/presences/structures/${structureId}/absences/markers?startAt=${start}&endAt=${end}`)
            .reply(200, data);

        absenceService.getAbsenceMarkers(structureId, start, end).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });
});
