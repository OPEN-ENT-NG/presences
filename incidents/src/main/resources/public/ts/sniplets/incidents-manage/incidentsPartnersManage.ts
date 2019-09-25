interface ViewModel {
    safeApply(fn?: () => void): void;
}

const vm: ViewModel = {
    safeApply: null,
};

export const incidentsPartnersManage = {
    title: 'incidents.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            incidentsPartnersManage.that = this;
            vm.safeApply = this.safeApply;
        }
    }
};