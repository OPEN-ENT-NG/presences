package fr.openent.presences.common.presences;

import io.vertx.core.eventbus.EventBus;

public class Presences {
    private String address = "fr.openent.presences";
    private EventBus eb;

    private Presences() {
    }

    public static Presences getInstance() {
        return PresencesHolder.instance;
    }

    public void init(EventBus eb) {
        this.eb = eb;
    }

    private static class PresencesHolder {
        private static final Presences instance = new Presences();

        private PresencesHolder() {
        }
    }
}
