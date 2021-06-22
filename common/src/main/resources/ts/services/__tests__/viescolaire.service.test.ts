// tricks to fake "mock" entcore ng class in order to use service
jest.mock('entcore', () => ({
    ng: {service: jest.fn()}
}))

import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import {ViescolaireService} from "../ViescolaireService";

describe('ViescolaireService', () => {
    it('returns data when API request is correctly called', done => {
        var mock = new MockAdapter(axios);
        const data = {response: true};
        mock.onGet('/viescolaire/structures/structureId/time-slot').reply(200, data);
        ViescolaireService.getSlotProfile("structureId").then(response => {
            expect(response).toEqual(data);
            done();
        });
    });
});