package fr.zeamateis.usefulsaves.server.job;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import fr.zeamateis.usefulsaves.UsefulSaves;
import fr.zeamateis.usefulsaves.server.config.UsefulSavesConfig;
import fr.zeamateis.usefulsaves.util.MessageUtils;
import fr.zeamateis.usefulsaves.util.ZipUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.io.FileUtils;
import org.quartz.*;

import java.nio.file.Path;
import java.nio.file.Paths;


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
    private boolean flush;
    private ZipUtils zipUtils;

    public void setup(MinecraftServer server, boolean flush) {
        this.server = server;
        this.flush = flush;
        String folderName = this.server.getFolderName().replaceAll("\\s+", "-");
        this.zipUtils = new ZipUtils(String.format("%s/%s", UsefulSaves.getInstance().getBackupFolder(), folderName), folderName);
    }

    private void createZipFile() {
        this.zipUtils.generateFileList(FileUtils.getFile(server.getFolderName()));
        this.zipUtils.zipIt(server.getServerTime());
    }

    public void processSave() {
        if (saveIfServerEmpty()) {
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
                createZipFile();
                Path lastSavePath = Paths.get(this.zipUtils.outputFileName);
                MessageUtils.printMessageForAllPlayers(server, new TranslationTextComponent("usefulsaves.message.save.savingSuccess", lastSavePath.getFileName()));
                MessageUtils.printMessageForAllPlayers(server, new TranslationTextComponent("usefulsaves.message.save.backupSize", FileUtils.byteCountToDisplaySize(lastSavePath.toFile().length())));
                //Console print
                MessageUtils.printMessageInConsole("Success saving for %s, %s file size", lastSavePath.getFileName(), FileUtils.byteCountToDisplaySize(lastSavePath.toFile().length()));
            }
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
        this.setup((MinecraftServer) jobDataMap.get("server"), jobDataMap.getBoolean("flush"));
        this.processSave();
    }

}
