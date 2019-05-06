import {model} from 'entcore';
import rights from '../rights';

export const navigation = {
    title: 'sniplet.navigation.title',
    description: 'sniplet.navigation.description',
    public: false,
    controller: {
        init: function () {
            this.menu = {
                hovered: '',
                active: '',
                timeout: null
            }
        },
        hoverIn: function (menuItem) {
            this.menu.hovered = menuItem;
            window.clearTimeout(this.menu.timeout);
        },
        hoverOut: function () {
            this.menu.timeout = setTimeout(() => {
                this.menu.hovered = '';
                this.$apply();
            }, 250);
        },
        getCurrentState: () => {
            const regexp = /#\/([a-z].*)/;
            const res = regexp.exec(window.location.hash);
            return (res !== null && res.length > 1) ? res[1] : '';
        },
        hasRight: (right) => {
            return model.me.hasWorkflow(rights.workflow[right]);
        }
    }
};