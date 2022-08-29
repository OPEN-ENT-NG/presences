package fr.openent.incidents.helper.init;

import fr.openent.incidents.model.*;

import java.util.ArrayList;
import java.util.List;

public class Init1DIncidentsHelper implements IInitIncidentsHelper {
    private static final int NB_INCIDENT_PLACE = 6;
    private static final int NB_PARTNERS = 3;

    protected Init1DIncidentsHelper() {
    }

    @Override
    public List<IncidentType> getIncidentTypes() {
        return IInitIncidentsHelper.init2DIncidentsHelper.getIncidentTypes();
    }

    @Override
    public List<Place> getPlaces() {
        List<Place> placeList = new ArrayList<>();
        for (int i = 0; i < NB_INCIDENT_PLACE; i++) {
            Place place = new Place(i)
                    .setLabel("incidents.init.1d.incident.place." + i);
            placeList.add(place);
        }
        return placeList;
    }

    @Override
    public List<ProtagonistType> getProtagonistTypes() {
        return IInitIncidentsHelper.init2DIncidentsHelper.getProtagonistTypes();
    }

    @Override
    public List<Seriousness> getSeriousnessTypes() {
        return IInitIncidentsHelper.init2DIncidentsHelper.getSeriousnessTypes();
    }

    @Override
    public List<Partner> getPartners() {
        List<Partner> partnerList = new ArrayList<>();
        for (int i = 0; i < NB_PARTNERS; i++) {
            Partner partner = new Partner(i)
                    .setLabel("incidents.init.1d.incident.partner." + i);
            partnerList.add(partner);
        }
        return partnerList;
    }

    @Override
    public List<PunishmentType> getPunishmentTypes() {
        List<PunishmentType> punishmentTypeList = new ArrayList<>();

        punishmentTypeList.add(new PunishmentType(null, "incidents.init.1d.incident.punishment.type.0", "PUNISHMENT", 1, false));
        punishmentTypeList.add(new PunishmentType(null, "incidents.init.1d.incident.punishment.type.1", "PUNISHMENT", 3, false));
        punishmentTypeList.add(new PunishmentType(null, "incidents.init.1d.incident.punishment.type.2", "SANCTION", 3, false));
        punishmentTypeList.add(new PunishmentType(null, "incidents.init.1d.incident.punishment.type.3", "SANCTION", 4, false));
        punishmentTypeList.add(new PunishmentType(null, "incidents.init.1d.incident.punishment.type.4", "SANCTION", 4, false));
        punishmentTypeList.add(new PunishmentType(null, "incidents.init.1d.incident.punishment.type.5", "SANCTION", 4, false));

        return punishmentTypeList;
    }
}
