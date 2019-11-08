import {Behaviours} from 'entcore';
import {
    absenceForm,
    exemptionForm,
    forgottenNotebookForm,
    navigation,
    presencesManage,
    presencesReasonManage
} from './sniplets'
import rights from './rights';
import incidentsRights from '@incidents/rights';

Behaviours.register('presences', {
    rights,
    incidentsRights,
    sniplets: {
        navigation,
        'exemption-form': exemptionForm,
        'absence-form': absenceForm,
        'forgotten-notebook-form': forgottenNotebookForm,
        'presences-manage': presencesManage,
        'presences-manage/reason-manage/sniplet-presences-reason-manage': presencesReasonManage
    }
});
