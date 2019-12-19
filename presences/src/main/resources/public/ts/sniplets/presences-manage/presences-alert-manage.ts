export const presencesAlertManage = {
    title: 'presences.alert.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            console.log('alert');
            // this.vm = vm;
            this.setHandler();
            presencesAlertManage.that = this;
            // vm.safeApply = safeApply;
        },

        removeEvent: function () {
            console.log("-")
        },

        addEvent: function () {
            console.log("+")
        }
    }
};