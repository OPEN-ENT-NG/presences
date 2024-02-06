package fr.openent.massmailing.mailing;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.openent.presences.common.helper.FutureHelper;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Mail extends MassMailingProcessor {
    private EmailSender emailSender = Massmailing.emailSender;

    public Mail(String structure, Template template, Boolean massmailed,
                List<MassmailingType> massmailingTypeList, List<Integer> reasons, List<Integer> punishmentsTypes,
                List<Integer> sanctionsTypes, String start, String end,
                Boolean noReason, boolean isMultiple, JsonObject students) {
        super(MailingType.MAIL, structure, template, massmailed, massmailingTypeList, reasons, punishmentsTypes, sanctionsTypes,
                start, end, noReason,  isMultiple, students);
    }

    @Override
    public void massmail(HttpServerRequest request, Handler<Either<String, Boolean>> handler) {
        super.process(event -> {
            if (event.isLeft()) {
                String errorMessage = "[Massmailing@Mail] Failed to process mailing";
                LOGGER.error(String.format("%s %s", errorMessage, event.left().getValue()));
                handler.handle(new Either.Left<>(errorMessage));
                return;
            }

            List<JsonObject> mails = event.right().getValue();
            List<Future> futures = new ArrayList<>();
            for (JsonObject mail : mails) {
                Future<JsonObject> future = Future.future();
                futures.add(future);
                send(mail, FutureHelper.handlerJsonObject(future));
            }

            CompositeFuture.join(futures).setHandler(asyncEvent -> {
                if (asyncEvent.failed()) {
                    String message = "[Massmailing@Mail::send] Failed to send mails";
                    LOGGER.error(String.format("%s %s", message, asyncEvent.cause().getMessage()));
                    handler.handle(new Either.Left<>(message));
                }
                else handler.handle(new Either.Right<>(asyncEvent.succeeded()));
            });
        });
    }

    void send(JsonObject mail, Handler<Either<String, JsonObject>> handler) {
        String contact = mail.getString("contact");
        String message = mail.getString("message");
        String subject = I18n.getInstance().translate("massmailing.mail.subject", getTemplate().getDomain(), getTemplate().getLocale(), mail.getString("studentDisplayName").toUpperCase());
        emailSender.sendEmail(null, contact, null, null, subject, message, null, false, event -> {
            if (event.failed()) {
                String errorMessage = "[Massmailing@Mail] Failed to send mail";
                //TODO Réaliser une sauvegarde
                LOGGER.error(String.format("%s %s %s", errorMessage, event.cause().getMessage(), mail));
                handler.handle(new Either.Left<>(errorMessage));
                return;
            }

            saveMassmailing(mail, handler);
        });
    }
}
