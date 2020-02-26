package fr.zeamateis.usefulsaves.server.job;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import fr.zeamateis.usefulsaves.UsefulSaves;
import fr.zeamateis.usefulsaves.server.config.UsefulSavesConfig;
import fr.zeamateis.usefulsaves.util.MessageUtils;
import fr.zeamateis.usefulsaves.util.ZipUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.io.FileUtils;
import org.quartz.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Main Job class to backup files
 * <p>
 * {@link DisallowConcurrentExecution} prevent multiple SaveJob instances running
 *
 * @author ZeAmateis
 */
@DisallowConcurrentExecution
public class SaveJob implements Job {
    private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(new TranslationTextComponent("commands.save.failed"));

    private MinecraftServer server;
    private CommandSource commandSource;
    private boolean flush;
    private ZipUtils zipUtils = new ZipUtils();

    /**
     * Setup the Save task
     *
     * @param server          The {@link MinecraftServer} instance
     * @param flush           Flush save ?
     * @param deleteExisting  Delete existing save ? (on duplicated names)
     * @param sourceWhitelist Whitelist of folders and file to add in save archive
     */
    public void setup(MinecraftServer server, CommandSource commandSource, boolean flush, boolean deleteExisting, List<Path> sourceWhitelist) {
        this.server = server;
        this.commandSource = commandSource;
        this.flush = flush;
        this.zipUtils.setDeleteExisting(deleteExisting);
        this.zipUtils.getSourceWhitelist().addAll(sourceWhitelist);
    }

