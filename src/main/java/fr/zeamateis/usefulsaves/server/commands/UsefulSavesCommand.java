package fr.zeamateis.usefulsaves.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.zeamateis.usefulsaves.UsefulSaves;
import fr.zeamateis.usefulsaves.server.commands.argument.CronArgumentType;
import fr.zeamateis.usefulsaves.server.commands.argument.FileArgumentType;
import fr.zeamateis.usefulsaves.server.commands.argument.TimeZoneArgumentType;
import fr.zeamateis.usefulsaves.server.commands.argument.YesNoArgumentsType;
import fr.zeamateis.usefulsaves.server.config.UsefulSavesConfig;
import fr.zeamateis.usefulsaves.server.job.SaveJob;
import fr.zeamateis.usefulsaves.server.job.SchedulerManager;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.io.FileUtils;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

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
public class UsefulSavesCommand {

    private static File fileToDelete;

    public static void register(CommandDispatcher<CommandSource> dispatcher, SchedulerManager manager) {
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
                                                        .executes(context -> manager.scheduleCronSave(
                                                                context.getSource().getServer(),
                                                                context.getSource(),
                                                                CronArgumentType.getCron(context, "cron").getCronExpression(),
                                                                TimeZoneArgumentType.getTimeZone(context, "timeZone"),
                                                                UsefulSaves.getInstance().taskObject.get().isFlush()
                                                        )))
                                                //No flush parameter
                                                .executes(context -> manager.scheduleCronSave(
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
                                                .executes(context -> manager.scheduleCronSave(
                                                        context.getSource().getServer(),
                                                        context.getSource(),
                                                        CronArgumentType.getCron(context, "cron").getCronExpression(),
                                                        TimeZone.getTimeZone(UsefulSavesConfig.Common.timeZone.get()),
                                                        UsefulSaves.getInstance().taskObject.get().isFlush()
                                                )))
                                        //No flush parameter
                                        .executes(context -> manager.scheduleCronSave(
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
                        //List backuped files
                        .executes(UsefulSavesCommand::listBackup)
                        //Running task info
                        .executes(context -> printInfo(context, manager))
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
                        .then(Commands.literal("backupFolder")
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
    }

    private static int pauseTask(CommandContext<CommandSource> context, SchedulerManager manager) {
        if (manager.getScheduler() != null) {
            try {
                if (!manager.isPaused()) {
                    manager.getScheduler().pauseAll();
                    manager.setPaused(true);
                    manager.setStatus(SchedulerManager.SchedulerStatus.PAUSED);
                    context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.scheduled.pause"), false);
                }
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
        return 1;
    }

    private static int resumeTask(CommandContext<CommandSource> context, SchedulerManager manager) {
        if (manager.getScheduler() != null) {
            try {
                if (manager.isPaused()) {
                    manager.getScheduler().resumeAll();
                    manager.setPaused(false);
                    manager.setStatus(SchedulerManager.SchedulerStatus.RUNNING);
                    context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.scheduled.resume"), false);
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
    private static int processSave(MinecraftServer server, CommandSource commandSource, boolean flush, SchedulerManager manager) {
        SaveJob saveJob = new SaveJob();
        List<Path> paths = UsefulSavesConfig.Common.savedFileWhitelist.get().stream().map(Paths::get).collect(Collectors.toList());
        //Add default world to whitelist
        server.getWorlds().forEach(serverWorld -> {
            if (paths.stream().noneMatch(o -> o.equals(serverWorld.getSaveHandler().getWorldDirectory().toPath())))
                paths.add(serverWorld.getSaveHandler().getWorldDirectory().toPath());
            if (UsefulSavesConfig.Common.savedFileWhitelist.get().stream().noneMatch(o -> o.equals(serverWorld.getSaveHandler().getWorldDirectory().toPath().toString()))) {
                UsefulSavesConfig.Common.savedFileWhitelist.get().add(serverWorld.getSaveHandler().getWorldDirectory().toPath().toString());
                UsefulSavesConfig.Common.savedFileWhitelist.save();
            }
        });
        //Check emptyness and process save
        if (!paths.isEmpty()) {
            saveJob.setup(server, commandSource, flush, false, paths);
            if (!manager.getSchedulerStatus().equals(SchedulerManager.SchedulerStatus.RUNNING))
                manager.setStatus(SchedulerManager.SchedulerStatus.RUNNING_NO_TASKS);
            saveJob.processSave();
        }
        return 1;
    }

    /**
     * Count how many files are in backup folder
     */
    private static int listBackup(CommandContext<CommandSource> context) {
        try (Stream<Path> walk = Files.walk(Paths.get(UsefulSaves.getInstance().getBackupFolder().getPath()))) {
            List<String> backupList = walk
                    .filter(Files::isRegularFile)
                    .map(x -> x.getFileName().toString())
                    .collect(Collectors.toList());
            if (!backupList.isEmpty())
                context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.backupCount", backupList.size()), false);
            else
                context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.backupCount.empty", backupList.size()), false);
        } catch (IOException ignored) {
        }
        return 1;
    }

    /**
     * Clear backup folder, remove all backups files
     */
    private static int clearBackupFiles(CommandContext<CommandSource> context) {
        try {
            FileUtils.cleanDirectory(UsefulSaves.getInstance().getBackupFolder());
            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.clearBackupFolder"), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * Stop the {@link SaveJob} task
     */
    public static int unscheduleTask(CommandContext<CommandSource> context, SchedulerManager manager) {
        if (manager.unscheduleSaveJob()) {
            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.scheduled.stop"), false);
        } else {
            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.scheduled.stop.notRunning"), false);
        }
        return 1;
    }

    public static int printInfo(CommandContext<CommandSource> context, SchedulerManager manager) {
        if (manager.getScheduler() != null) {
            List<TriggerKey> triggerKeys = manager.getTriggers().stream().filter(Objects::nonNull).map(Trigger::getKey).collect(Collectors.toList());
            if (!triggerKeys.isEmpty()) {
                try {
                    context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.scheduled.runningSince", manager.getScheduler().getMetaData().getRunningSince(), manager.getSchedulerStatus()), false);
                    return 1;
                } catch (SchedulerException e) {
                    return 0;
                }
            }
            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.scheduled.status", manager.getSchedulerStatus()), false);
        }
        return 1;
    }

}
