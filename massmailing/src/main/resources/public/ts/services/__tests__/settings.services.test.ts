jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));

import {http} from 'entcore-toolkit';
import {mockHttpResponse} from '@test-utils/httpMock';
import {settingsService, Template} from "@massmailing/services";
import {MailTemplateCategory} from "@common/core/enum/mail-template-category";

describe('SettingsService', () => {
    it('test of the proper functioning of the get method', done => {
        const structureId: string = 'structureId';
        const type = "MAIL"
        const category = "category"
        const data = {response: true};
        const url = `/massmailing/settings/templates/${type}?structure=${structureId}&category=${category}`;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url}));

        settingsService.get("MAIL", "structureId", "category").then(response => {
            expect(http.get).toHaveBeenCalledWith(url);
            expect(response).toEqual(data);
            done();
        });
    });

    it('test of the proper functioning of the create method', done => {
        const data = {response: true};
        const template: Template = {category: MailTemplateCategory.ALL, content: "", name: "", id: 4};
        const url = `/massmailing/settings/templates`;
        (http.post as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url, method: 'post'}));

        settingsService.create(template).then(response => {
            expect(http.post).toHaveBeenCalledWith(url, template);
            expect(response).toEqual(data);
            done();
        });
    });

    it('test of the proper functioning of the update method', done => {
        const data = {response: true};
        const template: Template = {category: MailTemplateCategory.ALL, content: "", name: "", id: 4};
        const url = `/massmailing/settings/templates/${template.id}`;
        (http.put as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url, method: 'put'}));

        settingsService.update(template).then(response => {
            expect(http.put).toHaveBeenCalledWith(url, template);
            expect(response).toEqual(data);
            done();
        });
    });

    it('test of the proper functioning of the delete method', done => {
        const data = {response: true};
        const template: Template = {category: MailTemplateCategory.ALL, content: "", name: "", id: 4};
        const url = `/massmailing/settings/templates/${template.id}`;
        (http.delete as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url, method: 'delete'}));

        settingsService.delete(template).then(response => {
            expect(http.delete).toHaveBeenCalledWith(url);
            expect(response).toEqual(data);
            done();
        });
    });
});
