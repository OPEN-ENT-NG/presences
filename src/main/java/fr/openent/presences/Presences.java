package fr.openent.presences;

import fr.openent.presences.controller.PresenceController;
import org.entcore.common.http.BaseServer;

public class Presences extends BaseServer {

	@Override
	public void start() throws Exception {
		super.start();
		addController(new PresenceController());
	}

}
