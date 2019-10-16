package fr.openent.massmailing;

import fr.openent.massmailing.controller.MassmailingController;
import fr.openent.massmailing.starter.DatabaseStarter;
import org.entcore.common.http.BaseServer;

public class Massmailing extends BaseServer {

    @Override
    public void start() throws Exception {
        super.start();

        addController(new MassmailingController());

        vertx.setTimer(30000, handle -> new DatabaseStarter().init());
    }

}
