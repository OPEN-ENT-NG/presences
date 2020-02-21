import {Me, model} from 'entcore';
import rights from '../rights';
import incidentsRights from '@incidents/rights'
import massmailingRights from '@massmailing/rights';
import {PreferencesUtils} from "../utilities";

declare let window: any;

console.log('Sniplet Navigation');

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
        init: async function () {
            this.structures = initStructures();
            let preferenceStructure = PreferencesUtils.getPreference("presences.structure");
            let preferenceStructureId =  preferenceStructure ? preferenceStructure['defaultId'] : null;
            let structure = this.structures.length > 1 && preferenceStructureId ? this.structures.find((s) => s.id === preferenceStructureId) : this.structures[0];
            this.menu = {
                structure: structure,
                hovered: '',
                active: '',
                timeout: null
            };
            await this.setStructure(structure);
            this.$apply();
        },
        setStructure: async function (structure: Structure) {
            window.structure = structure;
            this.menu.structure = structure;
            await PreferencesUtils.updateStructure(structure.id);
            this.$apply();
            console.log("Stop");
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
        },
        hasMassmailingRight: (right) => {
            return model.me.hasWorkflow(massmailingRights.workflow[right]);
        }
    }
};