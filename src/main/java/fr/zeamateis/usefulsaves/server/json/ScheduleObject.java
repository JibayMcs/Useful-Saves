package fr.zeamateis.usefulsaves.server.json;

import java.util.TimeZone;

/**
 * @author ZeAmateis
 */
public class ScheduleObject {

    private CronObject cronObject;
    private String timeZone;

    private boolean flush;

    public ScheduleObject(CronObject cronObject, TimeZone timeZone, boolean flush) {
        this.cronObject = cronObject;
        this.timeZone = timeZone.getID();
        this.flush = flush;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public boolean isFlush() {
        return flush;
    }

    public CronObject getCronObject() {
        return cronObject;
    }

    @Override
    public String toString() {
        return "ScheduleObject{" +
                "cronObject=" + cronObject +
                ", timeZone='" + timeZone + '\'' +
                ", flush=" + flush +
                '}';
    }
}
