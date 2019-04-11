package fr.openent.presences;

import fr.openent.presences.controller.PresenceController;
import org.entcore.common.http.BaseServer;

public class Presences extends BaseServer {

    public static String dbSchema;

    @Override
	public void start() throws Exception {
		super.start();
        dbSchema = config.getString("db-schema");
		addController(new PresenceController());
	}

}
