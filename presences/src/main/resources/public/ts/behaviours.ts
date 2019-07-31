import {Behaviours} from 'entcore';
import {exemptionForm, navigation} from './sniplets'
import rights from './rights';

Behaviours.register('presences', {
    rights,
    sniplets: {
        navigation,
        'exemption-form': exemptionForm
    }
});
