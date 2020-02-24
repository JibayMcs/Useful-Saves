package fr.zeamateis.usefulsaves.server.task;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import fr.zeamateis.usefulsaves.UsefulSaves;
import fr.zeamateis.usefulsaves.server.config.UsefulSavesConfig;
import fr.zeamateis.usefulsaves.util.MessageUtils;
import fr.zeamateis.usefulsaves.util.ZipUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author ZeAmateis
 */
public class SaveTask implements Runnable {
    private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(new TranslationTextComponent("commands.save.failed"));

    private final MinecraftServer server;

    private final boolean flush;

    private final ZipUtils zipUtils;

    public SaveTask(MinecraftServer server, boolean flush) {
        this.server = server;
        this.flush = flush;
        String folderName = this.server.getFolderName().replaceAll("\\s+", "-");
        this.zipUtils = new ZipUtils(String.format("%s/%s", UsefulSaves.getInstance().getBackupFolder(), folderName), folderName);
    }

    private void createZipFile() {
        this.zipUtils.generateFileList(FileUtils.getFile(server.getFolderName()));
        this.zipUtils.zipIt(server.getServerTime());
    }

    public void save() throws CommandSyntaxException {
        if (saveIfServerEmpty()) {
            MessageUtils.printMessageForAllPlayers(server, new TranslationTextComponent("usefulsaves.message.save.saving"));
            //Console Print
            MessageUtils.printMessageInConsole("Saving the game (this may take a moment!)");

            server.getPlayerList().saveAllPlayerData();
            boolean flag = server.save(true, flush, true);

            if (!flag) {
                throw FAILED_EXCEPTION.create();
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
        else if (!server.getPlayerList().getPlayers().isEmpty())
            return true;
        else return false;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        try {
            save();
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
    }

    public ZipUtils getZipUtils() {
        return zipUtils;
    }
}
