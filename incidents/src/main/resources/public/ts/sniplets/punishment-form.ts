import {idiom as lang, moment, toasts} from 'entcore';
import {IPunishment, IPunishmentBody, IStructureSlot, Student} from "../models";
import {SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS} from '@common/model'
import {IAngularEvent} from "angular";
import {StudentsSearch} from "@common/utils";
import {SearchService} from "@common/services/SearchService";
import {punishmentService, punishmentsTypeService, ViescolaireService} from "@incidents/services";
import {PunishmentCategoryType} from "@incidents/models/PunishmentCategory";
import {IPunishmentType} from "@incidents/models/PunishmentType";
import {User} from "@common/model/User";
import {PunishmentsUtils} from "@incidents/utilities/punishments";
import {AxiosResponse} from "axios";

declare let window: any;

console.log('Sniplet Punishment create/edit form');

interface ViewModel {
    safeApply(fn?: () => void): void;

    createPunishmentLightBox: boolean;
    studentsSearch: StudentsSearch;
    date: { date: string, startTime: Date, endTime: Date };
    punishment: IPunishment;
    form: IPunishmentBody;
    punishmentTypes: Array<IPunishmentType>;
    punishmentCategoriesType: typeof PunishmentCategoryType;
    structureTimeSlot: IStructureSlot;

    selectPunishmentGroupBy: (punishment: IPunishmentType) => string;

    safeApply(fn?: () => void): void;

    checkOptionState(): void;

    removeEmptyOption(): void;

    openPunishmentLightbox(): void;

    editPunishmentForm(punishment: IPunishment): void;

    isFormValid(): boolean;

    preparePunishmentForm(): void;

    initPunishmentEdit(punishment: IPunishment): void;

    setCategory(type: IPunishmentType): void;

    create(): Promise<void>;

    update(): Promise<void>;

    delete(): Promise<void>;

    getStudentsFromView(): void;

    // search bar method

    searchStudent(studentForm: string): Promise<void>;

    selectStudent(valueInput, studentItem): void;

    removeSelectedStudents(studentItem): void;

    closePunishmentLightbox(): void;
}

