package fr.zeamateis.usefulsaves;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.zeamateis.usefulsaves.server.commands.UsefulSavesCommand;
import fr.zeamateis.usefulsaves.server.commands.argument.CalendarArgumentType;
import fr.zeamateis.usefulsaves.server.commands.argument.FileArgumentType;
import fr.zeamateis.usefulsaves.server.commands.argument.TimeUnitArgumentType;
import fr.zeamateis.usefulsaves.server.commands.argument.YesNoArgumentsType;
import fr.zeamateis.usefulsaves.server.config.UsefulSavesConfig;
import fr.zeamateis.usefulsaves.server.task.SaveTask;
import fr.zeamateis.usefulsaves.server.task.TaskObject;
import net.minecraft.command.arguments.ArgumentSerializer;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.util.ResourceLocation;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author ZeAmateis
 */
@Mod(UsefulSaves.MODID)
public class UsefulSaves {

    public static final String MODID = "usefulsaves";
    private static final Logger LOGGER = LogManager.getLogger();
    private static UsefulSaves instance;
    public final AtomicReference<TaskObject> taskObject = new AtomicReference<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final File startingSaveTaskFile = new File("./usefulsaves-start-task.json");
    private final File backupFolder;
    private Gson gson = new GsonBuilder().create();

    public UsefulSaves() {
        instance = this;

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, UsefulSavesConfig.COMMON_SPECS);

        //Create backup folder if doesn't exist
        this.backupFolder = new File(".", UsefulSavesConfig.Common.backupsFolderName.get());
        if (!Files.exists(backupFolder.toPath())) {
            try {
                Files.createDirectories(backupFolder.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onSetup);
        MinecraftForge.EVENT_BUS.register(new ServerEvents());

        //If server crash stop properly the executer service
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            UsefulSavesCommand.stopScheduledTask();
            executor.shutdown();
        }));

    }

    public static UsefulSaves getInstance() {
        return instance;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     * Register the fresh new {@link com.mojang.brigadier.arguments.ArgumentType} to Minecraft
     */
    public void onSetup(FMLCommonSetupEvent event) {
        ArgumentTypes.register(new ResourceLocation(MODID, "time_unit").toString(), TimeUnitArgumentType.class, new ArgumentSerializer<>(TimeUnitArgumentType::timeUnit));
        ArgumentTypes.register(new ResourceLocation(MODID, "file").toString(), FileArgumentType.class, new ArgumentSerializer<>(FileArgumentType::file));
        ArgumentTypes.register(new ResourceLocation(MODID, "yes_no").toString(), YesNoArgumentsType.class, new ArgumentSerializer<>(YesNoArgumentsType::yesNo));
        ArgumentTypes.register(new ResourceLocation(MODID, "calendar").toString(), CalendarArgumentType.class, new ArgumentSerializer<>(CalendarArgumentType::calendar));
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }


    /**
     * Schedule {@link SaveTask} at given period, with gived time unit
     *
     * @param saveTask The task to be executed
     * @param period   The delay period between tasks
     * @param timeUnit The time unit of the delay period
     */
    public int scheduleSave(SaveTask saveTask, long period, TimeUnit timeUnit) {
        if (saveTask != null) {
            UsefulSavesCommand.setScheduledSaveFuture((ScheduledFuture<SaveTask>) getExecutor().scheduleAtFixedRate(saveTask, 0, period, timeUnit));
        }
        return 1;
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
            //Register Command
            UsefulSavesCommand.register(event.getCommandDispatcher());
        }

        @SubscribeEvent
        public void onServerStartedEvent(FMLServerStartedEvent event) {
            if (startingSaveTaskFile.exists())
                if (UsefulSavesConfig.Common.enableTaskOnServerStart.get())
                    try {
                        taskObject.set(instance.getGson().fromJson(new FileReader(startingSaveTaskFile), TaskObject.class));
                        if (taskObject.get() != null)
                            scheduleSave(new SaveTask(event.getServer(), taskObject.get().isFlush()), taskObject.get().getPeriod(), taskObject.get().getTimeUnit());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

        }

        @SubscribeEvent
        public void onServerClose(FMLServerStoppingEvent event) {
            if (UsefulSavesCommand.getScheduledSaveFuture() != null) {
                UsefulSavesCommand.getScheduledSaveFuture().cancel(true);
            }
            executor.shutdown();
        }
    }
}
