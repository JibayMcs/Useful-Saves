package fr.zeamateis.usefulsaves.server.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ZeAmateis
 */
public class TimeUnitArgumentType implements ArgumentType<TimeUnit> {

    private static final Collection<String> EXAMPLES = Stream.of(TimeUnit.values())
            .map(Enum::name)
            .collect(Collectors.toList());

    public TimeUnitArgumentType() {
    }


    public static TimeUnitArgumentType timeUnit() {
        return new TimeUnitArgumentType();
    }

    public static TimeUnit getTimeUnit(final CommandContext<?> context, final String name) {
        return context.getArgument(name, TimeUnit.class);
    }


    @Override
    public TimeUnit parse(StringReader reader) {
        return TimeUnit.valueOf(reader.readUnquotedString().toUpperCase());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        if ("nanoseconds".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("nanoseconds");
        }
        if ("microseconds".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("microseconds");
        }
        if ("seconds".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("seconds");
        }
        if ("minutes".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("minutes");
        }
        if ("hours".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("hours");
        }
        if ("days".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("days");
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
