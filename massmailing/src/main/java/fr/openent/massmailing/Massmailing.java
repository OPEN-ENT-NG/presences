package fr.openent.massmailing;

import fr.openent.massmailing.controller.MassmailingController;
import fr.openent.massmailing.controller.SettingsController;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.starter.DatabaseStarter;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.BaseServer;

import java.util.HashMap;
import java.util.Iterator;

public class Massmailing extends BaseServer {

    public static final String dbSchema = "massmailing";

    public static final String MANAGE = "massmailing.manage";

    static HashMap<MailingType, Boolean> types;

    @Override
    public void start() throws Exception {
        super.start();
        EventBus eb = getEventBus(vertx);
        types = mailingsConfig();

        addController(new MassmailingController());
        addController(new SettingsController(eb));

        vertx.setTimer(30000, handle -> new DatabaseStarter().init());
    }

    private HashMap<MailingType, Boolean> mailingsConfig() {
        HashMap<MailingType, Boolean> conf = new HashMap<>();
        JsonObject mailings = config.getJsonObject("mailings", new JsonObject());
        Iterator<String> it = mailings.fieldNames().iterator();
        while (it.hasNext()) {
            String mailing = it.next();
            conf.put(MailingType.valueOf(mailing), mailings.getBoolean(mailing));
        }

        return conf;
    }

}
