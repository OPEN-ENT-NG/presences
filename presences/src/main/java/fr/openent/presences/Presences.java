package fr.openent.presences;

import fr.openent.presences.controller.ExemptionController;
import fr.openent.presences.controller.PresenceController;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.http.BaseServer;

public class Presences extends BaseServer {

	public static String dbSchema;
	public static String ebViescoAddress;

	@Override
	public void start() throws Exception {
		super.start();
        dbSchema = config.getString("db-schema");
		ebViescoAddress = "viescolaire";
		final EventBus eb = getEventBus(vertx);

		addController(new PresenceController());
		addController(new ExemptionController(eb));
	}

}
