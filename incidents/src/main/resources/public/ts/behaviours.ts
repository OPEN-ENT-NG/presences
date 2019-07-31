import {Behaviours} from 'entcore';
import {incidentForm} from './sniplets';

Behaviours.register('incidents', {
    sniplets: {
        'incident-form': incidentForm
    }
});
