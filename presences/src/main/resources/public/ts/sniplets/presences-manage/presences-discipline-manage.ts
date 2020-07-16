import {AxiosResponse} from "axios";
import {Discipline, DisciplineRequest} from "../../models";
import {disciplineService} from "../../services";
import {PRESENCES_DISCIPLINE} from "@common/core/enum/presences-event";

declare let window: any;

interface ViewModel {
    safeApply(fn?: () => void): void;

    form: DisciplineRequest;

    disciplines: Discipline[];

    isFormValid(form: DisciplineRequest): boolean;

    hasDisciplines(): boolean;

    proceedAfterAction(response: AxiosResponse): void;

    getDisciplines(): Promise<void>;

    createDiscipline(): Promise<void>;

    toggleVisibility(discipline: Discipline): Promise<void>;

    deleteDiscipline(disciplineType: Discipline): Promise<void>

    openDisciplinesManageLightbox(discipline: Discipline): void;
}

function safeApply() {
    let that = disciplineManage.that;
    return new Promise((resolve, reject) => {
        var phase = that.$root.$$phase;
        if (phase === '$apply' || phase === '$digest') {
            if (resolve && (typeof (resolve) === 'function')) resolve();
        } else {
            if (resolve && (typeof (resolve) === 'function')) that.$apply(resolve);
            else that.$apply();
        }
    });
}


const vm: ViewModel = {
    safeApply: null,
    disciplines: [],
    form: {} as DisciplineRequest,

    isFormValid(form: DisciplineRequest): boolean {
        return !!(form.label);
    },

    async createDiscipline(): Promise<void> {
        vm.form.structureId = window.model.vieScolaire.structure.id;
        let response = await disciplineService.create(vm.form);
        vm.proceedAfterAction(response);
        vm.form.label = '';
    },

    async deleteDiscipline(discipline: Discipline): Promise<void> {
        let response = await disciplineService.delete(discipline.id);
        vm.proceedAfterAction(response);
    },

    hasDisciplines(): boolean {
        return vm.disciplines && vm.disciplines.length !== 0;
    },

    async getDisciplines(): Promise<void> {
        vm.disciplines = await disciplineService.get(window.model.vieScolaire.structure.id);
        vm.safeApply();
    },

    async toggleVisibility(discipline: Discipline): Promise<void> {
        discipline.hidden = !discipline.hidden;
        let form = {} as DisciplineRequest;
        form.id = discipline.id;
        form.hidden = discipline.hidden;
        form.label = discipline.label;
        await disciplineService.update(form);
    },

    proceedAfterAction(response: AxiosResponse): void {
        if (response.status === 200 || response.status === 201) {
            vm.getDisciplines();
        }
    },

    openDisciplinesManageLightbox(discipline: Discipline): void {
        disciplineManage.that.$emit(PRESENCES_DISCIPLINE.SEND, discipline);
    },

};

export const disciplineManage = {
    title: 'disciplines.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            this.setHandler();
            disciplineManage.that = this;
            vm.safeApply = safeApply;
        },
        setHandler: function () {
            // using vieScolaire.structure to update current structure from viescolaire
            this.$watch(() => window.model.vieScolaire.structure, async () => vm.getDisciplines());
            this.$on(PRESENCES_DISCIPLINE.RESPOND, () => vm.getDisciplines());
            this.$on('reload', vm.getDisciplines);
        }
    }
};