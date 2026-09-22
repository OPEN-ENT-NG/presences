// tricks to fake "mock" entcore ng class in order to use service
jest.mock('entcore', () => ({
    ng: {service: jest.fn()}
}))

jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));

import {IndicatorBody} from "../../model/Indicator";
import {indicatorService} from "../indicator.service";
import {http} from 'entcore-toolkit';
import {mockHttpResponse} from '@test-utils/httpMock';

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
        const data = {response: true};
        const url = `/statistics-presences/structures/${structure}/indicators/${name}?page=${page}`;
        (http.post as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url, method: 'post'}));
        indicatorService.fetchIndicator(structure, name, page, body).then(response => {
            expect(http.post).toHaveBeenCalledWith(url, body);
            expect(response).toEqual(data);
            done();
        });
    });

    it('should return data when API fetchGraphIndicator request is correctly called', done => {
        const data = {response: true};
        const url = `/statistics-presences/structures/${structure}/indicators/${name}/graph`;
        (http.post as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url, method: 'post'}));
        indicatorService.fetchGraphIndicator(structure, name, body).then(response => {
            expect(http.post).toHaveBeenCalledWith(url, body);
            expect(response).toEqual(data);
            done();
        });
    });

})
;
