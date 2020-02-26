package fr.zeamateis.usefulsaves;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.zeamateis.usefulsaves.server.commands.CronHelpCommand;
import fr.zeamateis.usefulsaves.server.commands.UsefulSavesCommand;
import fr.zeamateis.usefulsaves.server.commands.argument.CronArgumentType;
import fr.zeamateis.usefulsaves.server.commands.argument.FileArgumentType;
import fr.zeamateis.usefulsaves.server.commands.argument.TimeZoneArgumentType;
import fr.zeamateis.usefulsaves.server.commands.argument.YesNoArgumentsType;
import fr.zeamateis.usefulsaves.server.config.UsefulSavesConfig;
import fr.zeamateis.usefulsaves.server.job.SchedulerManager;
import fr.zeamateis.usefulsaves.server.json.ScheduleObject;
import net.minecraft.command.arguments.ArgumentSerializer;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
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
@Mod(UsefulSaves.MODID)
public class UsefulSaves {

    public static final String MODID = "usefulsaves";

    private static final Logger LOGGER = LogManager.getLogger();
    private static UsefulSaves instance;

    public final AtomicReference<ScheduleObject> taskObject = new AtomicReference<>();

    private final File backupFolder;
    private final SchedulerManager schedulerManager;
    private Gson gson = new GsonBuilder().create();

    public UsefulSaves() {
        instance = this;
        this.schedulerManager = new SchedulerManager();

        //Configs
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, UsefulSavesConfig.COMMON_SPECS);

        //Create backup folder if doesn't exist
        this.backupFolder = new File(UsefulSavesConfig.Common.backupsFolder.get());
        if (!Files.exists(backupFolder.toPath())) {
            try {
                Files.createDirectories(backupFolder.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onSetup);
        MinecraftForge.EVENT_BUS.register(new ServerEvents());
    }

    public static UsefulSaves getInstance() {
        return instance;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     * register fresh new ArgumentTypes
     *
     * @param event
     */
    private void onSetup(FMLCommonSetupEvent event) {
        ArgumentTypes.register("cron", CronArgumentType.class, new ArgumentSerializer<>(CronArgumentType::cron));
        ArgumentTypes.register("file", FileArgumentType.class, new ArgumentSerializer<>(FileArgumentType::file));
        ArgumentTypes.register("yes_no", YesNoArgumentsType.class, new ArgumentSerializer<>(YesNoArgumentsType::yesNo));
        ArgumentTypes.register("timezone", TimeZoneArgumentType.class, new ArgumentSerializer<>(TimeZoneArgumentType::timeZone));
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

    public class ServerEvents {

        @SubscribeEvent
        public void onServerStarting(FMLServerAboutToStartEvent event) {

        }

        @SubscribeEvent
        public void onServerStarting(FMLServerStartingEvent event) {
            //Register Commands
            UsefulSavesCommand.register(event.getCommandDispatcher(), schedulerManager);
            CronHelpCommand.register(event.getCommandDispatcher());
        }

        @SubscribeEvent
        public void onServerStartedEvent(FMLServerStartedEvent event) {
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
                if (!UsefulSavesConfig.Common.cronTaskObject.get().isEmpty())
                    if (UsefulSavesConfig.Common.enableTaskOnServerStart.get())
                        try {
                            getLogger().info("Loading startup tasks from file...");
                            taskObject.set(instance.getGson().fromJson(UsefulSavesConfig.Common.cronTaskObject.get(), ScheduleObject.class));

                            if (taskObject.get() != null)
                                //Check if cron object is not null
                                if (taskObject.get().getCronObject() != null) {
                                    //loading from file
                                    schedulerManager.scheduleCronSave(event.getServer(), event.getServer().getCommandSource(), taskObject.get().getCronObject().getCron(), TimeZone.getTimeZone(taskObject.get().getTimeZone()), taskObject.get().isFlush());
                                } else getLogger().info("cronObject null or empty, ignoring starting task...");
                            else getLogger().info("Json Task empty, ignoring starting task...");
                        } finally {
                            getLogger().info("Loaded tasks from file finished.");
                        }
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }

        @SubscribeEvent
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
}
