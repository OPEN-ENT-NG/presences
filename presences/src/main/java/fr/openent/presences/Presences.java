package fr.openent.presences;

import fr.openent.presences.controller.*;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.http.BaseServer;

public class Presences extends BaseServer {

    public static String dbSchema;
    public static String ebViescoAddress = "viescolaire";

    public static final String READ_REGISTER = "presences.register.read";
    public static final String CREATE_REGISTER = "presences.register.create";
    public static final String SEARCH = "presences.search";
    public static final String EXPORT = "presences.export";
    public static final String CREATE_EVENT = "presences.event.create";
    public static final String READ_EXEMPTION = "presences.exemption.read";
    public static final String MANAGE_EXEMPTION = "presences.exemption.manage";

    public static Integer PAGE_SIZE = 20;

    @Override
	public void start() throws Exception {
		super.start();
        dbSchema = config.getString("db-schema");
		ebViescoAddress = "viescolaire";
		final EventBus eb = getEventBus(vertx);

        addController(new PresencesController(eb));
        addController(new CourseController(eb));
        addController(new RegisterController(eb));
        addController(new EventController());
		addController(new ExemptionController(eb));
        addController(new SearchController(eb));
	}

}
