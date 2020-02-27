package fr.zeamateis.usefulsaves;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.zeamateis.usefulsaves.server.commands.CronHelpCommand;
import fr.zeamateis.usefulsaves.server.commands.UsefulSavesCommand;
import fr.zeamateis.usefulsaves.server.config.UsefulSavesConfig;
import fr.zeamateis.usefulsaves.server.job.SchedulerManager;
import fr.zeamateis.usefulsaves.server.json.ScheduleObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author ZeAmateis
 */
@Mod(modid = UsefulSaves.MODID)
public class UsefulSaves {

    public static final String MODID = "usefulsaves";

    private static Logger logger;

    @Mod.Instance
    private static UsefulSaves instance;

    public final AtomicReference<ScheduleObject> taskObject = new AtomicReference<>();
    private final Gson gson = new GsonBuilder().create();
    private File backupFolder;
    private SchedulerManager schedulerManager;

    public static UsefulSaves getInstance() {
        return instance;
    }

    public static Logger getLogger() {
        return logger;
    }


    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        //Create backup folder if doesn't exist
        this.backupFolder = new File(UsefulSavesConfig.backupsFolder);
        if (!Files.exists(backupFolder.toPath())) {
            try {
                Files.createDirectories(backupFolder.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        logger = event.getModLog();
        this.schedulerManager = new SchedulerManager();
    }

    /**
     * register fresh new ArgumentTypes
     *
     * @param event
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
       /* ArgumentTypes.register("cron", CronArgumentType.class, new ArgumentSerializer<>(CronArgumentType::cron));
        ArgumentTypes.register("file", FileArgumentType.class, new ArgumentSerializer<>(FileArgumentType::file));
        ArgumentTypes.register("yes_no", YesNoArgumentsType.class, new ArgumentSerializer<>(YesNoArgumentsType::yesNo));
        ArgumentTypes.register("timezone", TimeZoneArgumentType.class, new ArgumentSerializer<>(TimeZoneArgumentType::timeZone));*/
    }

    public SchedulerManager getSchedulerManager() {
        return schedulerManager;
    }

    public File getBackupFolder() {
        return backupFolder;
    }

    public Gson getGson() {
        return gson;
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerAboutToStartEvent event) {

    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        //Register Commands
        event.registerServerCommand(new UsefulSavesCommand(schedulerManager));
        event.registerServerCommand(new CronHelpCommand());

        try {
            //Create and start scheduler if is null
            if (schedulerManager.getScheduler() == null) {
                schedulerManager.tryCreateScheduler();
            }
            // Restart scheduler if is only shutdown
            else if (schedulerManager.getScheduler() != null) {
                if (schedulerManager.getScheduler().isShutdown())
                    schedulerManager.tryCreateScheduler();
            }

            //Auto-reload task from file
            if (schedulerManager.getScheduler() != null)
                if (!UsefulSavesConfig.cronTaskObject.isEmpty())
                    if (UsefulSavesConfig.enableTaskOnServerStart)
                        try {
                            getLogger().info("Loading startup tasks from file...");
                            taskObject.set(instance.getGson().fromJson(UsefulSavesConfig.cronTaskObject, ScheduleObject.class));

                            if (taskObject.get() != null)
                                //Check if cron object is not null
                                if (taskObject.get().getCronObject() != null) {
                                    //loading from file
                                    schedulerManager.scheduleCronSave(false, event.getServer(), null, taskObject.get().getCronObject().getCron(), TimeZone.getTimeZone(taskObject.get().getTimeZone()), taskObject.get().isFlush());
                                } else getLogger().info("cronObject null or empty, ignoring starting task...");
                            else getLogger().info("Json Task empty, ignoring starting task...");
                        } finally {
                            getLogger().info("Loaded tasks from file finished.");
                        }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }


    @Mod.EventHandler
    public void onServerClose(FMLServerStoppingEvent event) {
        //Shutdown scheduler on stopping server
        if (schedulerManager.getScheduler() != null) {
            try {
                schedulerManager.getScheduler().shutdown();
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
    }
}
