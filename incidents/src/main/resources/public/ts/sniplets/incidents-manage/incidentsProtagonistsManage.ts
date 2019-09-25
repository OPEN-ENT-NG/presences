interface ViewModel {
    safeApply(fn?: () => void): void;
}

const vm: ViewModel = {
    safeApply: null,
};

export const incidentsProtagonistsManage = {
    title: 'incidents.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            incidentsProtagonistsManage.that = this;
            vm.safeApply = this.safeApply;
        }
    }
};