    public void processSave() {
        if (saveIfServerEmpty()) {
            if (canProcessSave()) {
                MessageUtils.printMessageForAllPlayers(server, new TranslationTextComponent("usefulsaves.message.save.saving"));
                //Console Print
                MessageUtils.printMessageInConsole("Saving the game (this may take a moment!)");

                server.getPlayerList().saveAllPlayerData();
                boolean flag = server.save(true, flush, true);

                if (!flag) {
                    try {
                        throw FAILED_EXCEPTION.create();
                    } catch (CommandSyntaxException ignored) {
                    }
                } else {
                    //Create zipped file
                    LocalDateTime date = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH-mm-ss-SSS");
                    server.getWorlds().forEach(serverWorld -> {
                        Path outputCompressedSave = Paths.get(UsefulSaves.getInstance().getBackupFolder().getPath(), String.format("%s-%s.zip", server.getFolderName().replaceAll("[^\\dA-Za-z ]", "").replaceAll("\\s+", "-"), formatter.format(date)));
                        this.zipUtils.setOutputSavePath(outputCompressedSave);
                        this.zipUtils.getSourceWhitelist().forEach(path -> UsefulSaves.getLogger().debug("Zipping: " + path.toString()));
                        try {
                            this.zipUtils.createSave();
                            MessageUtils.printMessageForAllPlayers(server, new TranslationTextComponent("usefulsaves.message.save.savingSuccess", outputCompressedSave.getFileName()));
                            MessageUtils.printMessageForAllPlayers(server, new TranslationTextComponent("usefulsaves.message.save.backupSize", FileUtils.byteCountToDisplaySize(outputCompressedSave.toFile().length())));
                            //Console print
                            MessageUtils.printMessageInConsole("Success saving for %s, %s file size", outputCompressedSave.getFileName(), FileUtils.byteCountToDisplaySize(outputCompressedSave.toFile().length()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } else if (UsefulSavesConfig.Common.deleteOldOnMaximumReach.get()) {
                this.tryDeleteOldestBackup();
                this.processSave();
            } else {
                UsefulSaves.getInstance().getSchedulerManager().unscheduleSaveJob();
                UsefulSaves.getInstance().getSchedulerManager().setStatus(SchedulerManager.SchedulerStatus.MAXIMUM_BACKUP_REACH);
                if (this.commandSource != null)
                    this.commandSource.sendFeedback(new TranslationTextComponent("usefulsaves.message.maximumBackup"), false);
                MessageUtils.printMessageInConsole("Maximum backups reach in backup folder");
            }
        }
    }

    /**
     * Check maximum backups restriction
     */
    private boolean canProcessSave() {
        if (UsefulSavesConfig.Common.maximumSavedBackups.get() == -1)
            return true;
        else {
            return UsefulSavesConfig.Common.maximumSavedBackups.get() != -1 && countBackups() <= UsefulSavesConfig.Common.maximumSavedBackups.get();
        }
    }

    private int countBackups() {
        try (Stream<Path> walk = Files.walk(Paths.get(UsefulSaves.getInstance().getBackupFolder().getPath()))) {
            List<String> backupList = walk
                    .filter(Files::isRegularFile)
                    .map(x -> x.getFileName().toString())
                    .collect(Collectors.toList());
            return backupList.size();
        } catch (IOException ignored) {
            return -1;
        }
    }

    private void tryDeleteOldestBackup() {
        try (Stream<Path> walk = Files.walk(Paths.get(UsefulSaves.getInstance().getBackupFolder().getPath()))) {
            Comparator<? super Path> lastModifiedComparator =
                    (p1, p2) -> Long.compare(p1.toFile().lastModified(), p2.toFile().lastModified());
            walk.filter(Files::isRegularFile).sorted(lastModifiedComparator).limit(1).map(Path::toFile)
                    .forEach(file -> {
                        try {
                            FileUtils.forceDelete(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    /**
     * Process or not, save task if server is empty
     */
    private boolean saveIfServerEmpty() {
        if (UsefulSavesConfig.Common.saveIfServerEmpty.get() && server.getPlayerList().getPlayers().isEmpty())
            return true;
        else return !server.getPlayerList().getPlayers().isEmpty();
    }


    /**
     * This creates a Zip file at the location specified by zip
     * containing the full directory tree rooted at contents
     *
     * @param zip      the zip file, this must not exist
     * @param contents the root of the directory tree to copy
     * @throws IOException, specific exceptions thrown for specific errors
     */
    public void createZip(final Path zip, final Path contents) throws IOException {
        if (Files.exists(zip)) {
            throw new FileAlreadyExistsException(zip.toString());
        }
        if (!Files.exists(contents)) {
            throw new FileNotFoundException("The location to zip must exist");
        }
        final Map<String, String> env = new HashMap<>();
        //creates a new Zip file rather than attempting to read an existing one
        env.put("create", "true");
        // locate file system by using the syntax
        // defined in java.net.JarURLConnection

        final URI uri = URI.create("jar:" + zip.toFile().toURI());
        try (final FileSystem zipFileSystem = FileSystems.newFileSystem(uri.normalize(), env)) {
            final Stream<Path> files = Files.walk(contents);
            {
                final Path root = zipFileSystem.getPath("/");
                files.forEach(file -> {
                    try {
                        copyToZip(root, contents, file);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    /**
     * Copy a specific file/folder to the zip archive
     * If the file is a folder, create the folder. Otherwise copy the file
     *
     * @param root     the root of the zip archive
     * @param contents the root of the directory tree being copied, for relativization
     * @param file     the specific file/folder to copy
     */
    private void copyToZip(final Path root, final Path contents, final Path file) throws IOException {
        final Path to = root.resolve(contents.relativize(file).toString());
        if (Files.isDirectory(file)) {
            Files.createDirectories(to);
        } else {
            Files.copy(file, to);
        }
    }


    /**
     * <p>
     * Called by the <code>{@link org.quartz.Scheduler}</code> when a <code>{@link org.quartz.Trigger}</code>
     * fires that is associated with the <code>Job</code>.
     * </p>
     *
     * <p>
     * The implementation may wish to set a
     * {@link JobExecutionContext#setResult(Object) result} object on the
     * {@link JobExecutionContext} before this method exits.  The result itself
     * is meaningless to Quartz, but may be informative to
     * <code>{@link org.quartz.JobListener}s</code> or
     * <code>{@link org.quartz.TriggerListener}s</code> that are watching the job's
     * execution.
     * </p>
     *
     * @param context
     * @throws JobExecutionException if there is an exception while executing the job.
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        MinecraftServer server = (MinecraftServer) jobDataMap.get("server");
        CommandSource commandSource = (CommandSource) jobDataMap.get("commandSource");
        boolean isFlushed = jobDataMap.getBoolean("flush");
        boolean deleteIfExist = jobDataMap.getBoolean("deleteExisting");
        List<Path> sourceWhitelist = (List<Path>) jobDataMap.get("sourceWhitelist");

        this.setup(server, commandSource, isFlushed, deleteIfExist, sourceWhitelist);
        this.processSave();
    }

}
