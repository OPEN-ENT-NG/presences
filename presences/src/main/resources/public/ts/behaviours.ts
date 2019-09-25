import {Behaviours} from 'entcore';
import {absenceForm, exemptionForm, navigation, presencesManage, presencesReasonManage} from './sniplets'
import rights from './rights';
import incidentsRights from '@incidents/rights';

Behaviours.register('presences', {
    rights,
    incidentsRights,
    sniplets: {
        navigation,
        'exemption-form': exemptionForm,
        'absence-form': absenceForm,
        'presences-manage': presencesManage,
        'presences-manage/sniplet-presences-reason-manage': presencesReasonManage
    }
});
