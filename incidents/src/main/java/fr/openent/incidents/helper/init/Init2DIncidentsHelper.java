package fr.openent.incidents.helper.init;

import fr.openent.incidents.model.*;
import io.vertx.core.json.JsonArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Init2DIncidentsHelper implements IInitIncidentsHelper {
    private static final int NB_INCIDENT_TYPE = 6;
    private static final int NB_INCIDENT_PLACE = 6;
    private static final int NB_PROTAGONIST_TYPES = 4;
    private static final int NB_PARTNERS = 5;

    protected Init2DIncidentsHelper() {
    }

    @Override
    public List<IncidentType> getIncidentTypes() {
        List<IncidentType> incidentTypeList = new ArrayList<>();

        for (int i = 0; i < NB_INCIDENT_TYPE; i++) {
            IncidentType incidentType = new IncidentType(i)
                    .setLabel("incidents.init.2d.incident.type." + i);
            incidentTypeList.add(incidentType);
        }
        return incidentTypeList;
    }

    @Override
    public List<Place> getPlaces() {
        List<Place> placeList = new ArrayList<>();
        for (int i = 0; i < NB_INCIDENT_PLACE; i++) {
            Place place = new Place(i)
                    .setLabel("incidents.init.2d.incident.place." + i);
            placeList.add(place);
        }
        return placeList;
    }

    @Override
    public List<ProtagonistType> getProtagonistTypes() {
        List<ProtagonistType> protagonistTypeList = new ArrayList<>();
        for (int i = 0; i < NB_PROTAGONIST_TYPES; i++) {
            ProtagonistType protagonistType = new ProtagonistType(i)
                    .setLabel("incidents.init.2d.incident.protagonist.type." + i);
            protagonistTypeList.add(protagonistType);
        }
        return protagonistTypeList;
    }

    @Override
    public List<Seriousness> getSeriousnessTypes() {
        List<Integer> seriousnessLevel = Arrays.asList(0, 2, 4, 5, 7);
        List<Seriousness> seriousnessList = new ArrayList<>();
        for (int i = 0; i < seriousnessLevel.size(); i++) {
            Seriousness seriousness = new Seriousness(i)
                    .setLevel(seriousnessLevel.get(1))
                    .setLabel("incidents.init.2d.incident.seriousness." + i);
            seriousnessList.add(seriousness);
        }
        return seriousnessList;
    }

    @Override
    public List<Partner> getPartners() {
        List<Partner> partnerList = new ArrayList<>();
        for (int i = 0; i < NB_PARTNERS; i++) {
            Partner partner = new Partner(i)
                    .setLabel("incidents.init.2d.incident.partner." + i);
            partnerList.add(partner);
        }
        return partnerList;
    }

    @Override
    public List<PunishmentType> getPunishmentTypes() {
        List<PunishmentType> punishmentTypeList = new ArrayList<>();

        punishmentTypeList.add(new PunishmentType(null, "incidents.init.2d.incident.punishment.type.0", "PUNISHMENT", 1, false));
        punishmentTypeList.add(new PunishmentType(null, "incidents.init.2d.incident.punishment.type.1", "PUNISHMENT", 2, false));
        punishmentTypeList.add(new PunishmentType(null, "incidents.init.2d.incident.punishment.type.2", "PUNISHMENT", 3, false));
        punishmentTypeList.add(new PunishmentType(null, "incidents.init.2d.incident.punishment.type.3", "SANCTION", 3, false));
        punishmentTypeList.add(new PunishmentType(null, "incidents.init.2d.incident.punishment.type.4", "SANCTION", 3, false));
        punishmentTypeList.add(new PunishmentType(null, "incidents.init.2d.incident.punishment.type.5", "SANCTION", 4, false));
        punishmentTypeList.add(new PunishmentType(null, "incidents.init.2d.incident.punishment.type.6", "SANCTION", 4, false));
        punishmentTypeList.add(new PunishmentType(null, "incidents.init.2d.incident.punishment.type.7", "SANCTION", 4, false));

        return punishmentTypeList;
    }
}
