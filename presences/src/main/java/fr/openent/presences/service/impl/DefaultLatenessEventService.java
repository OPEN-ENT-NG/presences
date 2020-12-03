package fr.openent.presences.service.impl;

import fr.openent.presences.model.Event.EventBody;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.LatenessEventService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserInfos;

public class DefaultLatenessEventService implements LatenessEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLatenessEventService.class);
    private final EventService eventService;

    public DefaultLatenessEventService(EventBus eb) {
        eventService = new DefaultEventService(eb);
    }

    @Override
    public void create(EventBody eventBody, UserInfos userInfos, Handler<Either<String, JsonObject>> handler) {
        // gros traitement à faire avant de vouloir faire le create
        // @ todo faire le traitement ici

        eventService.create(eventBody.toJSON(), userInfos, event -> {
            if (event.isLeft()) {
                LOGGER.error("[Presences@DefaultLatenessEventService::create] Failed to create lateness event ", event.left().getValue());
                handler.handle(new Either.Left<>(event.left().getValue()));
            } else {
                handler.handle(new Either.Right<>(event.right().getValue()));
            }
        });
    }

    @Override
    public void update(Integer eventId, EventBody eventBody, Handler<Either<String, JsonObject>> handler) {
        // traitement
        // @ todo faire le traitement ici

        eventService.updateEvent(eventId, eventBody.toJSON(), event -> {
            if (event.isLeft()) {
                LOGGER.error("[Presences@DefaultLatenessEventService::update] Failed to update lateness event ", event.left().getValue());
                handler.handle(new Either.Left<>(event.left().getValue()));
            } else {
                handler.handle(new Either.Right<>(event.right().getValue()));
            }
        });
    }
}
