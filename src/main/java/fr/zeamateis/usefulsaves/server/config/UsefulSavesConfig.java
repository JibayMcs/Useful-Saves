package fr.zeamateis.usefulsaves.server.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

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

        public static ForgeConfigSpec.ConfigValue<String> backupsFolderName;
        public static ForgeConfigSpec.BooleanValue saveIfServerEmpty, printMessage, enableTaskOnServerStart;
        public static ForgeConfigSpec.IntValue backupCompression;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("common");

            backupsFolderName = builder
                    .comment("Name of the \"backup\" folder")
                    .define("backupsFolderName", "backups");

            saveIfServerEmpty = builder
                    .comment("Process save task if no player connected ?")
                    .define("saveIfServerEmpty", false);

            printMessage = builder
                    .comment("Print Useful Saves messages in chat ?")
                    .define("printMessage", true);

            backupCompression = builder.comment("Define compression level for backups archive files")
                    .defineInRange("backupCompression", -1, -1, 9);

            enableTaskOnServerStart = builder
                    .comment("Enable the previous saved scheduled task on server start ?")
                    .define("enableTaskOnServerStart", true);

            builder.pop();
        }
    }
}
