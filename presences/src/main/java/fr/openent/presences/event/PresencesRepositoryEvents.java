package fr.openent.presences.event;

import fr.openent.presences.Presences;
import fr.openent.presences.core.constants.Field;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.RepositoryEvents;

public class PresencesRepositoryEvents implements RepositoryEvents {

    private static final Logger log = LoggerFactory.getLogger(PresencesRepositoryEvents.class);

    private EventBus eb;

    public PresencesRepositoryEvents(EventBus eb) {
        this.eb = eb;
    }

    @Override
    public void deleteGroups(JsonArray jsonArray) {
        log.info(String.format("[Common@%s::deleteGroups] Delete groups event is not implemented", this.getClass().getSimpleName()));
    }

    @Override
    public void deleteUsers(JsonArray jsonArray) {
        log.info(String.format("[Common@%s::deleteUsers] Delete users event is not implemented", this.getClass().getSimpleName()));
    }

    @Override
    public void transition(JsonObject structure) {
        Presences.launchResetAlertsWorker(eb, new JsonObject().put(Field.STRUCTURE, structure));
    }
}
