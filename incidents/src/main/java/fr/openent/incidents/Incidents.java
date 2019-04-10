package fr.openent.incidents;

import fr.openent.incidents.controllers.IncidentsController;
import org.entcore.common.http.BaseServer;

public class Incidents extends BaseServer {

	@Override
	public void start() throws Exception {
		super.start();

        addController(new IncidentsController());
	}

}
