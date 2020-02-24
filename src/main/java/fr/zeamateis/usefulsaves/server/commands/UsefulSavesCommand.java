package fr.zeamateis.usefulsaves.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.zeamateis.usefulsaves.UsefulSaves;
import fr.zeamateis.usefulsaves.server.commands.argument.CalendarArgumentType;
import fr.zeamateis.usefulsaves.server.commands.argument.FileArgumentType;
import fr.zeamateis.usefulsaves.server.commands.argument.TimeUnitArgumentType;
import fr.zeamateis.usefulsaves.server.commands.argument.YesNoArgumentsType;
import fr.zeamateis.usefulsaves.server.config.UsefulSavesConfig;
import fr.zeamateis.usefulsaves.server.task.SaveTask;
import fr.zeamateis.usefulsaves.server.task.TaskObject;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ZeAmateis
 */
public class UsefulSavesCommand {
    private static ScheduledFuture<SaveTask> scheduledSaveFuture;
    private static SaveTask saveTask;

    private static File fileToDelete;

    public static void register(CommandDispatcher<CommandSource> dispatcher) {

        dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal("useful-saves")
                        //Is player op ?
                        .requires(requierements -> requierements.hasPermissionLevel(4))
                        //Stop scheduled save
                        .then(Commands.literal("stop").executes(UsefulSavesCommand::stopTask))
                        //Clear backup folder
                        .then(Commands.literal("clear-backups-folder").executes(UsefulSavesCommand::clearBackupFiles))
                        //List backuped files
                        .then(Commands.literal("list").executes(UsefulSavesCommand::listBackup))
                        //Test time format
                        .then(Commands.literal("time")
                                .then(Commands.argument("time", CalendarArgumentType.calendar())
                                        .executes(context -> {
                                            System.out.println(CalendarArgumentType.getCalendar(context, "time").getTime());
                                            return 1;
                                        })
                                )
                        )
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
                        //Scheduled tasks
                        .then(Commands.literal("schedule")
                                //Saving task
                                .then(Commands.literal("save")
                                        /*.then(Commands.argument("time", CalendarArgumentType.calendar())
                                                .then(Commands.argument("flush", BoolArgumentType.bool())
                                                        .executes(context -> {
                                                            UsefulSaves.getInstance().taskObject.set(new TaskObject(IntegerArgumentType.getInteger(context, "delay"), TimeUnitArgumentType.getTimeUnit(context, "timeUnit"), BoolArgumentType.getBool(context, "flush")));
                                                            try (Writer writer = new FileWriter("./usefulsaves-start-task.json")) {
                                                                UsefulSaves.getInstance().getGson().toJson(UsefulSaves.getInstance().taskObject.get(), writer);
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            }
                                                            return UsefulSaves.getInstance().scheduleSave(
                                                                    new SaveTask(context.getSource().getServer(), UsefulSaves.getInstance().taskObject.get().isFlush()),
                                                                    UsefulSaves.getInstance().taskObject.get().getPeriod(),
                                                                    UsefulSaves.getInstance().taskObject.get().getTimeUnit());
                                                        }))
                                                //No flush parameter
                                                .executes(context -> {
                                                    UsefulSaves.getInstance().taskObject.set(new TaskObject(IntegerArgumentType.getInteger(context, "delay"), TimeUnitArgumentType.getTimeUnit(context, "timeUnit"), false));
                                                    try (Writer writer = new FileWriter("./usefulsaves-start-task.json")) {
                                                        UsefulSaves.getInstance().getGson().toJson(UsefulSaves.getInstance().taskObject.get(), writer);
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    return UsefulSaves.getInstance().scheduleSave(
                                                            new SaveTask(context.getSource().getServer(), UsefulSaves.getInstance().taskObject.get().isFlush()),
                                                            UsefulSaves.getInstance().taskObject.get().getPeriod(),
                                                            UsefulSaves.getInstance().taskObject.get().getTimeUnit());
                                                })
                                        )*/
                                        //Simple integer Delay value
                                        .then(Commands.argument("delay", IntegerArgumentType.integer(1))
                                                //TimeUnit value
                                                .then(Commands.argument("timeUnit", TimeUnitArgumentType.timeUnit())
                                                                //.then(Commands.argument("value", StringArgumentType.word())
                                                                //With flush parameter
                                                                .then(Commands.argument("flush", BoolArgumentType.bool())
                                                                        .executes(context -> {
                                                                            UsefulSaves.getInstance().taskObject.set(new TaskObject(IntegerArgumentType.getInteger(context, "delay"), TimeUnitArgumentType.getTimeUnit(context, "timeUnit"), BoolArgumentType.getBool(context, "flush")));
                                                                            try (Writer writer = new FileWriter("./usefulsaves-start-task.json")) {
                                                                                UsefulSaves.getInstance().getGson().toJson(UsefulSaves.getInstance().taskObject.get(), writer);
                                                                            } catch (IOException e) {
                                                                                e.printStackTrace();
                                                                            }
                                                                            return UsefulSaves.getInstance().scheduleSave(
                                                                                    new SaveTask(context.getSource().getServer(), UsefulSaves.getInstance().taskObject.get().isFlush()),
                                                                                    UsefulSaves.getInstance().taskObject.get().getPeriod(),
                                                                                    UsefulSaves.getInstance().taskObject.get().getTimeUnit());
                                                                        }))
                                                                //No flush parameter
                                                                .executes(context -> {
                                                                    UsefulSaves.getInstance().taskObject.set(new TaskObject(IntegerArgumentType.getInteger(context, "delay"), TimeUnitArgumentType.getTimeUnit(context, "timeUnit"), false));
                                                                    try (Writer writer = new FileWriter("./usefulsaves-start-task.json")) {
                                                                        UsefulSaves.getInstance().getGson().toJson(UsefulSaves.getInstance().taskObject.get(), writer);
                                                                    } catch (IOException e) {
                                                                        e.printStackTrace();
                                                                    }
                                                                    return UsefulSaves.getInstance().scheduleSave(
                                                                            new SaveTask(context.getSource().getServer(), UsefulSaves.getInstance().taskObject.get().isFlush()),
                                                                            UsefulSaves.getInstance().taskObject.get().getPeriod(),
                                                                            UsefulSaves.getInstance().taskObject.get().getTimeUnit());
                                                                })
                                                        //)
                                                )
                                        )
                                )
                        )
                        //Simple unscheduled save
                        .then(Commands.literal("save")
                                //With flush parameter
                                .then(Commands.argument("flush", BoolArgumentType.bool())
                                        .executes(context -> processSave(context.getSource().getServer(), BoolArgumentType.getBool(context, "flush"))))
                                //No flush parameter
                                .executes(context -> processSave(context.getSource().getServer(), false))
                        )
                        // TODO
                        // Time left before next save
                        /*.then(Commands.literal("time")
                                .executes(context -> {
                                    DateFormat simple = new SimpleDateFormat("HH:mm:ss");
                                    Date result = new Date(scheduledSaveFuture.getDelay(TimeUnit.MILLISECONDS));
                                    MessageUtils.printMessageForAllPlayers(context.getSource().getServer(), new StringTextComponent(simple.format(result)));
                                    return 1;
                                })
                        )*/
                        .then(Commands.literal("printChatMessage")
                                .then(Commands.argument("printChatMessage", BoolArgumentType.bool())
                                        .executes(context -> {
                                            UsefulSavesConfig.Common.printMessage.set(BoolArgumentType.getBool(context, "printChatMessage"));
                                            UsefulSavesConfig.Common.printMessage.save();
                                            return 1;
                                        })
                                )
                        )
        );
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
     * Process a single save, without schedule
     */
    private static int processSave(MinecraftServer server, boolean flush) {
        try {
            new SaveTask(server, flush).save();
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
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
     * Stop the current {@link UsefulSavesCommand#scheduledSaveFuture}
     */
    public static int stopTask(CommandContext<CommandSource> context) {
        if (stopScheduledTask()) {
            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.scheduled.stop"), false);
        } else {
            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.scheduled.stop.notRunning"), false);
        }
        return 1;
    }

    public static boolean stopScheduledTask() {
        if (scheduledSaveFuture != null) {
            return scheduledSaveFuture.cancel(true);
        } else return false;
    }

    public static ScheduledFuture<?> getScheduledSaveFuture() {
        return scheduledSaveFuture;
    }

    public static void setScheduledSaveFuture(ScheduledFuture<SaveTask> scheduledSaveFuture) {
        UsefulSavesCommand.scheduledSaveFuture = scheduledSaveFuture;
    }
}
