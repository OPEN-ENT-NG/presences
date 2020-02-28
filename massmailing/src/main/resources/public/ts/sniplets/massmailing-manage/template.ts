import {toasts} from 'entcore';
import {settingsService, Template} from '../../services/';

console.log('massmailing');

declare let window: any;

interface ViewModel {
    mail: Template,
    mails: Template[],
    sms: Template,
    smss: Template[]
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
    sms: {
        name: '',
        content: ''
    },
    smss: [],
    deletion: {
        show: false,
        template: {
            name: '',
            content: ''
        }
    },
    syncTemplates: async function (type: 'MAIL' | 'SMS' | 'PDF'): Promise<void> {
        try {
            const data = await settingsService.get(type, window.model.vieScolaire.structure.id);
            vm[`${type.toLowerCase()}s`] = data;
            mailTemplateForm.that.$apply();
        } catch (e) {
            throw e;
        }
    },
    update: async function (template: Template): Promise<void> {
        try {
            await settingsService.update(template);
            toasts.confirm('massmailing.templates.update.success');
            vm[`${template.type.toLowerCase()}s`].map(_template => {
                if (template.id === _template.id) {
                    _template.name = template.name;
                    _template.content = template.content;
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
            await settingsService.delete(template);
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
            if (vm[type.toLowerCase()].name.trim() === '' && vm[type.toLowerCase()].content.trim() === '') return;
            vm[type.toLowerCase()] = {
                ...vm[type.toLowerCase()],
                structure_id: window.model.vieScolaire.structure.id,
                type
            };
            const data = await settingsService.create(vm[type.toLowerCase()]);
            toasts.confirm('massmailing.templates.creation.success');
            vm[`${type.toLowerCase()}s`].push(data);
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
            this.vm.syncTemplates('SMS');
            this.$on('reload', () => {
                this.vm.syncTemplates('MAIL');
                this.vm.syncTemplates('SMS');
            });
        }
    }
};