jest.mock('entcore', () => ({
    ng: {service: jest.fn()}
}))

jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));

import {http} from 'entcore-toolkit';
import {mockHttpResponse} from '@test-utils/httpMock';
import {Setting, settingService} from '../SettingsService';
describe('SettingsService', () => {
    it('returns data when retrieve request is correctly called', done => {

        const structureId: string = 'structureId';
        const data = {response: true};
        const url = `/presences/structures/${structureId}/settings`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        settingService.retrieve("structureId").then(response => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when retrieve multiple slot request is correctly called', done => {

        const structureId: string = 'structureId';
        const data: Setting = {"allow_multiple_slots": true};
        const url = `/presences/structures/${structureId}/settings/multiple-slots`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        settingService.retrieveMultipleSlotSetting("structureId").then(response => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(response).toEqual(data.allow_multiple_slots);
            done();
        });
    });

    it('returns data when put request is correctly called', done => {

        const structureId: string = 'structureId';
        const data = {response: true};
        const url = `/presences/structures/${structureId}/settings`;
        (http.put as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url, method: 'put'}));

        settingService.put("structureId", {}).then(response => {
            expect(http.put).toHaveBeenCalledWith(url, {});
            expect(response).toEqual(data);
            done();
        });
    });
});
