package fr.zeamateis.usefulsaves.server.commands;

import com.google.common.collect.Lists;
import fr.zeamateis.usefulsaves.UsefulSaves;
import fr.zeamateis.usefulsaves.server.job.SaveJob;
import fr.zeamateis.usefulsaves.server.job.SchedulerManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import org.apache.commons.io.FileUtils;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import scala.actors.threadpool.Arrays;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main command handler for {@link UsefulSaves}
 *
 * @author ZeAmateis
 */
public class UsefulSavesCommand extends CommandBase {

    private static File fileToDelete;
    private final SchedulerManager manager;

    public UsefulSavesCommand(SchedulerManager manager) {
        this.manager = manager;
    }

    private static int pauseTask(ICommandSender sender, SchedulerManager manager) {
        if (manager.getScheduler() != null) {
            try {
                if (!manager.isPaused()) {
                    manager.getScheduler().pauseAll();
                    manager.setPaused(true);
                    manager.setStatus(SchedulerManager.SchedulerStatus.PAUSED);
                    sender.sendMessage(new TextComponentTranslation("usefulsaves.message.scheduled.pause"));
                }
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
        return 1;
    }

    private static int resumeTask(ICommandSender sender, SchedulerManager manager) {
        if (manager.getScheduler() != null) {
            try {
                if (manager.isPaused()) {
                    manager.getScheduler().resumeAll();
                    manager.setPaused(false);
                    manager.setStatus(SchedulerManager.SchedulerStatus.RUNNING);
                    sender.sendMessage(new TextComponentTranslation("usefulsaves.message.scheduled.resume"));
                }
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
        return 1;
    }

    /**
     * Process simple unscheduled save
     */
    private static int processSave(MinecraftServer server, ICommandSender sender, boolean flush, SchedulerManager manager) {
        SaveJob saveJob = new SaveJob();
        //Check emptyness and process save
        if (!manager.filesToSave(server).isEmpty()) {
            saveJob.setup(server, sender, flush, false, manager.filesToSave(server));
            if (!manager.getSchedulerStatus().equals(SchedulerManager.SchedulerStatus.RUNNING))
                manager.setStatus(SchedulerManager.SchedulerStatus.RUNNING_NO_TASKS);
            saveJob.processSave();
        }
        return 1;
    }

    /**
     * Clear backup folder, remove all backups files
     */
    private static int clearBackupFiles(ICommandSender sender) {
        try {
            FileUtils.cleanDirectory(UsefulSaves.getInstance().getBackupFolder());
            sender.sendMessage(new TextComponentTranslation("usefulsaves.message.clearBackupFolder"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * Stop the {@link SaveJob} task
     */
    public static int unscheduleTask(ICommandSender sender, SchedulerManager manager) {
        if (manager.unscheduleSaveJob()) {
            sender.sendMessage(new TextComponentTranslation("usefulsaves.message.scheduled.stop"));
        } else {
            sender.sendMessage(new TextComponentTranslation("usefulsaves.message.scheduled.stop.notRunning"));
        }
        return 1;
    }

    /**
     * Count how many files are in backup folder
     */
    private static void listBackup(ICommandSender sender) {
        try (Stream<Path> walk = Files.walk(Paths.get(UsefulSaves.getInstance().getBackupFolder().getPath()))) {
            List<String> backupList = walk
                    .filter(Files::isRegularFile)
                    .map(x -> x.getFileName().toString())
                    .collect(Collectors.toList());
            if (!backupList.isEmpty())
                sender.sendMessage(new TextComponentTranslation("usefulsaves.message.backupCount", backupList.size()));
            else
                sender.sendMessage(new TextComponentTranslation("usefulsaves.message.backupCount.empty", backupList.size()));
        } catch (IOException ignored) {
        }
    }

    public static void printInfo(ICommandSender sender, SchedulerManager manager) {
        if (manager.getScheduler() != null) {
            List<TriggerKey> triggerKeys = manager.getTriggers().stream().filter(Objects::nonNull).map(Trigger::getKey).collect(Collectors.toList());
            if (!triggerKeys.isEmpty()) {
                try {
                    sender.sendMessage(new TextComponentTranslation("usefulsaves.message.scheduled.runningSince", manager.getScheduler().getMetaData().getRunningSince(), manager.getSchedulerStatus()));
                } catch (SchedulerException e) {
                }
            }
            sender.sendMessage(new TextComponentTranslation("usefulsaves.message.scheduled.status", manager.getSchedulerStatus()));
        }
    }

    /**
     * Gets the name of the command
     */
    @Override
    public String getName() {
        return "useful-saves";
    }

    /*public static void register(SchedulerManager manager) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("useful-saves")
                //Is player op ?
                .requires(requierements -> requierements.hasPermissionLevel(4))
                //Clear backup folder
                .then(Commands.literal("clear-backups-folder").executes(UsefulSavesCommand::clearBackupFiles))
                //Delete a specific backup file
                .then(Commands.literal("delete")
                        .then(Commands.argument("file", FileArgumentType.file())
                                .executes(context -> {
                                    fileToDelete = FileArgumentType.getFile(context, "file");
                                    context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.delete.confirm_one", fileToDelete.getName()), false);
                                    context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.delete.confirm_two"), false);
                                    return 1;
                                })
                        )
                        //Confirm delete action
                        .then(Commands.literal("confirm")
                                .then(Commands.argument("yesNo", YesNoArgumentsType.yesNo())
                                        .executes(context -> {
                                            if (YesNoArgumentsType.getResponse(context, "yesNo"))
                                                try {
                                                    FileUtils.forceDelete(fileToDelete);
                                                    context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.confirm.delete", fileToDelete.getName()), false);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            return 1;
                                        })
                                )
                        )
                )
                //Scheduled tasks
                .then(Commands.literal("schedule")
                        //Pause scheduled task
                        .then(Commands.literal("pause").executes(context -> pauseTask(context, manager)))
                        //Resume scheduled tasks
                        .then(Commands.literal("resume").executes(context -> resumeTask(context, manager)))
                        //Stop scheduled task
                        .then(Commands.literal("stop").executes(context -> unscheduleTask(context, manager)))
                        //Cron task manager
                        .then(Commands.literal("cron")
                                .then(Commands.argument("cron", CronArgumentType.cron())
                                        //Custom TimeZone
                                        .then(Commands.argument("timeZone", TimeZoneArgumentType.timeZone())
                                                //With flush parameter
                                                .then(Commands.argument("flush", BoolArgumentType.bool())
                                                        .executes(context -> manager.scheduleCronSave(true,
                                                                context.getSource().getServer(),
                                                                context.getSource(),
                                                                CronArgumentType.getCron(context, "cron").getCronExpression(),
                                                                TimeZoneArgumentType.getTimeZone(context, "timeZone"),
                                                                UsefulSaves.getInstance().taskObject.get().isFlush()
                                                        )))
                                                //No flush parameter
                                                .executes(context -> manager.scheduleCronSave(true,
                                                        context.getSource().getServer(),
                                                        context.getSource(),
                                                        CronArgumentType.getCron(context, "cron").getCronExpression(),
                                                        TimeZoneArgumentType.getTimeZone(context, "timeZone"),
                                                        false
                                                ))
                                        )
                                        //Default TimeZone
                                        //With flush parameter
                                        .then(Commands.argument("flush", BoolArgumentType.bool())
                                                .executes(context -> manager.scheduleCronSave(true,
                                                        context.getSource().getServer(),
                                                        context.getSource(),
                                                        CronArgumentType.getCron(context, "cron").getCronExpression(),
                                                        TimeZone.getTimeZone(UsefulSavesConfig.Common.timeZone.get()),
                                                        UsefulSaves.getInstance().taskObject.get().isFlush()
                                                )))
                                        //No flush parameter
                                        .executes(context -> manager.scheduleCronSave(true,
                                                context.getSource().getServer(),
                                                context.getSource(),
                                                CronArgumentType.getCron(context, "cron").getCronExpression(),
                                                TimeZone.getTimeZone(UsefulSavesConfig.Common.timeZone.get()),
                                                false
                                        ))

                                )
                        )
                        // Restart sheduler in case of any crash or shutdowns
                        .then(Commands.literal("restart")
                                .executes(context -> {
                                    if (!manager.isPaused() && manager.getScheduler() == null)
                                        manager.tryCreateScheduler();
                                    return 1;
                                })
                        )
                )
                //Simple unscheduled save
                .then(Commands.literal("save-now")
                        //With flush parameter
                        .then(Commands.argument("flush", BoolArgumentType.bool())
                                .executes(context -> processSave(context.getSource().getServer(), context.getSource(), BoolArgumentType.getBool(context, "flush"), manager)))
                        //No flush parameter
                        .executes(context -> processSave(context.getSource().getServer(), context.getSource(), false, manager))
                )
                //Display informations
                .then(Commands.literal("info")
                        //Running task info
                        .executes(context -> {
                            listBackup(context);
                            printInfo(context, manager);
                            return 1;
                        })
                )
                // TODO
                // Time left before next save

                //Config parameters
                .then(Commands.literal("config")
                        //Print message in tchat
                        .then(Commands.literal("printChatMessage")
                                .then(Commands.argument("printChatMessage", BoolArgumentType.bool())
                                        .executes(context -> {
                                            UsefulSavesConfig.Common.printMessage.set(BoolArgumentType.getBool(context, "printChatMessage"));
                                            UsefulSavesConfig.Common.printMessage.save();
                                            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.config.printChatMessage", BoolArgumentType.getBool(context, "printChatMessage")), false);
                                            return 1;
                                        })
                                )
                        )
                        //Start task on starting server ?
                        .then(Commands.literal("enableTaskOnServerStart")
                                .then(Commands.argument("enableTaskOnServerStart", BoolArgumentType.bool())
                                        .executes(context -> {
                                            UsefulSavesConfig.Common.enableTaskOnServerStart.set(BoolArgumentType.getBool(context, "enableTaskOnServerStart"));
                                            UsefulSavesConfig.Common.enableTaskOnServerStart.save();
                                            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.config.enableTaskOnServerStart", BoolArgumentType.getBool(context, "enableTaskOnServerStart")), false);

                                            return 1;
                                        })
                                )
                        )
                        //TODO enable compression
                        //Define zip compression level
                        /*.then(Commands.literal("compression")
                                .then(Commands.argument("compresionLevel", IntegerArgumentType.integer(-1, 9))
                                        .executes(context -> {
                                                    UsefulSavesConfig.Common.backupCompression.set(IntegerArgumentType.getInteger(context, "compresionLevel"));
                                                    UsefulSavesConfig.Common.backupCompression.save();
                                                    context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.config.compresionLevel", IntegerArgumentType.getInteger(context, "compresionLevel")), false);
                                                    return 1;
                                                }
                                        )
                                )
                        )*/
                        //Define the backup folder
                        /*.then(Commands.literal("backupFolder")
                                .then(Commands.argument("folder", StringArgumentType.string())
                                        .executes(context -> {
                                            UsefulSavesConfig.Common.backupsFolder.set(StringArgumentType.getString(context, "folder"));
                                            UsefulSavesConfig.Common.backupsFolder.save();
                                            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.config.backupFolder", StringArgumentType.getString(context, "folder")), false);
                                            return 1;
                                        })
                                )
                        )
                        //Define maximum saves in backup folder
                        .then(Commands.literal("maximum-backups")
                                .then(Commands.argument("max", IntegerArgumentType.integer(-1, Integer.MAX_VALUE))
                                        .executes(context -> {
                                            UsefulSavesConfig.Common.maximumSavedBackups.set(IntegerArgumentType.getInteger(context, "max"));
                                            UsefulSavesConfig.Common.maximumSavedBackups.save();
                                            context.getSource().sendFeedback(
                                                    new TranslationTextComponent("usefulsaves.message.config.maximumSavedBackups",
                                                            IntegerArgumentType.getInteger(context, "max") == -1 ?
                                                                    new TranslationTextComponent("usefulsaves.message.config.maximumSavedBackups.unlimited") :
                                                                    IntegerArgumentType.getInteger(context, "max")), false);
                                            return 1;
                                        })
                                )
                        )
                        //Define TimeZone
                        .then(Commands.literal("timeZone")
                                .then(Commands.argument("id", TimeZoneArgumentType.timeZone())
                                        .executes(context -> {
                                            UsefulSavesConfig.Common.timeZone.set(TimeZoneArgumentType.getTimeZone(context, "id").getID());
                                            UsefulSavesConfig.Common.timeZone.save();
                                            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.config.timeZone", TimeZoneArgumentType.getTimeZone(context, "id").getID()), false);
                                            return 1;
                                        })
                                )
                        )
                        //Save or not if server is empty
                        .then(Commands.literal("saveIfServerEmpty")
                                .then(Commands.argument("save", BoolArgumentType.bool())
                                        .executes(context -> {
                                            UsefulSavesConfig.Common.saveIfServerEmpty.set(BoolArgumentType.getBool(context, "save"));
                                            UsefulSavesConfig.Common.saveIfServerEmpty.save();
                                            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.config.saveIfServerEmpty", BoolArgumentType.getBool(context, "save")), false);
                                            return 1;
                                        })
                                )
                        )
                        //Define if old backups are deleted if maximum files reach
                        .then(Commands.literal("deleteOldOnMaximumReach")
                                .then(Commands.argument("delete", BoolArgumentType.bool())
                                        .executes(context -> {
                                            UsefulSavesConfig.Common.deleteOldOnMaximumReach.set(BoolArgumentType.getBool(context, "delete"));
                                            UsefulSavesConfig.Common.deleteOldOnMaximumReach.save();
                                            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.config.deleteOldOnMaximumReach", BoolArgumentType.getBool(context, "delete")), false);
                                            return 1;
                                        })
                                )
                        )


                )
        );
    }*/

    /**
     * Gets the usage string for the command.
     *
     * @param sender
     */
    @Override
    public String getUsage(ICommandSender sender) {
        return "";
    }

    /**
     * Get a list of aliases for this command. <b>Never return null!</b>
     */
    @Override
    public List<String> getAliases() {
        return Arrays.asList(new String[]{"useful-saves"});
    }

    /**
     * Callback for when the command is executed
     *
     * @param server
     * @param sender
     * @param args
     */
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

    }

    /**
     * Return the required permission level for this command.
     */
    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }

    /**
     * Check if the given ICommandSender has permission to execute this command
     *
     * @param server
     * @param sender
     */
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return super.checkPermission(server, sender);
    }

    /**
     * Get a list of options for when the user presses the TAB key
     *
     * @param server
     * @param sender
     * @param args
     * @param targetPos
     */
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length > 0) {
            switch (args.length) {
                case 1:
                    return Lists.newArrayList("clear-backups-folder", "delete", "confirm", "info", "config", "save-now", "schedule");
                case 2: {
                    switch (args[0]) {
                        case "clear-backups-folder":
                            return Lists.newArrayList();
                        case "delete": {
                            try (Stream<Path> walk = Files.walk(Paths.get(UsefulSaves.getInstance().getBackupFolder().getPath()))) {
                                List<String> result = walk.filter(Files::isRegularFile)
                                        .map(x -> x.getFileName().toString()).collect(Collectors.toList());
                                return Lists.newArrayList(result);
                            } catch (IOException ex) {
                                return Lists.newArrayList();
                            }
                        }
                        case "confirm": {
                            return Lists.newArrayList("yes", "no");
                        }
                        case "save-now":
                            return Lists.newArrayList("true", "false");
                        case "config": {
                            return Lists.newArrayList("printChatMessage", "enableTaskOnServerStart", "backupFolder",
                                    "maximum-backups", "timeZone", "saveIfServerEmpty", "deleteOldOnMaximumReach");
                        }
                        case "schedule": {
                            return Lists.newArrayList("cron", "stop", "pause", "resume", "restart");
                        }
                    }
                }
                case 3: {
                    switch (args[1]) {
                        case "printChatMessage":
                        case "deleteOldOnMaximumReach":
                        case "saveIfServerEmpty":
                        case "enableTaskOnServerStart":
                            return Lists.newArrayList("true", "false");
                        case "maximum-backups":
                            return Lists.newArrayList("-1", String.valueOf(Integer.MAX_VALUE));
                        case "timeZone":
                            return getListOfStringsMatchingLastWord(args, TimeZone.getAvailableIDs());
                    }
                }
            }
        }
        return Lists.newArrayList();
    }

    /**
     * Return whether the specified command parameter index is a username parameter.
     *
     * @param args
     * @param index
     */
    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

}
