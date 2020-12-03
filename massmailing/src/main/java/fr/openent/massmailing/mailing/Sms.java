package fr.openent.massmailing.mailing;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.openent.presences.common.helper.FutureHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class Sms extends MassMailingProcessor {
    private EventBus eventBus;

    public Sms(EventBus eb, String structure, Template template, Boolean massmailed, List<MassmailingType> massmailingTypeList,
               List<Integer> reasons, List<Integer> punishmentsTypes, List<Integer> sanctionsTypes, String start, String end,
               Boolean noReason, JsonObject students) {
        super(MailingType.SMS, structure, template, massmailed, massmailingTypeList, reasons, punishmentsTypes, sanctionsTypes,
                start, end, noReason, students);
        this.eventBus = eb;
    }


    @Override
    public void massmail(Handler<Either<String, Boolean>> handler) {
        super.process(event -> {
            if (event.isLeft()) {
                LOGGER.error("[Massmailing@Sms] Failed to process mailing", event.left().getValue());
                handler.handle(new Either.Left<>(event.left().getValue()));
                return;
            }

            List<JsonObject> smsList = event.right().getValue();
            List<Future> futures = new ArrayList<>();
            smsList.forEach(sms -> {
                if (sms.containsKey("contact") && !"".equals(sms.getString("contact"))) {
                    Future<JsonObject> future = Future.future();
                    futures.add(future);
                    send(sms, FutureHelper.handlerJsonObject(future));
                }
            });

            CompositeFuture.join(futures).setHandler(asyncEvent -> {
                if (asyncEvent.failed()) handler.handle(new Either.Left<>(asyncEvent.cause().toString()));
                else handler.handle(new Either.Right<>(asyncEvent.succeeded()));
            });
        });
    }

    private void send(JsonObject sms, Handler<Either<String, JsonObject>> handler) {

        String message = sms.getString("message");
        int maxLength = 160;
        if (message.length() > maxLength) {
            message = message.substring(0, maxLength - 3);
            message += "...";
        }

        JsonObject parameters = new JsonObject()
                .put("receivers", new JsonArray().add(sms.getString("contact")))
                .put("message", message)
                .put("senderForResponse", true)
                .put("noStopClause", true);
        JsonObject smsObject = new JsonObject()
                .put("provider", "OVH")
                .put("action", "send-sms")
                .put("parameters", parameters);

        eventBus.send(Massmailing.SMS_ADDRESS, smsObject, handlerToAsyncHandler(event -> {
            if ("error".equals(event.body().getString("status"))) {
                handler.handle(new Either.Left<>(event.body().getString("message")));
            } else {
                saveMassmailing(sms, handler);
            }
        }));
    }
}
