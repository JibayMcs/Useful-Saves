package fr.zeamateis.usefulsaves.server.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * @author ZeAmateis
 */
public class CalendarArgumentType implements ArgumentType<Calendar> {

    private static final Collection<String> EXAMPLES = Arrays.asList("15 17:22:30", "00:00:00");

    private final Calendar calendar;

    private SimpleDateFormat dayHoursFormat = new SimpleDateFormat("dd HH:mm:ss");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("EEEE:HH:mm:ss");

    public CalendarArgumentType() {
        calendar = Calendar.getInstance();
    }

    public static CalendarArgumentType calendar() {
        return new CalendarArgumentType();
    }

    public static Calendar getCalendar(CommandContext<?> context, String name) {
        return context.getArgument(name, Calendar.class);
    }

    @Override
    public Calendar parse(StringReader reader) throws CommandSyntaxException {

        String timeFormatted = reader.readQuotedString();
        try {
            Date date = timeFormat.parse(timeFormatted);
            calendar.setTime(date);
        } catch (ParseException ignored) {
        }

        return calendar;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if ("sunday".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("sunday");
        }
        if ("monday".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("monday");
        }
        if ("tuesday".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("tuesday");
        }
        if ("wednesday".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("wednesday");
        }
        if ("thursday".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("thursday");
        }
        if ("friday".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("friday");
        }
        if ("saturday".startsWith(builder.getRemaining().toLowerCase())) {
            builder.suggest("saturday");
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public enum DayOfWeek {
        SUNDAY(Calendar.SUNDAY),
        MONDAY(Calendar.MONDAY),
        TUESDAY(Calendar.TUESDAY),
        WEDNESDAY(Calendar.WEDNESDAY),
        THURSDAY(Calendar.THURSDAY),
        FRIDAY(Calendar.FRIDAY),
        SATURDAY(Calendar.SATURDAY);

        int day;

        DayOfWeek(int day) {
            this.day = day;
        }

        public int getDay() {
            return day;
        }


    }
}
