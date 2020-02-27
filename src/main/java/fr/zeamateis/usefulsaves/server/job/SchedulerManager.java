package fr.zeamateis.usefulsaves.server.job;

import fr.zeamateis.usefulsaves.UsefulSaves;
import fr.zeamateis.usefulsaves.server.config.UsefulSavesConfig;
import fr.zeamateis.usefulsaves.server.json.CronObject;
import fr.zeamateis.usefulsaves.server.json.ScheduleObject;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link SchedulerFactory} & {@link Scheduler} Manager
 *
 * @author ZeAmateis
 */
public class SchedulerManager {

    private final SchedulerFactory factory = new StdSchedulerFactory();
    private final JobDetail saveJob = JobBuilder.newJob(SaveJob.class).withIdentity("SaveTask", "default").build();
    private Scheduler scheduler;

    private CronTrigger cronTrigger;
    private Set<Trigger> triggers = new HashSet<>();
    //Execute save just after typped scheduled task
    private Trigger nowTrigger;

    private SchedulerStatus schedulerStatus = SchedulerStatus.UNKNOWN;
    private boolean pause;

    public SchedulerManager() {
        //If server crash or stopped, shutdown properly the executer service
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.updateTaskInConfig();
            this.unscheduleSaveJob();
            try {
                scheduler.shutdown();
                setStatus(SchedulerStatus.SHUTDOWN);
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }));

    }

    private void updateTaskInConfig() {
        if (UsefulSavesConfig.Common.enableTaskOnServerStart.get()) {
            UsefulSavesConfig.Common.cronTaskObject.set(UsefulSaves.getInstance().getGson().toJson(UsefulSaves.getInstance().taskObject.get()));
            UsefulSavesConfig.Common.cronTaskObject.save();
        }
    }

    public void tryCreateScheduler() {
        try {
            scheduler = factory.getScheduler();
            UsefulSaves.getLogger().info("Created Scheduler");
            scheduler.start();
            UsefulSaves.getLogger().info("Scheduler Started");
            setStatus(SchedulerStatus.RUNNING_NO_TASKS);
        } catch (SchedulerException ex) {
            setStatus(SchedulerStatus.UNKNOWN);
            ex.printStackTrace();
        }
    }

    /**
     * Remove Job from scheduler
     */
    public boolean unscheduleSaveJob() {
        if (this.scheduler != null) {
            List<TriggerKey> triggerKeys = this.triggers.stream().filter(Objects::nonNull).map(Trigger::getKey).collect(Collectors.toList());
            if (!triggerKeys.isEmpty()) {
                try {
                    this.scheduler.unscheduleJobs(triggerKeys);
                    this.setStatus(SchedulerManager.SchedulerStatus.NOT_RUNNING);
                    return true;
                } catch (SchedulerException e) {
                    return false;
                }
            }
        }
        return false;
    }

    public int scheduleCronSave(boolean startNow, MinecraftServer server, CommandSource commandSource, String cronTask, TimeZone timeZone, boolean flush) {
        try {
            UsefulSaves.getInstance().taskObject.set(new ScheduleObject(new CronObject(cronTask), timeZone, flush));
            updateTaskInConfig();

            if (!UsefulSavesConfig.Common.timeZone.get().equals(timeZone.getID())) {
                UsefulSavesConfig.Common.timeZone.set(timeZone.getID());
                UsefulSavesConfig.Common.timeZone.save();
            }

            cronTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("SaveCronTask", "default")
                    .withSchedule(
                            CronScheduleBuilder
                                    .cronSchedule(cronTask)
                                    .inTimeZone(TimeZone.getTimeZone(UsefulSaves.getInstance().taskObject.get().getTimeZone()))
                    )
                    .build();

            nowTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("SaveNowTask", "default")
                    .startNow()
                    .build();

            if (startNow) triggers.add(nowTrigger);
            triggers.add(cronTrigger);

            saveJob.getJobDataMap().put("server", server);
            saveJob.getJobDataMap().put("commandSource", commandSource);
            saveJob.getJobDataMap().put("flush", flush);
            //TODO Define in config
            //But pretty useless to define now for external user
            saveJob.getJobDataMap().put("deleteExisting", false);

            //Check emptyness
            if (!filesToSave(server).isEmpty()) {
                saveJob.getJobDataMap().putIfAbsent("sourceWhitelist", filesToSave(server));
            }

            scheduler.scheduleJob(saveJob, triggers, true);
            setStatus(SchedulerStatus.RUNNING);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * Create a list of paths usable without duplicated entries
     */
    public List<Path> filesToSave(MinecraftServer server) {
        //Read whiteList config, filter non-existing files/folder and process
        List<Path> whitelistPath = UsefulSavesConfig.Common.savedFileWhitelist.get().stream().map(Paths::get).filter(path -> path.toFile().exists()).collect(Collectors.toList());

        server.getWorlds().forEach(serverWorld -> {
            if (whitelistPath.stream().noneMatch(o -> o.equals(serverWorld.getSaveHandler().getWorldDirectory().toPath())))
                whitelistPath.add(serverWorld.getSaveHandler().getWorldDirectory().toPath());
            if (UsefulSavesConfig.Common.savedFileWhitelist.get().stream().noneMatch(o -> o.equals(serverWorld.getSaveHandler().getWorldDirectory().toPath().toString()))) {
                UsefulSavesConfig.Common.savedFileWhitelist.get().add(serverWorld.getSaveHandler().getWorldDirectory().toPath().toString());
                UsefulSavesConfig.Common.savedFileWhitelist.save();
            }
        });

        //Check duplicated entry, collect and process
        return whitelistPath.stream().distinct().filter(path -> path.toFile().exists()).collect(Collectors.toList());
    }

    /**
     * Getter for Scheduler
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    public Set<Trigger> getTriggers() {
        return triggers;
    }

    public boolean isPaused() {
        return pause;
    }

    public void setPaused(boolean pause) {
        this.pause = pause;
    }

    public void setStatus(SchedulerStatus statusIn) {
        this.schedulerStatus = statusIn;
    }

    public SchedulerStatus getSchedulerStatus() {
        return schedulerStatus;
    }

    public enum SchedulerStatus {
        NOT_RUNNING,
        RUNNING_NO_TASKS,
        RUNNING,
        PAUSED,
        MAXIMUM_BACKUP_REACH,
        SHUTDOWN,
        UNKNOWN
    }
}
