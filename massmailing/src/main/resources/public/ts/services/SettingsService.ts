import {ng} from 'entcore';
import http from 'axios';
import {MailTemplateCategory} from "@common/core/enum/mail-template-category";

export interface Template {
    id?: number
    structure_id?: string
    name: string,
    content: string,
    type?: 'MAIL' | 'PDF' | 'SMS',
    category: MailTemplateCategory
}

export interface SettingsService {
    /**
     * Retrieve templates based on given type and structure identifier
     * @param type template type. Should be MAIL, PDF or SMS
     * @param structure structure identifier
     * @param category category identifier
     */
    get(type: 'MAIL' | 'PDF' | 'SMS', structure: string, category: string): Promise<any>;

    /**
     * Create given template
     * @param template template to create
     */
    create(template: Template): Promise<any>;

    /**
     * Update given template
     * @param template template to update
     */
    update(template: Template): Promise<any>;

    /**
     * Delete given template
     * @param template template to delete
     */
    delete(template: Template): Promise<any>;
}

export const settingsService: SettingsService = {
    get: async function (type: "MAIL" | "PDF" | "SMS", structure: string, category: string): Promise<any> {
        try {
            const {data} = await http.get(`/massmailing/settings/templates/${type}?structure=${structure}&category=${category}`);
            return data;
        } catch (e) {
            throw e;
        }
    },

    create: async function (template: Template): Promise<any> {
        try {
            const {data} = await http.post(`/massmailing/settings/templates`, template);
            return data;
        } catch (e) {
            throw e;
        }
    },
    update: async function (template: Template): Promise<any> {
        try {
            const {data} = await http.put(`/massmailing/settings/templates/${template.id}`, template);
            return data;
        } catch (e) {
            throw e;
        }
    },
    delete: async function (template: Template): Promise<any> {
        try {
            const {data} = await http.delete(`/massmailing/settings/templates/${template.id}`);
            return data;
        } catch (e) {
            throw e;
        }
    }
};

export const ngSettingsService = ng.service('SettingsService', (): SettingsService => settingsService);