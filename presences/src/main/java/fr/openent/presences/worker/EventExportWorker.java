package fr.openent.presences.worker;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.service.ArchiveService;
import fr.openent.presences.service.CommonPresencesServiceFactory;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;
import org.vertx.java.busmods.BusModBase;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class EventExportWorker extends BusModBase implements Handler<Message<JsonObject>> {

    Logger log = LoggerFactory.getLogger(EventExportWorker.class);
    EmailSender emailSender;
    private ArchiveService archiveService;
    String locale;
    String domain;

    @Override
    public void start() {
        super.start();
        Storage storage = new StorageFactory(vertx, config).getStorage();
        CommonPresencesServiceFactory commonPresencesServiceFactory = new CommonPresencesServiceFactory(vertx, storage);
        this.emailSender = new EmailFactory(vertx, config).getSender();
        this.archiveService = commonPresencesServiceFactory.archiveService();
        eb.consumer(this.getClass().getName(), this);
    }

    @Override
    public void handle(Message<JsonObject> eventMessage) {
        eventMessage.reply(new JsonObject().put("status", "ok"));
        log.info("[" + this.getClass().getSimpleName() + "] receiving from route /event/archives/export");
        JsonArray structures = eventMessage.body().getJsonArray(Field.STRUCTURE, new JsonArray());
        locale = eventMessage.body().getString(Field.LOCALE);
        domain = eventMessage.body().getString(Field.DOMAIN);

        archiveService.archiveEventsExport(structures, domain, locale)
                .compose(this::sendReport)
                .onSuccess(success -> log.info("[Presences@EventExportWorker::handle] Event export worker success" ))
                .onFailure(error -> log.error("[Presences@EventExportWorker::handle] " +
                        "Processing Event Export Worker task failed. See previous logs: ", error.getMessage()));
    }

    @SuppressWarnings("unchecked")
    private Future<Void> sendReport(JsonArray files) {
        log.info("[" + this.getClass().getSimpleName() + "] - sendReport");

        Promise<Void> promise = Promise.promise();
        List<Future<Void>> futures = new ArrayList<>();

        JsonArray recipients = config.getJsonArray("mails-list-export", new JsonArray());

        String title = String.format("[%s] Export event", config.getString("host"));

        JsonArray filesToSend = new JsonArray();

        ((List<JsonObject>) files.getList()).forEach(file -> {
//            String base64Content = Base64.getEncoder().encodeToString(file.getString(Field.CONTENTS).getBytes(StandardCharsets.UTF_8));
            JsonObject formattedFile = new JsonObject()
                    .put(Field.NAME, file.getString(Field.NAME))
                    .put(Field.CONTENT, file.getString(Field.CONTENTS));
            filesToSend.add(formattedFile);
        });

        for (int i = 0; i < recipients.size(); i++) {
            futures.add(sendMail(recipients.getString(i), title, filesToSend));
        }

        FutureHelper.join(futures)
                .onSuccess(ar -> promise.complete())
                .onFailure(promise::fail);

        return promise.future();
    }

    private String description() {
        return "<div>" + I18n.getInstance().translate("presences.csv.report.from", domain, locale) + " " +
                DateHelper.getCurrentDayWithHours() + "</div>";
    }

    private Future<Void> sendMail(String recipient, String title, JsonArray attachments) {
        Promise<Void> promise = Promise.promise();

        emailSender.sendEmail(null, recipient, null, null, title, attachments, description(), null, false, event -> {
            if (event.failed()) {
                log.error("[Presences@EventExportWorker::sendMail] failed to send mail: ", event.cause().getMessage());
                promise.fail(event.cause().getCause());
            } else {
                promise.complete();
            }
        });

        return promise.future();
    }
}
