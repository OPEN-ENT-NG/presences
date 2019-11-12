package fr.openent.massmailing;

import fr.openent.massmailing.controller.MassmailingController;
import fr.openent.massmailing.controller.SettingsController;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.starter.DatabaseStarter;
import fr.openent.presences.common.presences.Presences;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.StorageFactory;

import java.util.HashMap;
import java.util.Iterator;

public class Massmailing extends BaseServer {

    public static final String dbSchema = "massmailing";

    public static final String MANAGE = "massmailing.manage";
    public static final String VIEW = "massmailing.view";

    static HashMap<MailingType, Boolean> types;
    public static EmailSender emailSender;
    public static WorkspaceHelper workspaceHelper;

    @Override
    public void start() throws Exception {
        super.start();
        EventBus eb = getEventBus(vertx);
        types = mailingsConfig();
        emailSender = new EmailFactory(vertx, config).getSender();
        workspaceHelper = new WorkspaceHelper(eb, new StorageFactory(vertx, config).getStorage());
        addController(new MassmailingController(eb));
        addController(new SettingsController(eb));

        Presences.getInstance().init(eb);

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
