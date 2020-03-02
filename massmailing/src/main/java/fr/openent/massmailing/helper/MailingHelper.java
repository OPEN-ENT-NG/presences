package fr.openent.massmailing.helper;

import fr.openent.massmailing.model.Mailing.Mailing;
import fr.openent.massmailing.model.Mailing.MailingEvent;
import fr.openent.presences.common.helper.PersonHelper;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.model.Person.Student;
import fr.openent.presences.model.Person.User;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;


public class MailingHelper {

    private PersonHelper personHelper;
    private UserService userService;

    public MailingHelper() {
        this.personHelper = new PersonHelper();
        this.userService = new DefaultUserService();
    }

    /**
     * Convert JsonArray Mailings into Mailings List
     *
     * @param mailingsJsonArray JsonArray data
     * @return new List of Mailing
     */
    public List<Mailing> getMailingListFromJsonArray(JsonArray mailingsJsonArray) {
        List<Mailing> mailings = new ArrayList<>();
        for (Object o : mailingsJsonArray) {
            if (!(o instanceof JsonObject)) continue;
            Mailing mailing = new Mailing((JsonObject) o);
            mailings.add(mailing);
        }
        return mailings;
    }

    /**
     * Convert List Mailings into Mailings JsonArray
     *
     * @param mailingsList mailings list
     * @return new JsonArray of mailings
     */
    public JsonArray toMailingsJsonArray(List<Mailing> mailingsList) {
        JsonArray mailings = new JsonArray();
        for (Mailing mailing : mailingsList) {
            mailings.add(mailing.toJSON());
        }
        return mailings;
    }

    /**
     * Convert JsonArray Mailing Event into Mailing Event List
     *
     * @param mailingEventsJsonArray JsonArray data
     * @return new List of Mailing Event
     */
    public List<MailingEvent> getMailingEventListFromJsonArray(JsonArray mailingEventsJsonArray) {
        List<MailingEvent> mailingEvents = new ArrayList<>();
        for (Object o : mailingEventsJsonArray) {
            if (!(o instanceof JsonObject)) continue;
            MailingEvent mailingEvent = new MailingEvent((JsonObject) o);
            mailingEvents.add(mailingEvent);
        }
        return mailingEvents;
    }

    /**
     * Convert List Mailing Event into Mailing Event JsonArray
     *
     * @param mailingEventsList Mailing Event list
     * @return new JsonArray of Mailing Event
     */
    public JsonArray toMailingEventJsonArray(List<MailingEvent> mailingEventsList) {
        JsonArray mailings = new JsonArray();
        for (MailingEvent mailing : mailingEventsList) {
            mailings.add(mailing.toJSON());
        }
        return mailings;
    }

    /**
     * add Student Object to Mailing->Student
     *
     * @param structureId Structure identifier
     * @param mailings    Mailings list
     * @param studentIds  Students list
     * @param future      Future to complete
     */
    public void addStudentToMailing(String structureId, List<Mailing> mailings, List<String> studentIds, Future<JsonObject> future) {
        if (studentIds.isEmpty()) {
            future.complete();
            return;
        }
        personHelper.getStudentsInfo(structureId, studentIds, studentAsync -> {
            List<Student> students = personHelper.getStudentListFromJsonArray(studentAsync.right().getValue());
            for (Mailing mailing : mailings) {
                for (Student student : students) {
                    if (mailing.getStudent().getId().equals(student.getId())) {
                        mailing.setStudent(student);
                    }
                }
            }
            future.complete();
        });
    }

    /**
     * add Recipient Object to Mailing->Recipient
     *
     * @param mailings     Mailings list
     * @param recipientIds Recipients | relatives | users list
     * @param future       Future to complete
     */
    public void addRecipientToMailing(List<Mailing> mailings, List<String> recipientIds, Future<JsonObject> future) {
        if (recipientIds.isEmpty()) {
            future.complete();
            return;
        }
        userService.getUsers(recipientIds, recipientAsync -> {
            List<User> recipients = personHelper.getUserListFromJsonArray(recipientAsync.right().getValue());
            for (Mailing mailing : mailings) {
                for (User recipient : recipients) {
                    if (mailing.getRecipient().getId().equals(recipient.getId())) {
                        mailing.getRecipient().setName(recipient.getName());
                    }
                }
            }
            future.complete();
        });
    }
}
