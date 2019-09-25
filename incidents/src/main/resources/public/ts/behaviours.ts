import {Behaviours} from 'entcore';
import {incidentForm, incidentsPlaceManage, incidentsTypeManage,
    incidentsProtagonistsManage, incidentsSeriousnessManage, incidentsPartnersManage} from './sniplets';
import rights from "./rights";

Behaviours.register('incidents', {
    rights,
    sniplets: {
        'incident-form': incidentForm,
        'incidents-manage/sniplet-incidents-type-manage': incidentsTypeManage,
        'incidents-manage/sniplet-incidents-places-manage': incidentsPlaceManage,
        'incidents-manage/sniplet-incidents-protagonists-manage': incidentsProtagonistsManage,
        'incidents-manage/sniplet-incidents-seriousness-manage': incidentsSeriousnessManage,
        'incidents-manage/sniplet-incidents-partners-manage': incidentsPartnersManage,

    }
});
