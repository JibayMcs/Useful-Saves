package fr.zeamateis.usefulsaves.util;

import fr.zeamateis.usefulsaves.UsefulSaves;
import fr.zeamateis.usefulsaves.server.config.UsefulSavesConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;

/**
 * @author ZeAmateis
 */
public class MessageUtils {

    public static void printMessageForAllPlayers(MinecraftServer server, ITextComponent message) {
        if (UsefulSavesConfig.Common.printMessage.get())
            server.getPlayerList().getPlayers().forEach(serverPlayerEntity -> serverPlayerEntity.sendMessage(message));
    }

    public static void printMessageInConsole(String message, Object... args) {
        UsefulSaves.getLogger().info(String.format(message, args));
    }
}
