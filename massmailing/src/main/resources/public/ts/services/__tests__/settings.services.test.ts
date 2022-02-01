import MockAdapter from "axios-mock-adapter";
import axios from "axios";
import {settingsService, Template} from "@massmailing/services";
import {MailTemplateCategory} from "@common/core/enum/mail-template-category";

describe('SettingsService', () => {
    it('test of the proper functioning of the get method', done => {
        const structureId: string = 'structureId';
        const type = "MAIL"
        const category = "category"
        const mock = new MockAdapter(axios);
        const data = {response: true};

        mock.onGet(`/massmailing/settings/templates/${type}?structure=${structureId}&category=${category}`)
            .reply(200, data);


        settingsService.get("MAIL", "structureId", "category").then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('test of the proper functioning of the create method', done => {
        const mock = new MockAdapter(axios);
        const data = {response: true};
        const template: Template = {category: MailTemplateCategory.ALL, content: "", name: "", id: 4};

        mock.onPost(`/massmailing/settings/templates`, template)
            .reply(200, data);


        settingsService.create(template).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('test of the proper functioning of the update method', done => {
        const mock = new MockAdapter(axios);
        const data = {response: true};
        const template: Template = {category: MailTemplateCategory.ALL, content: "", name: "", id: 4};

        mock.onPut(`/massmailing/settings/templates/${template.id}`, template)
            .reply(200, data);


        settingsService.update(template).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('test of the proper functioning of the delete method', done => {
        const mock = new MockAdapter(axios);
        const data = {response: true};
        const template: Template = {category: MailTemplateCategory.ALL, content: "", name: "", id: 4};

        mock.onDelete(`/massmailing/settings/templates/${template.id}`)
            .reply(200, data);


        settingsService.delete(template).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });
});