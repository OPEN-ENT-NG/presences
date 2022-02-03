import {angular, idiom as lang, toasts} from 'entcore';
import {settingsService, Template} from '../../services/';
import {MailTemplateCategory} from "@common/core/enum/mail-template-category";

console.log('massmailing');

declare let window: any;

interface ViewModel {
    pdf: Template,
    pdfs: Template[],
    mail: Template,
    mails: Template[],
    sms: Template,
    smss: Template[],
    smsMaxLength: number,
    deletion: {
        show: boolean,
        template: Template
    },
    mailCategory: any,

    syncTemplates(type: 'MAIL' | 'SMS' | 'PDF'): void

    create(type: 'MAIL' | 'SMS' | 'PDF'): Promise<void>

    update(template: Template, type: 'MAIL' | 'SMS' | 'PDF'): Promise<void>

    delete(template: Template): Promise<void>

    resetTemplate(type: 'MAIL' | 'SMS' | 'PDF'): void

    openTemplate(template: Template)

    copyCode(code: string, codeTooltip: string): any

    outCopy(code: string): void
}

const vm: ViewModel = {
    resetTemplate: function (type: "MAIL" | "SMS" | "PDF"): void {
        delete vm[type.toLowerCase()].id;
        vm[type.toLowerCase()].name = '';
        vm[type.toLowerCase()].content = '';
        // reset value content from <editor>
        if (vm[type.toLowerCase()].type === 'MAIL') {
            angular.element(document.getElementById("editor-mail")).scope().valueMail = '';
        } else if (vm[type.toLowerCase()].type === 'PDF') {
            angular.element(document.getElementById("editor-pdf")).scope().valuePDF = '';
        }
        mailTemplateForm.that.$apply();
    },
    smsMaxLength: 160,
    pdf: {
        name: '',
        content: '',
        category: MailTemplateCategory.ALL
    },
    pdfs: [],
    mail: {
        name: '',
        content: '',
        category: MailTemplateCategory.ALL
    },
    mails: [],
    sms: {
        name: '',
        content: '',
        category: MailTemplateCategory.ALL
    },
    smss: [],
    deletion: {
        show: false,
        template: {
            name: '',
            content: '',
            category: MailTemplateCategory.ALL
        }
    },
    mailCategory: MailTemplateCategory,

    syncTemplates: async function (type: 'MAIL' | 'SMS' | 'PDF'): Promise<void> {
        try {
            vm[`${type.toLowerCase()}s`] = await settingsService.get(type, window.model.vieScolaire.structure.id, "ALL");
            mailTemplateForm.that.$apply();
        } catch (e) {
            throw e;
        }
    },
    update: async function (template: Template, type: 'MAIL' | 'SMS' | 'PDF'): Promise<void> {
        // we assign "value" data from ngModel editor
        // to our template.content (only 'MAIL' & 'PDF' from <editor></editor is concerned)
        if (template.type === 'MAIL') {
            template.content = angular.element(document.getElementById("editor-mail")).scope().valueMail;
        } else if (template.type === 'PDF') {
            template.content = angular.element(document.getElementById("editor-pdf")).scope().valuePDF;
        }
        try {
            await settingsService.update(template);
            toasts.confirm('massmailing.templates.update.success');
            vm[`${template.type.toLowerCase()}s`].map(_template => {
                if (template.id === _template.id) {
                    _template.name = template.name;
                    _template.content = template.content;
                    _template.category = template.category;
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
        } finally {
            vm.resetTemplate(template.type);
        }
    },
    create: async function (type: 'MAIL' | 'SMS' | 'PDF') {
        try {
            if (vm[type.toLowerCase()].name.trim() === '' && vm[type.toLowerCase()].content.trim() === '') return;
            vm[type.toLowerCase()] = {
                ...vm[type.toLowerCase()],
                structure_id: window.model.vieScolaire.structure.id,
                type
            };
            // we assign "value" data from ngModel editor
            // to our template(vm[type.toLowerCase()]).content (only 'MAIL' & 'PDF' from <editor></editor is concerned)
            if (vm[type.toLowerCase()].type === 'MAIL') {
                vm[type.toLowerCase()].content = angular.element(document.getElementById("editor-mail")).scope().valueMail;
            } else if (vm[type.toLowerCase()].type === 'PDF') {
                vm[type.toLowerCase()].content = angular.element(document.getElementById("editor-pdf")).scope().valuePDF;
            }
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
    openTemplate: function ({id, structure_id, type, name, content, category}: Template) {
        // Fix <editor> issues for interacting with ngModel from editor
        // we get its element and use "value" data instead of our View Model
        if (type === 'MAIL') {
            angular.element(document.getElementById("editor-mail")).scope().valueMail = content;
        } else if (type === 'PDF') {
            angular.element(document.getElementById("editor-pdf")).scope().valuePDF = content;
        }
        vm[type.toLowerCase()] = {id, structure_id, type, name, content, category};
    },
    copyCode: function (code: string, codeTooltip: string) {
        let copyText = document.getElementById(code);
        let textArea = document.createElement("textarea");
        textArea.value = copyText.innerText;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand("Copy");
        textArea.remove();

        let tooltip = document.getElementById(codeTooltip);
        tooltip.innerHTML = lang.translate('massmailing.copied') + ':' + copyText.textContent;
    },
    outCopy: function (codeTooltip: string) {
        let tooltip = document.getElementById(codeTooltip);
        tooltip.innerHTML = lang.translate('massmailing.copy');
    }
};

export const mailTemplateForm = {
    title: 'massmailing.template.form',
    public: false,
    that: null,
    controller: {
        init: function (): void {
            this.vm = vm;
            this.setHandler();
            mailTemplateForm.that = this;
            this.load();
        },
        load: function (): void {
            vm.resetTemplate('PDF');
            vm.syncTemplates('PDF');

            vm.resetTemplate('MAIL');
            vm.syncTemplates('MAIL');

            vm.resetTemplate('SMS');
            vm.syncTemplates('SMS');
        },
        setHandler: function (): void {
            this.$on('reload', this.load);
            this.$on('$destroy', () => {
                vm.pdf = {name: '', content: '', category: MailTemplateCategory.ALL};
                vm.mail = {name: '', content: '', category: MailTemplateCategory.ALL};
                vm.sms = {name: '', content: '', category: MailTemplateCategory.ALL}
            });
            this.$watch(() => window.model.vieScolaire.structure, this.load);
            this.$watch(() => vm.sms.content, (newVal, oldVal) => {
                if (newVal.length > vm.smsMaxLength && typeof oldVal === "string") {
                    vm.sms.content = oldVal;
                }
            });
        }
    }
};