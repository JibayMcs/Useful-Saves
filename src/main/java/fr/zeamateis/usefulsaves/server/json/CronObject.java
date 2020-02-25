package fr.zeamateis.usefulsaves.server.json;

public class CronObject {
    public String cron;

    public CronObject(String cron) {
        this.cron = cron;
    }

    public String getCron() {
        return cron;
    }

    @Override
    public String toString() {
        return "CronObject{" +
                "cron='" + cron + '\'' +
                '}';
    }
}