const vm: ViewModel = {
    safeApply: null,
    createPunishmentLightBox: false,
    studentsSearch: undefined,
    punishmentTypes: [],
    form: {} as IPunishmentBody,
    punishmentCategoriesType: PunishmentCategoryType,
    structureTimeSlot: {} as IStructureSlot,
    date: {
        date: moment(),
        startTime: moment().set({second: 0, millisecond: 0}).toDate(),
        endTime: moment().add(1, 'h').set({second: 0, millisecond: 0}).toDate(),
    },
    punishment: {} as IPunishment,
    selectPunishmentGroupBy: (punishmentType: IPunishmentType) => punishmentType.type === PunishmentsUtils.RULES.punishment ?
        lang.translate("incidents.punishments") : lang.translate("incidents.sanctions"),

    openPunishmentLightbox(): void {
        vm.studentsSearch = new StudentsSearch(window.structure.id, SearchService);
        vm.form = {} as IPunishmentBody;
        vm.form.type = null as IPunishmentType;
        vm.getStudentsFromView();
        vm.punishment = {} as IPunishment;
        // check if add empty state
        vm.checkOptionState();
        vm.createPunishmentLightBox = true;
        vm.safeApply();
    },

    checkOptionState(): void {
        let selectPunishmentType = document.getElementById('selectPunishmentType');
        if (selectPunishmentType['options'][0].value !== '') {
            let option = document.createElement('option');
            option.value = '';
            selectPunishmentType.insertBefore(option, selectPunishmentType.firstChild)
        }
    },

    removeEmptyOption(): void {
        let selectPunishmentType = document.getElementById('selectPunishmentType');
        if (selectPunishmentType['options'][0].value === '') {
            selectPunishmentType.removeChild(selectPunishmentType['options'][0]);
        }
    },

    editPunishmentForm(punishment: IPunishment): void {
        vm.studentsSearch = new StudentsSearch(window.structure.id, SearchService);
        vm.initPunishmentEdit(punishment);
        vm.createPunishmentLightBox = true;
        vm.safeApply();
    },

    isFormValid(): boolean {
        return vm.form.type_id != null &&
            vm.form.fields != null &&
            (vm.studentsSearch.getSelectedStudents().map(student => student["id"]).length > 0 ||
                vm.punishment.student != undefined);
    },

    preparePunishmentForm: (): void => {
        if (vm.punishment.id) {
            vm.form.student_id = vm.punishment.student.id;
            // todo edit incident for next feature
            vm.form.incident_id = null;
        } else {
            vm.form.structure_id = window.structure.id;
            vm.form.student_ids = vm.studentsSearch.getSelectedStudents().map(student => student["id"]);
            // todo edit incident for next feature
            vm.form.incident_id = null;
        }
    },

    initPunishmentEdit: (punishment: IPunishment): void => {
        /* when click on card to edit presence */
        vm.punishment = {
            id: punishment.id,
            structure_id: punishment.structure_id,
            type: punishment.type,
            fields: punishment.fields,
            description: punishment.description,
            owner: {
                displayName: punishment.owner.displayName,
                id: punishment.owner.id
            } as User,
            student: {
                id: punishment.student.id,
                name: punishment.student.name,
                className: punishment.student.className,
            } as Student,
        };
        vm.form.id = punishment.id;
        vm.form.structure_id = punishment.structure_id;
        vm.form.description = punishment.description;
        vm.form.fields = punishment.fields;
        vm.form.category_id = punishment.type.punishment_category_id;
        vm.form.type_id = punishment.type.id;
        vm.form.type = punishment.type;
    },

    setCategory: (): void => {
        vm.removeEmptyOption();
        vm.form.category_id = vm.form.type.punishment_category_id;
        vm.form.type_id = vm.form.type.id;
    },

    async create(): Promise<void> {
        vm.preparePunishmentForm();
        if (!PunishmentsUtils.isValidPunishmentBody(vm.form)) {
            toasts.warning(lang.translate('incidents.invalid.form'));
            return;
        }
        let response = await punishmentService.create(vm.form);
        if (response && (response.status == 200 || response.status == 201)) {
            vm.closePunishmentLightbox();
            toasts.confirm(lang.translate('incidents.punishment.create.succeed'));
        } else {
            toasts.warning(response.data.toString());
        }
        punishmentForm.that.$emit(SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS.CREATION);
        vm.safeApply();
    },

    async update(): Promise<void> {
        vm.preparePunishmentForm();
        if (!PunishmentsUtils.isValidPunishmentBody(vm.form)) {
            toasts.warning(lang.translate('incidents.invalid.form'));
            return;
        }
        let response = await punishmentService.update(vm.form);
        if (response && (response.status == 200 || response.status == 201)) {
            vm.closePunishmentLightbox();
            toasts.confirm(lang.translate('incidents.punishment.edit.succeed'));
        } else {
            toasts.warning(response.data.toString());
        }
        punishmentForm.that.$emit(SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS.EDIT);
        vm.safeApply();
    },

    async delete(): Promise<void> {
        vm.preparePunishmentForm();
        let response: AxiosResponse = await punishmentService.delete(vm.form.id, window.structure.id);
        if (response.status == 200 || response.status == 201) {
            vm.closePunishmentLightbox();
            toasts.confirm(lang.translate('incidents.punishment.delete.succeed'));
        } else {
            toasts.warning(response.data.toString());
        }
        punishmentForm.that.$emit(SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS.DELETE);
        vm.safeApply();
    },

    closePunishmentLightbox(): void {
        vm.form = {} as IPunishmentBody;
        vm.punishment = {} as IPunishment;
        vm.createPunishmentLightBox = false;
    },

    getStudentsFromView: (): void => {
        const viewUrl = window.location.hash;
        const findView = (view) => {
            if (viewUrl.includes(view)) {
                return viewUrl;
            }
        };
        switch (viewUrl) {
            case findView('#/calendar'): {
                // window.item stored from calendar controller
                vm.studentsSearch.setSelectedStudents([window.item]);
                break;
            }
            case findView('#/alerts'): {
                let students: Array<User> = [];
                // window.alerts_item stored from alerts controller
                if ('alerts_item' in window) {
                    window.alerts_item
                        .filter(alert => alert.selected)
                        .forEach(alert => {
                            students.push({
                                id: alert.student_id,
                                displayName: alert.name,
                                toString: () => alert.name
                            } as User);
                        });
                    vm.studentsSearch.setSelectedStudents(students);
                }
                break;
            }
            default:
                return;
        }
    },

    // search bar method

    async searchStudent(studentForm: string): Promise<void> {
        await vm.studentsSearch.searchStudents(studentForm);
        vm.safeApply();
    },

    async selectStudent(valueInput, studentItem): Promise<void> {
        vm.studentsSearch.selectStudents(valueInput, studentItem);
        vm.studentsSearch.student = "";
    },

    async removeSelectedStudents(studentItem): Promise<void> {
        vm.studentsSearch.removeSelectedStudents(studentItem);
    },
};

export const punishmentForm = {
    public: false,
    that: null,
    controller: {
        init: function () {
            this.vm = vm;
            vm.safeApply = this.safeApply;
            punishmentForm.that = this;
            this.setHandler();
        },
        async getPunishmentCategory(structure_id: string): Promise<void> {
            if (!vm.punishmentTypes || vm.punishmentTypes.length <= 1) {
                vm.punishmentTypes = await punishmentsTypeService.get(structure_id);
            }
            if (PunishmentsUtils.canCreatePunishmentOnly()) {
                vm.punishmentTypes = vm.punishmentTypes.filter((punishmentType: IPunishmentType) =>
                    punishmentType.type === PunishmentsUtils.RULES.punishment);
            }
            vm.punishmentTypes = vm.punishmentTypes.filter(punishmentType => !punishmentType.hidden);
        },
        async getTimeSlots(structure_id: string): Promise<void> {
            if (!vm.structureTimeSlot || !('slots' in vm.structureTimeSlot)) {
                vm.structureTimeSlot = await ViescolaireService.getSlotProfile(structure_id);
            }
        },
        setHandler: function () {
            this.$on(SNIPLET_FORM_EMIT_PUNISHMENT_EVENTS.OPEN, (event: IAngularEvent, punishment) => vm.editPunishmentForm(punishment));
            this.$on("$destroy", () => {
                delete window.alerts_item;
            });
            this.$watch(() => window.structure, async () => {
                this.getPunishmentCategory(window.structure.id);
                this.getTimeSlots(window.structure.id);
                vm.safeApply();
            });
        }
    }
};