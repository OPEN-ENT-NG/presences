package fr.openent.incidents.helper.init;

import fr.openent.incidents.model.*;
import fr.openent.presences.enums.InitTypeEnum;
import org.entcore.common.http.request.JsonHttpServerRequest;

import java.util.List;

public interface IInitIncidentsHelper {
    Init1DIncidentsHelper init1DIncidentsHelper = new Init1DIncidentsHelper();
    Init2DIncidentsHelper init2DIncidentsHelper = new Init2DIncidentsHelper();

    static IInitIncidentsHelper getInstance(InitTypeEnum initTypeEnum) {
        return (initTypeEnum == InitTypeEnum.ONE_D) ? init1DIncidentsHelper : init2DIncidentsHelper;
    }

    List<IncidentType> getIncidentTypes();

    List<Place> getPlaces();

    List<ProtagonistType> getProtagonistTypes();

    List<Seriousness> getSeriousnessTypes(JsonHttpServerRequest request);

    List<Partner> getPartners();

    List<PunishmentType> getPunishmentTypes();
}
