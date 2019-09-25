
console.log("presenceManage sniplet");

interface ViewModel {
    safeApply(fn?: () => void): void;
    scrollToElement($element): void;
}

function safeApply() {
    let that = presencesManage.that;
    return new Promise((resolve, reject) => {
        var phase = that.$root.$$phase;
        if(phase === '$apply' || phase === '$digest') {
            if(resolve && (typeof(resolve) === 'function')) resolve();
        } else {
            if (resolve && (typeof(resolve) === 'function')) that.$apply(resolve);
            else that.$apply();
        }
    });
}

const vm: ViewModel = {
    safeApply: null,
    scrollToElement(target): void {
        let element = document.getElementById(target);
        element.scrollIntoView({behavior: "smooth", block: "start", inline: "nearest"});
        vm.safeApply();
    },
};

export const presencesManage = {
    title: 'presences.manage.title',
    public: false,
    that: null,
    controller: {
        init: async function () {
            this.vm = vm;
            presencesManage.that = this;
            vm.safeApply = safeApply;
        }
    }
};