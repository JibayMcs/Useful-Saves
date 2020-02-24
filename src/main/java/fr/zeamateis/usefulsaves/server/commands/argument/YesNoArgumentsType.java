package fr.zeamateis.usefulsaves.server.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * @author ZeAmateis
 */
public class YesNoArgumentsType implements ArgumentType<Boolean> {
    private static final Collection<String> EXAMPLES = Arrays.asList("true", "false");

    private YesNoArgumentsType() {
    }

    public static YesNoArgumentsType yesNo() {
        return new YesNoArgumentsType();
    }

    public static boolean getResponse(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Boolean.class);
    }

    @Override
    public Boolean parse(final StringReader reader) throws CommandSyntaxException {
        return readYesNo(reader);
    }

    private boolean readYesNo(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String value = reader.readString();
        if (value.isEmpty()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedBool().createWithContext(reader);
        }

        if (value.equals("yes")) {
            return true;
        } else if (value.equals("no")) {
            return false;
        }
        return false;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        if ("yes".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("yes");
        }
        if ("no".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("no");
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}