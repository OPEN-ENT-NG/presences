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
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.sms.SmsSender;
import org.entcore.common.sms.SmsSenderFactory;

import java.util.ArrayList;
import java.util.List;

public class Sms extends MassMailingProcessor {
    private EventBus eventBus;

    public Sms(EventBus eb, String structure, Template template, Boolean massmailed, List<MassmailingType> massmailingTypeList,
               List<Integer> reasons, List<Integer> punishmentsTypes, List<Integer> sanctionsTypes, String start, String end,
               Boolean noReason, boolean isMultiple, JsonObject students) {
        super(MailingType.SMS, structure, template, massmailed, massmailingTypeList, reasons, punishmentsTypes, sanctionsTypes,
                start, end, noReason,  isMultiple, students);
        this.eventBus = eb;
    }


    @Override
    public void massmail(HttpServerRequest request, Handler<Either<String, Boolean>> handler) {
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
                    send(request, sms, FutureHelper.handlerJsonObject(future));
                }
            });

            CompositeFuture.join(futures).setHandler(asyncEvent -> {
                if (asyncEvent.failed()) handler.handle(new Either.Left<>(asyncEvent.cause().toString()));
                else handler.handle(new Either.Right<>(asyncEvent.succeeded()));
            });
        });
    }

    private void send(HttpServerRequest request, JsonObject sms, Handler<Either<String, JsonObject>> handler) {

        String message = sms.getString("message");
        int maxLength = 160;
        if (message.length() > maxLength) {
            message = message.substring(0, maxLength - 3);
            message += "...";
        }

        final SmsSender smsSender = SmsSenderFactory.getInstance().newInstance(EventStoreFactory.getFactory().getEventStore(Sms.class.getSimpleName()));
        smsSender.send(request, sms.getString("contact"), message, Massmailing.MODULE)
                .onSuccess(report -> saveMassmailing(sms, handler))
                .onFailure(failure -> {
                    String errorMessage = "[Massmailing@Sms::send] Failed to send sms mailing";
                    LOGGER.error(errorMessage, failure);
                    handler.handle(new Either.Left<>(errorMessage));
                });
    }
}
