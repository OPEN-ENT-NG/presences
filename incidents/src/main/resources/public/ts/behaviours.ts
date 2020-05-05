import {Behaviours} from 'entcore';
import {
    incidentForm,
    incidentsManageLightbox,
    incidentsPartnersManage,
    incidentsPlaceManage,
    incidentsProtagonistsManage,
    incidentsSeriousnessManage,
    incidentsTypeManage,
    punishmentsTypeManage,
    punishmentForm
} from './sniplets';
import incidentsRights from "./rights";
import rights from '@presences/rights';
import {incidentsMementoWidget} from "./sniplets/memento/incidents";

Behaviours.register('incidents', {
    incidentsRights,
    rights,
    sniplets: {
        'incident-form': incidentForm,
        'punishment-form': punishmentForm,
        'incidents-manage/sniplet-incidents-type-manage': incidentsTypeManage,
        'incidents-manage/sniplet-incidents-places-manage': incidentsPlaceManage,
        'incidents-manage/sniplet-incidents-protagonists-manage': incidentsProtagonistsManage,
        'incidents-manage/sniplet-incidents-seriousness-manage': incidentsSeriousnessManage,
        'incidents-manage/sniplet-incidents-partners-manage': incidentsPartnersManage,
        'incidents-manage/sniplet-incidents-manage-lightbox': incidentsManageLightbox,
        'memento/incidents': incidentsMementoWidget,
        'punishments-manage/sniplet-punishments-type-manage': punishmentsTypeManage
    }
});
