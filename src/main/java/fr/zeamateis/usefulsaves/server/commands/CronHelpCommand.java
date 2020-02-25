package fr.zeamateis.usefulsaves.server.commands;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.zeamateis.usefulsaves.UsefulSaves;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.TranslationTextComponent;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Simple helping command to deal with crontab
 *
 * @author ZeAmateis
 */
public class CronHelpCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSource>literal("cron")
                .then(Commands.argument("expression", StringArgumentType.string())
                        .then(Commands.argument("locale", StringArgumentType.word())
                                .executes(context -> getDescription(context, StringArgumentType.getString(context, "expression"), StringArgumentType.getString(context, "locale")))
                        )
                        .executes(context -> getDescription(context, StringArgumentType.getString(context, "expression"), "en-US"))
                )
        );
    }

    static int getDescription(CommandContext<CommandSource> context, String expression, String locale) {
        System.out.println(expression + " " + locale);
        expression = expression.replaceAll("\\s", "+").replaceAll("\"", "");
        try {
            String fromattedURL = String.format("https://cronexpressiondescriptor.azurewebsites.net/api/descriptor/?expression=%s&locale=%s", expression, locale);
            URL url = new URL(fromattedURL);
            JsonObject descriptor = UsefulSaves.getInstance().getGson().fromJson(new InputStreamReader(url.openStream()), new TypeToken<JsonObject>() {
            }.getType());
            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.cronhelp.result", descriptor.get("description").getAsString()), false);

        } catch (IOException ex) {
            context.getSource().sendFeedback(new TranslationTextComponent("usefulsaves.message.cronhelp.unableToReachURL"), false);
        }
        return 1;
    }
}
