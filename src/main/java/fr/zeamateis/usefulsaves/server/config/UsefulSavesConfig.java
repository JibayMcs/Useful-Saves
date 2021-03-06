package fr.zeamateis.usefulsaves.server.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * @author ZeAmateis
 */
public class UsefulSavesConfig {

    public static final ForgeConfigSpec COMMON_SPECS;
    public static final Common COMMON;

    static {
        Pair<Common, ForgeConfigSpec> commonPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPECS = commonPair.getRight();
        COMMON = commonPair.getLeft();
    }

    public static class Common {

        public static ForgeConfigSpec.ConfigValue<String> timeZone, backupsFolder, cronTaskObject;
        public static ForgeConfigSpec.BooleanValue saveIfServerEmpty, printMessage, enableTaskOnServerStart, deleteOldOnMaximumReach;
        public static ForgeConfigSpec.IntValue /*backupCompression,*/ maximumSavedBackups;

        public static ForgeConfigSpec.ConfigValue<List<String>> savedFileWhitelist;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("common");

            timeZone = builder
                    .comment("Define a TimeZone if server clock mismatch players clock", "Automaticly generated by default")
                    .define("timeZone", TimeZone.getDefault().getID());

            backupsFolder = builder
                    .comment("Define a backup folder")
                    .define("backupsFolder", "./backups");

            cronTaskObject = builder
                    .comment("Define a json formatted cron task to save/load", "If \"enableTaskOnServerStart\" is enabled task will be loaded.")
                    .define("cronTaskObject", "{}");

            saveIfServerEmpty = builder
                    .comment("Process save task if no player connected ?")
                    .define("saveIfServerEmpty", false);

            printMessage = builder
                    .comment("Print Useful Saves messages in chat ?")
                    .define("printMessage", true);

            /*backupCompression = builder.comment("Define compression level for backups archive files")
                    .defineInRange("backupCompression", -1, -1, 9);*/

            enableTaskOnServerStart = builder
                    .comment("Enable the previous saved scheduled task on server start ?", "Ensure \"cronTask\" is not empty or null")
                    .define("enableTaskOnServerStart", true);

            savedFileWhitelist = builder
                    .comment("Define a list of files or folder to save on saving process", "Use absolute path !")
                    .define("savedFileWhitelist", new ArrayList<String>());

            maximumSavedBackups = builder
                    .comment("Define maximum created backups", "\"-1\" = unlimited saves")
                    .defineInRange("maximumSavedBackups", -1, -1, Integer.MAX_VALUE);

            deleteOldOnMaximumReach = builder
                    .comment("Defined to delete oldest backups if maximum saves are reach",
                            "Used if \"maximumSavedBackups\" are defined")
                    .define("deleteOldOnMaximumReach", false);

            builder.pop();
        }
    }
}
