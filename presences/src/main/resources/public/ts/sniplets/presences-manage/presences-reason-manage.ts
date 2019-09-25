
interface ViewModel {
    addReason(): void;
    reasons: string[];
}

const vm: ViewModel = {
    reasons: [],
    addReason(): void {
        console.log("add reason");
    }
};

export const presencesReasonManage = {
    title: 'presences.absence.reason.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            vm.reasons = [
                'Maladie', 'test', 'Mort', 'accident', 'hopital', 'raison', 'preprod issue', 'support', 'jin', 'appart', 'dur',
                'absence', 'presence', 'title'
            ];
            presencesReasonManage.that = this;
        }
    }
};