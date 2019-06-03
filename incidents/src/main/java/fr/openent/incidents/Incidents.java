package fr.openent.incidents;

import fr.openent.incidents.controllers.IncidentsController;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.http.BaseServer;

public class Incidents extends BaseServer {

	public static String dbSchema;
	public static String ebViescoAddress = "viescolaire";
	;
	public static Integer PAGE_SIZE = 20;

	@Override
	public void start() throws Exception {

		super.start();
		dbSchema = config.getString("db-schema");

		final EventBus eb = getEventBus(vertx);
		addController(new IncidentsController(eb));
	}

}
