package fr.openent.statistics_presences.service;

import fr.openent.presences.common.service.*;
import fr.openent.presences.common.service.impl.*;
import fr.openent.statistics_presences.service.impl.DefaultStatisticsPresencesService;
import fr.openent.statistics_presences.service.impl.DefaultStatisticsService;
import fr.openent.statistics_presences.service.impl.DefaultStatisticsWeeklyAudiencesService;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;

import java.util.Map;

public class CommonServiceFactory {
    private final Vertx vertx;
    private final StatisticsPresencesService statisticsPresencesService;
    private final StatisticsService statisticsService;

    public CommonServiceFactory(Vertx vertx) {
        this.vertx = vertx;
        statisticsPresencesService = new DefaultStatisticsPresencesService(this);
        statisticsService = new DefaultStatisticsService("");
    }

    public UserService userService() {
        return new DefaultUserService();
    }

    public GroupService groupService() { return new DefaultGroupService(this.vertx.eventBus()); }

    /**
     * @deprecated Replaced by {@link #getStatisticsPresencesService()}
     */
    @Deprecated
    public StatisticsPresencesService statisticsPresencesService() {
        return new DefaultStatisticsPresencesService(this);
    }

    public StatisticsWeeklyAudiencesService statisticsWeeklyAudiencesService() {
        return new DefaultStatisticsWeeklyAudiencesService(this);
    }

    public EventBus eventBus() {
        return this.vertx.eventBus();
    }

    public Vertx vertx() {
        return this.vertx;
    }

    public StatisticsPresencesService getStatisticsPresencesService() {
        return statisticsPresencesService;
    }

    public StatisticsService getStatisticsService() {
        return statisticsService;
    }
}
