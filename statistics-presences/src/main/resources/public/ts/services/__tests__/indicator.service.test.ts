// tricks to fake "mock" entcore ng class in order to use service
jest.mock('entcore', () => ({
    ng: {service: jest.fn()}
}))

import {IndicatorBody} from "../../model/Indicator";
import {indicatorService} from "../indicator.service";
import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';

describe('IndicatorService', () => {
    const body: IndicatorBody = {
        end: "2021-06-20",
        start: "2021-06-15",
        types: [],
        filters: {},
        reasons: [],
        punishmentTypes: [],
        sanctionTypes: [],
        users: [],
        audiences: []
    };
    const structure: string = "structureId";
    const name: string = "Monthly";
    const page: number = 1;

    it('should return data when API fetchIndicator request is correctly called', done => {
        let mock = new MockAdapter(axios);
        const data = {response: true};
        mock.onPost(`/statistics-presences/structures/${structure}/indicators/${name}?page=${page}`, body).reply(200, data);
        indicatorService.fetchIndicator(structure, name, page, body).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('should return data when API fetchGraphIndicator request is correctly called', done => {
        let mock = new MockAdapter(axios);
        const data = {response: true};
        mock.onPost(`/statistics-presences/structures/${structure}/indicators/${name}/graph`, body).reply(200, data);
        indicatorService.fetchGraphIndicator(structure, name, body).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

})
;