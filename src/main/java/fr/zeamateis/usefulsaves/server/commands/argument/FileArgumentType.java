package fr.zeamateis.usefulsaves.server.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.zeamateis.usefulsaves.UsefulSaves;
import net.minecraft.command.ISuggestionProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ZeAmateis
 */
public class FileArgumentType implements ArgumentType<File> {

    public FileArgumentType() {
    }

    public static File getFile(final CommandContext<?> context, final String name) {
        return context.getArgument(name, File.class);
    }

    public static FileArgumentType file() {
        return new FileArgumentType();
    }

    @Override
    public File parse(StringReader reader) throws CommandSyntaxException {
        return new File(UsefulSaves.getInstance().getBackupFolder(), reader.readUnquotedString());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        try (Stream<Path> walk = Files.walk(Paths.get(UsefulSaves.getInstance().getBackupFolder().getPath()))) {
            List<String> result = walk.filter(Files::isRegularFile)
                    .map(x -> x.getFileName().toString()).collect(Collectors.toList());
            return ISuggestionProvider.suggest(result, builder);
        } catch (IOException e) {
            e.printStackTrace();
            return Suggestions.empty();
        }
    }

}
