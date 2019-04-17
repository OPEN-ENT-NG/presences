package fr.openent.presences;

import fr.openent.presences.controller.*;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.http.BaseServer;

public class Presences extends BaseServer {

    public static String dbSchema;
    public static String ebViescoAddress = "viescolaire";

    public static final String READ_REGISTER = "presences.register.read";
    public static final String CREATE_REGISTER = "presences.register.create";
    public static final String SEARCH_REGISTER = "presences.register.search";
    public static final String CREATE_EVENT = "presences.event.create";

	@Override
	public void start() throws Exception {
		super.start();
        dbSchema = config.getString("db-schema");
		ebViescoAddress = "viescolaire";
		final EventBus eb = getEventBus(vertx);

		addController(new PresenceController());
        addController(new CourseController(eb));
        addController(new RegisterController(eb));
        addController(new EventController());
		addController(new ExemptionController(eb));
	}

}
