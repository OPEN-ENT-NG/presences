import {Behaviours} from 'entcore';
import {absenceForm, exemptionForm, navigation} from './sniplets'
import rights from './rights';
import incidentsRights from '../../../../../../incidents/src/main/resources/public/ts/rights';


Behaviours.register('presences', {
    rights,
    incidentsRights,
    sniplets: {
        navigation,
        'exemption-form': exemptionForm,
        'absence-form': absenceForm
    }
});
