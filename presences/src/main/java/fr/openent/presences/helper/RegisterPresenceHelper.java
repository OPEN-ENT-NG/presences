package fr.openent.presences.helper;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.PersonHelper;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.enums.Events;
import fr.openent.presences.model.Course;
import fr.openent.presences.model.Event.RegisterEvent;
import fr.openent.presences.model.Person.User;
import fr.openent.presences.model.Register;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RegisterPresenceHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterPresenceHelper.class);
    private CourseHelper courseHelper;
    private UserService userService;
    private PersonHelper personHelper;

    public RegisterPresenceHelper(EventBus eb) {
        this.courseHelper = new CourseHelper(eb);
        this.userService = new DefaultUserService();
        this.personHelper = new PersonHelper();
    }

    /**
     * Convert JsonArray into register list
     *
     * @param array               JsonArray response
     * @param mandatoryAttributes List of mandatory attributes
     * @return new list of events
     */
    public static List<Register> getRegisterListFromJsonArray(JsonArray array, List<String> mandatoryAttributes) {
        List<Register> registerList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            Register register = new Register((JsonObject) o, mandatoryAttributes);
            registerList.add(register);
        }
        return registerList;
    }

    /**
     * Convert JsonArray into register events list
     *
     * @param array JsonArray response
     * @return new list of events
     */
    public static List<RegisterEvent> getRegisterEventListFromJsonArray(JsonArray array) {
        List<RegisterEvent> registerEventList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            RegisterEvent registerEvent = new RegisterEvent((JsonObject) o);
            registerEventList.add(registerEvent);
        }
        return registerEventList;
    }

    @SuppressWarnings("unchecked")
    public void addCourseToStudentEvent(JsonObject register, Handler<AsyncResult<JsonObject>> handler) {
        List<String> groupName = ((List<JsonObject>) register.getJsonArray("students")
                .getList())
                .stream().map(student -> student.getString("group_name"))
                .collect(Collectors.toList());
        List<String> teachersIds = ((List<JsonObject>) register.getJsonArray("teachers")
                .getList())
                .stream().map(teacher -> teacher.getString("id"))
                .collect(Collectors.toList());
        String date = DateHelper.getDateString(register.getString("start_date"), DateHelper.YEAR_MONTH_DAY);
        courseHelper.getCoursesList(register.getString("structure_id"), teachersIds, groupName, date, date, courseAsyncResult -> {
            if (courseAsyncResult.failed()) {
                String message = "[Presences@EventHelper] Failed to retrieve courses for existing event";
                LOGGER.error(message);
                handler.handle(Future.failedFuture(message + " " + courseAsyncResult.cause()));
            } else {
                List<Course> courses = courseAsyncResult.result();
                ((List<JsonObject>) register.getJsonArray("students").getList()).forEach(student -> {
                    ((List<JsonObject>) student.getJsonArray("day_history").getList()).forEach(dayHistory -> {
                        ((List<JsonObject>) dayHistory.getJsonArray("events").getList()).forEach(event -> {
                            for (Course course : courses) {
                                try {
                                    boolean isDateBetween = DateHelper.isBetween(
                                            event.getString("start_date"), event.getString("end_date"),
                                            course.getStartDate(), course.getEndDate(),
                                            DateHelper.SQL_FORMAT, DateHelper.MONGO_FORMAT
                                    );
                                    if (!event.getString("type").toUpperCase().equals(Events.ABSENCE.toString()) &&
                                            !event.containsKey("course") && isDateBetween) {
                                        event.put("course", course.toJSON());
                                    }
                                } catch (ParseException e) {
                                    String message = "[Presences@RegisterPresenceHelper] Failed to check if event's " +
                                            "date is between course's date";
                                    LOGGER.error(message, e);
                                }
                            }
                        });
                    });
                });
                handler.handle(Future.succeededFuture(register));
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void addOwnerToStudentEvents(JsonObject register, Future<JsonObject> future) {
        List<String> userIds = this.getAllOwnerIds(register.getJsonArray("students"));
        userService.getUsers(userIds, result -> {
            if (result.isLeft()) {
                String message = "[Presences@EventHelper] Failed to retrieve users info";
                LOGGER.error(message);
                future.fail(message);
            }
            List<User> owners = personHelper.getUserListFromJsonArray(result.right().getValue());
            Map<String, User> userMap = new HashMap<>();
            owners.forEach(owner -> userMap.put(owner.getId(), owner));
            ((List<JsonObject>) register.getJsonArray("students").getList()).forEach(student ->
                    ((List<JsonObject>) student.getJsonArray("day_history").getList()).forEach(dayHistory ->
                            ((List<JsonObject>) dayHistory.getJsonArray("events").getList()).forEach(event -> {
                                if (!event.getString("type").toUpperCase().equals(Events.ABSENCE.toString()) && event.getValue("owner") instanceof String) {
                                    event.put("owner", userMap.getOrDefault(event.getString("owner"), null).toJSON());
                                }
                            })
                    )
            );
            future.complete();
        });
    }

    @SuppressWarnings("unchecked")
    private List<String> getAllOwnerIds(JsonArray students) {
        List<String> userIds = new ArrayList<>();
        ((List<JsonObject>) students.getList()).forEach(student ->
                ((List<JsonObject>) student.getJsonArray("day_history").getList()).forEach(dayHistory ->
                        ((List<JsonObject>) dayHistory.getJsonArray("events").getList()).forEach(event -> {
                            if (event.containsKey("owner") && !userIds.contains(event.getString("owner"))) {
                                userIds.add(event.getString("owner"));
                            }
                        })
                )
        );
        return userIds;
    }
}
