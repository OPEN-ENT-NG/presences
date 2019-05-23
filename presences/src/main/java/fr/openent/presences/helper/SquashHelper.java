package fr.openent.presences.helper;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.service.RegisterService;
import fr.openent.presences.service.impl.DefaultRegisterService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;

public class SquashHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(SquashHelper.class);
    private RegisterService registerService;

    public SquashHelper(EventBus eb) {
        this.registerService = new DefaultRegisterService(eb);
    }

    /**
     * Squash courses with registers. Each course will be squashed with its register.
     *
     * @param structureId Structure identifier
     * @param start       Start period date
     * @param end         End period date
     * @param courses     Course list
     * @param handler     Function handler returning data
     */
    public void squash(String structureId, String start, String end, JsonArray courses, Handler<Either<String, JsonArray>> handler) {
        registerService.list(structureId, start, end, registerEvent -> {
            if (registerEvent.isLeft()) {
                String message = "[Presences@SquashHelper] Failed to retrieve registers";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonObject registers = groupRegisters(registerEvent.right().getValue());
            for (int i = 0; i < courses.size(); i++) {
                boolean found = false;
                int j = 0;
                JsonObject course = courses.getJsonObject(i);
                JsonArray courseRegisters = registers.getJsonArray(course.getString("_id"));
                if (courseRegisters == null) {
                    continue;
                }
                while (!found && j < courseRegisters.size()) {
                    JsonObject register = courseRegisters.getJsonObject(j);
                    try {
                        if (DateHelper.getAbsTimeDiff(course.getString("startDate"), register.getString("start_date")) < DateHelper.TOLERANCE
                                && DateHelper.getAbsTimeDiff(course.getString("endDate"), register.getString("end_date")) < DateHelper.TOLERANCE) {
                            course.put("register_id", register.getInteger("id"));
                            course.put("register_state_id", register.getInteger("state_id"));
                            course.put("notified", register.getBoolean("notified"));
                            found = true;
                        } else {
                            course.put("notified", false);
                        }
                    } catch (ParseException err) {
                        LOGGER.error("[Presences@SquashHelper] Failed to parse date for register " + register.getInteger("id"), err);
                    } finally {
                        j++;
                    }
                }
            }

            handler.handle(new Either.Right<>(courses));
        });
    }

    /**
     * Format registers by course identifier
     *
     * @param registers Registers list
     * @return Json object containing each registers grouped by course identifier
     */
    private JsonObject groupRegisters(JsonArray registers) {
        JsonObject values = new JsonObject();
        JsonObject register, o;
        for (int i = 0; i < registers.size(); i++) {
            register = registers.getJsonObject(i);
            if (!values.containsKey(register.getString("course_id"))) {
                values.put(register.getString("course_id"), new JsonArray());
            }

            o = new JsonObject()
                    .put("id", register.getInteger("id"))
                    .put("start_date", register.getString("start_date"))
                    .put("end_date", register.getString("end_date"))
                    .put("state_id", register.getInteger("state_id"))
                    .put("notified", register.getBoolean("notified"));
            values.getJsonArray(register.getString("course_id")).add(o);
        }

        return values;
    }

}
