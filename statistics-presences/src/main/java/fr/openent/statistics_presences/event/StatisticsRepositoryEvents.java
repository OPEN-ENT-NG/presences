package fr.openent.statistics_presences.event;

import fr.openent.presences.core.constants.Field;
import fr.openent.statistics_presences.service.CommonServiceFactory;
import fr.openent.statistics_presences.service.StatisticsService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.RepositoryEvents;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StatisticsRepositoryEvents implements RepositoryEvents {
    private static final Logger log = LoggerFactory.getLogger(StatisticsRepositoryEvents.class);
    private final StatisticsService statisticsService;

    public StatisticsRepositoryEvents(CommonServiceFactory commonServiceFactory) {
        this.statisticsService = commonServiceFactory.getStatisticsService();
    }

    @Override
    public void deleteGroups(JsonArray jsonArray) {
        log.info(String.format("[StatisticsPresences@%s::deleteGroups] Delete groups event is not implemented", this.getClass().getSimpleName()));
    }

    @Override
    public void deleteUsers(JsonArray jsonArray) {
        if(jsonArray == null)
            return;
        for(int i = jsonArray.size(); i-- > 0;)
        {
            if(jsonArray.hasNull(i))
                jsonArray.remove(i);
            else if (jsonArray.getJsonObject(i) != null && jsonArray.getJsonObject(i).getString(Field.ID) == null)
                jsonArray.remove(i);
        }
        if(jsonArray.size() == 0)
            return;

        List<String> studentIdList = jsonArray.stream()
                .map(JsonObject.class::cast)
                .map(user -> user.getString(Field.ID))
                .collect(Collectors.toList());

        this.deleteStats(studentIdList).onComplete(ar ->{});
    }

    private Future<Void> deleteStats(List<String> studentIdList) {
        Promise<Void> promise = Promise.promise();

        Function<String, Future<Void>> function = studentId -> {
            log.info(String.format("[StatisticsPresences@StatisticsRepositoryEvents::deleteUsers] Delete stats for user %s", studentId));
            return this.statisticsService.deleteStudentStats(null, studentId, null, null);
        };

        Promise<Void> init = Promise.promise();
        Future<Void> current = init.future();

        for (int i = 0; i < studentIdList.size(); i++) {
            int indice = i;

            current = current.compose(result -> {
                Promise<Void> otherPromise = Promise.promise();
                function.apply(studentIdList.get(indice))
                        .onComplete(ar -> {
                            if (ar.failed()) {
                                log.error(String.format("[StatisticsPresences@StatisticsRepositoryEvents::deleteUsers] " +
                                        "Delete fail for student %s. %s", studentIdList.get(indice), ar.cause()));
                            }
                            otherPromise.complete();
                        });

                return otherPromise.future();
            });
        }
        current.onComplete(ar -> promise.complete());
        init.complete();

        return promise.future();
    }
}
