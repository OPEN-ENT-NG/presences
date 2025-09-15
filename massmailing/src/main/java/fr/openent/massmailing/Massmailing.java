package fr.openent.massmailing;

import fr.openent.massmailing.controller.*;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.starter.DatabaseStarter;
import fr.openent.presences.common.incidents.Incidents;
import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DB;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sms.SmsSenderFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

import java.util.HashMap;
import java.util.Iterator;

public class Massmailing extends BaseServer {

    public static final String dbSchema = "massmailing";

    public static final String MANAGE = "massmailing.manage";
    public static final String MANAGE_RESTRICTED = "massmailing.manage.restricted";
    public static final String VIEW = "massmailing.view";

    public static String MODULE = "PRESENCES";

    public static Integer PAGE_SIZE = 20;

    static HashMap<MailingType, Boolean> types;
    public static EmailSender emailSender;
    public static WorkspaceHelper workspaceHelper;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
      final Promise<Void> promise = Promise.promise();
      super.start(promise);
      promise.future()
        .compose(e -> this.init())
        .onComplete(startPromise);
    }
    public Future<Void> init() {
        EventBus eb = getEventBus(vertx);
        return StorageFactory.build(vertx, config).compose(storageFactory -> {
            final Storage storage = storageFactory.getStorage();
            types = mailingsConfig();
            emailSender = EmailFactory.getInstance().getSender();
            SmsSenderFactory.getInstance().init(vertx, config);
            workspaceHelper = new WorkspaceHelper(eb, storage);
            Sql sqlAdmin = Sql.createInstance(vertx.eventBus(), Field.SQLPERSISTORADMIN);

            DB.getInstance().init(Neo4j.getInstance(), Sql.getInstance(), MongoDb.getInstance());

            addController(new MassmailingController(eb, vertx, config, storage));
            addController(new SettingsController(eb));
            addController(new EventBusController());
            addController(new MailingController(eb, storage));
            addController(new ConfigController());

            Presences.getInstance().init(eb);
            Incidents.getInstance().init(eb);
            Viescolaire.getInstance().init(eb);

            vertx.setTimer(30000, handle -> new DatabaseStarter().init(sqlAdmin));
            return Future.succeededFuture();
          });
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
