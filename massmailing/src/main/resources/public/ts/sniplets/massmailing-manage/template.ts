import {toasts} from 'entcore';
import {SettingsService, Template} from '../../services/';

console.log('massmailing');

declare let window: any;

interface ViewModel {
    mail: Template,
    mails: Template[],
    deletion: {
        show: boolean,
        template: Template
    }

    syncTemplates(type: 'MAIL' | 'SMS' | 'PDF'): void

    create(type: 'MAIL' | 'SMS' | 'PDF'): Promise<void>

    update(template: Template): Promise<void>

    delete(template: Template): Promise<void>

    resetTemplate(type: 'MAIL' | 'SMS' | 'PDF'): void

    openTemplate(template: Template)
}

const vm: ViewModel = {
    resetTemplate: function (type: "MAIL" | "SMS" | "PDF"): void {
        vm[type.toLowerCase()] = {
            name: '',
            content: ''
        };
        mailTemplateForm.that.$apply();
    },
    mail: {
        name: '',
        content: ''
    },
    mails: [],
    deletion: {
        show: false,
        template: {
            name: '',
            content: ''
        }
    },
    syncTemplates: async function (type: 'MAIL' | 'SMS' | 'PDF'): Promise<void> {
        try {
            const data = await SettingsService.get(type, window.model.vieScolaire.structure.id);
            vm[`${type.toLowerCase()}s`] = data;
            mailTemplateForm.that.$apply();
        } catch (e) {
            throw e;
        }
    },
    update: async function (template: Template): Promise<void> {
        try {
            await SettingsService.update(template);
            toasts.confirm('massmailing.templates.update.success');
            vm[`${template.type.toLowerCase()}s`].map(mail => {
                if (template.id === mail.id) {
                    mail.name = template.name;
                    mail.content = template.content;
                }
            });
        } catch (e) {
            toasts.warning('massmailing.templates.update.error');
            throw e;
        } finally {
            vm.resetTemplate(template.type);
        }
    },
    delete: async function (template: Template): Promise<void> {
        try {
            await SettingsService.delete(template);
            vm[`${template.type.toLowerCase()}s`] = vm[`${template.type.toLowerCase()}s`].filter(t => t.id !== template.id);
            vm.deletion.show = false;
            toasts.confirm('massmailing.templates.deletion.success');
            mailTemplateForm.that.$apply();
        } catch (e) {
            toasts.warning('massmailing.templates.deletion.error');
            throw e;
        }
    },
    create: async function (type) {
        try {
            vm.mail = {...vm.mail, structure_id: window.model.vieScolaire.structure.id, type};
            const data = await SettingsService.create(vm.mail);
            toasts.confirm('massmailing.templates.creation.success');
            vm.mails.push(data);
        } catch (e) {
            toasts.warning('massmailing.templates.creation.error');
            throw e;
        } finally {
            vm.resetTemplate(type);
        }
    },
    openTemplate: function ({id, structure_id, type, name, content}: Template) {
        vm[type.toLowerCase()] = {id, structure_id, type, name, content};
    }
};

export const mailTemplateForm = {
    title: 'massmailing.template.form',
    public: false,
    that: null,
    controller: {
        init: function () {
            this.vm = vm;
            mailTemplateForm.that = this;
            this.vm.syncTemplates('MAIL');
        }
    }
};