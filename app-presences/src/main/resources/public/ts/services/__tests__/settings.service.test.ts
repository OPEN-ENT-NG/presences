jest.mock('entcore', () => ({
    ng: {service: jest.fn()}
}))

import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import {Setting, settingService} from '../SettingsService';
describe('SettingsService', () => {
    it('returns data when retrieve request is correctly called', done => {

        const structureId: string = 'structureId';
        const mock = new MockAdapter(axios);
        const data = {response: true};

        mock.onGet(`/presences/structures/${structureId}/settings`)
            .reply(200, data);


        settingService.retrieve("structureId").then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('returns data when retrieve multiple slot request is correctly called', done => {

        const structureId: string = 'structureId';
        const mock = new MockAdapter(axios);
        const data: Setting = {"allow_multiple_slots": true};

        mock.onGet(`/presences/structures/${structureId}/settings/multiple-slots`)
            .reply(200, data);


        settingService.retrieveMultipleSlotSetting("structureId").then(response => {
            expect(response).toEqual(data.allow_multiple_slots);
            done();
        });
    });

    it('returns data when put request is correctly called', done => {

        const structureId: string = 'structureId';
        const mock = new MockAdapter(axios);
        const data = {response: true};

        mock.onPut(`/presences/structures/${structureId}/settings`)
            .reply(200, data);

        settingService.put("structureId", {}).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });
});
