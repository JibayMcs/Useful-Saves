package fr.zeamateis.usefulsaves.server.task;

import java.util.concurrent.TimeUnit;

/**
 * @author ZeAmateis
 */
public class TaskObject {

    private int period;
    private TimeUnit timeUnit;
    private boolean flush;

    public TaskObject(int period, TimeUnit timeUnit, boolean flush) {
        this.period = period;
        this.timeUnit = timeUnit;
        this.flush = flush;
    }

    public int getPeriod() {
        return period;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public boolean isFlush() {
        return flush;
    }

    @Override
    public String toString() {
        return "TaskObject{" +
                "period=" + period +
                ", timeUnit='" + timeUnit + '\'' +
                ", flush=" + flush +
                '}';
    }
}
