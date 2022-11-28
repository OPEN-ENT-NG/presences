package fr.openent.incidents.helper.init;

import fr.openent.incidents.model.*;
import fr.openent.presences.enums.InitTypeEnum;
import org.entcore.common.http.request.JsonHttpServerRequest;

import java.util.List;

public interface IInitIncidentsHelper {
    DefaultInit1DIncidentsHelper defaultInit1DIncidentsHelper = new DefaultInit1DIncidentsHelper();
    DefaultInit2DIncidentsHelper defaultInit2DIncidentsHelper = new DefaultInit2DIncidentsHelper();

    //Must be changed if implementing a different function per platform
    static IInitIncidentsHelper getInstance(InitTypeEnum initTypeEnum, String ignoredPlatform) {
        return getDefaultInstance(initTypeEnum);
    }

    static IInitIncidentsHelper getDefaultInstance(InitTypeEnum initTypeEnum) {
        return (initTypeEnum == InitTypeEnum.ONE_D) ? defaultInit1DIncidentsHelper : defaultInit2DIncidentsHelper;
    }

    List<IncidentType> getIncidentTypes();

    List<Place> getPlaces();

    List<ProtagonistType> getProtagonistTypes();

    List<Seriousness> getSeriousnessTypes(JsonHttpServerRequest request);

    List<Partner> getPartners();

    List<PunishmentType> getPunishmentTypes();
}
