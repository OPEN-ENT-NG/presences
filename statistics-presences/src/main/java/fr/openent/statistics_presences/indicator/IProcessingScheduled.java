package fr.openent.statistics_presences.indicator;

import io.vertx.core.Handler;

public interface IProcessingScheduled<T> extends Handler<T> {
    /**
     * Process the schedule normally
     *
     * @param event Event
     */
    void process(T event);

    /**
     * This methode is launch when a worker is already in progress
     *
     * @param event Event
     */
    void alreadyInProgress(T event);

    @Override
    default void handle(T event) {
        tryStart(event);
    }

    /**
     * @param event Event
     * @return True if not worker is running, false if a worker is already running
     */
    default boolean tryStart(T event) {
        if (ProcessingScheduledHolder.FINISH) {
            ProcessingScheduledHolder.INSTANCE = this;
            ProcessingScheduledHolder.FINISH = false;
            process(event);
            return true;
        } else {
            alreadyInProgress(event);
            return false;
        }
    }

    class ProcessingScheduledHolder {
        private static IProcessingScheduled<?> INSTANCE = null;
        private static boolean FINISH = true;

        private ProcessingScheduledHolder() {
        }

        public static void finish() {
            ProcessingScheduledHolder.FINISH = true;
        }
    }
}
