package fr.zeamateis.usefulsaves.server.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.zeamateis.usefulsaves.UsefulSaves;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.util.concurrent.CompletableFuture;

/**
 * @author ZeAmateis
 */
public class CronArgumentType implements ArgumentType<CronExpression> {

    public CronArgumentType() {
    }

    public static CronArgumentType cron() {
        return new CronArgumentType();
    }

    public static CronExpression getCron(CommandContext<?> context, String name) {
        return context.getArgument(name, CronExpression.class);
    }

    @Override
    public CronExpression parse(StringReader reader) throws CommandSyntaxException {
        try {
            return new CronExpression(reader.readString());
        } catch (ParseException e) {
            UsefulSaves.getLogger().error(e.getLocalizedMessage());
        }
        return null;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return builder.suggest("\"0 0 * ? * *\"").buildFuture();
    }
}