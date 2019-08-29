import {Behaviours} from 'entcore';
import {incidentForm} from './sniplets';
import rights from "./rights";

Behaviours.register('incidents', {
    rights,
    sniplets: {
        'incident-form': incidentForm
    }
});
