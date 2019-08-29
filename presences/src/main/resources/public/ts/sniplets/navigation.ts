import {model} from 'entcore';
import rights from '../rights';
import incidentsRights from '../../../../../../../incidents/src/main/resources/public/ts/rights'

declare let window: any;

interface Structure {
    id: string;
    name: string;
}

function initStructures(): Structure[] {
    const {structures, structureNames} = model.me;
    const values = [];
    for (let i = 0; i < structures.length; i++) {
        if (window.structures.indexOf(structures[i]) !== -1) {
            values.push({id: structures[i], name: structureNames[i]});
        }
    }
    return values;
}

export const navigation = {
    title: 'sniplet.navigation.title',
    description: 'sniplet.navigation.description',
    public: false,
    controller: {
        init: function () {
            this.structures = initStructures();
            this.menu = {
                structure: this.structures[0],
                hovered: '',
                active: '',
                timeout: null
            };
            this.setStructure(this.structures[0]);
            this.$apply();
        },
        setStructure: function (structure: Structure) {
            window.structure = structure;
            this.menu.structure = structure;
            this.$apply();
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
            const res = window.location.hash.split('/');
            return (res !== null && res.length > 1) ? res[1] : '';
        },
        hasRight: (right) => {
            return model.me.hasWorkflow(rights.workflow[right]);
        },
        hasIncidentRight: (right) => {
            return model.me.hasWorkflow(incidentsRights.workflow[right]);
        }
    }
};