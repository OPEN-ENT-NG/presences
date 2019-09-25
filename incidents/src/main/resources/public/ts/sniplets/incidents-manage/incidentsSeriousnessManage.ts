interface ViewModel {
    safeApply(fn?: () => void): void;
}

const vm: ViewModel = {
    safeApply: null,
};

export const incidentsSeriousnessManage = {
    title: 'incidents.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            incidentsSeriousnessManage.that = this;
            vm.safeApply = this.safeApply;
        }
    }
};