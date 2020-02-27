package fr.zeamateis.usefulsaves.server.commands;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import fr.zeamateis.usefulsaves.UsefulSaves;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Simple helping command to deal with crontab
 *
 * @author ZeAmateis
 */
public class CronHelpCommand extends CommandBase {


    private int getDescription(ICommandSender sender, String expression, String locale) {
        UsefulSaves.getLogger().debug(expression + " " + locale);
        expression = expression.replaceAll("\\s", "+").replaceAll("\"", "");
        try {
            String fromattedURL = String.format("https://cronexpressiondescriptor.azurewebsites.net/api/descriptor/?expression=%s&locale=%s", expression, locale);
            URL url = new URL(fromattedURL);
            JsonObject descriptor = UsefulSaves.getInstance().getGson().fromJson(new InputStreamReader(url.openStream()), new TypeToken<JsonObject>() {
            }.getType());
            notifyCommandListener(sender, this, "usefulsaves.message.cronhelp.result", descriptor.get("description").getAsString());
        } catch (IOException ex) {
            notifyCommandListener(sender, this, "usefulsaves.message.cronhelp.unableToReachURL");

        }
        return 1;
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
        if (args.length >= 6) {
            String expression = String.join("\\s", Arrays.asList(args[0].replaceAll("\"", ""), args[1], args[2], args[3], args[4], args[5].replaceAll("\"", "")));
            if (args.length == 6)
                getDescription(sender, expression, "en-US");
            else if (args.length == 7)
                getDescription(sender, expression, args[6]);
        } else {

        }
    }


    /**
     * Gets the name of the command
     */
    @Override
    public String getName() {
        return "cron";
    }

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
        return Arrays.asList("cron");
    }


    /**
     * Check if the given ICommandSender has permission to execute this command
     *
     * @param server
     * @param sender
     */
    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
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
        return Arrays.asList("");
    }
}
