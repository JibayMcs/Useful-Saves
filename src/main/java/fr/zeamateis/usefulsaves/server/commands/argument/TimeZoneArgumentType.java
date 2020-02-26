package fr.zeamateis.usefulsaves.server.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;

/**
 * @author ZeAmateis
 */
public class TimeZoneArgumentType implements ArgumentType<TimeZone> {

    public TimeZoneArgumentType() {
    }

    public static TimeZoneArgumentType timeZone() {
        return new TimeZoneArgumentType();
    }

    public static TimeZone getTimeZone(CommandContext<?> context, String name) {
        return context.getArgument(name, TimeZone.class);
    }

    @Override
    public TimeZone parse(StringReader reader) throws CommandSyntaxException {
        return TimeZone.getTimeZone(String.valueOf(reader.readQuotedString()));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String[] ids = TimeZone.getAvailableIDs();
        for (String id : ids) {
            if (String.format("\"%s\"", id).startsWith(builder.getRemaining()))
                builder.suggest(String.format("\"%s\"", id));
        }
        return builder.buildFuture();
    }
}
