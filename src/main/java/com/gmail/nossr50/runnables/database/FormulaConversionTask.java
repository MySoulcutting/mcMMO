package com.gmail.nossr50.runnables.database;

import com.gmail.nossr50.database.DatabaseManager;
import com.gmail.nossr50.datatypes.experience.FormulaType;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.player.PlayerProfile;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.locale.LocaleLoader;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.player.UserManager;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class FormulaConversionTask extends BukkitRunnable {
    private CommandSender sender;
    private FormulaType previousFormula;

    public FormulaConversionTask(CommandSender sender, FormulaType previousFormula) {
        this.sender = sender;
        this.previousFormula = previousFormula;
    }

    @Override
    public void run() {
        int convertedUsers = 0;
        long startMillis = System.currentTimeMillis();
        for (String playerName : mcMMO.getDatabaseManager().getStoredUsers()) {
            McMMOPlayer mcMMOPlayer = UserManager.getOfflinePlayer(playerName);
            PlayerProfile profile;

            // If the mcMMOPlayer doesn't exist, create a temporary profile and check if it's present in the database. If it's not, abort the process.
            if (mcMMOPlayer == null) {
                profile = mcMMO.getDatabaseManager().loadPlayerProfile(playerName, false);

                if (!profile.isLoaded()) {
                    mcMMO.p.debug("Profile not loaded.");
                    continue;
                }

                editValues(profile);
                // Since this is a temporary profile, we save it here.
                profile.scheduleAsyncSave();
            } else {
                profile = mcMMOPlayer.getProfile();
                editValues(profile);
            }
            convertedUsers++;
            Misc.printProgress(convertedUsers, DatabaseManager.progressInterval, startMillis);
        }

        sender.sendMessage(LocaleLoader.getString("Commands.mcconvert.Experience.Finish", mcMMO.getConfigManager().getConfigLeveling().getConfigExperienceFormula().toString()));
    }

    private void editValues(PlayerProfile profile) {
        mcMMO.p.debug("========================================================================");
        mcMMO.p.debug("Conversion report for " + profile.getPlayerName() + ":");
        for (PrimarySkillType primarySkillType : PrimarySkillType.NON_CHILD_SKILLS) {
            int oldLevel = profile.getSkillLevel(primarySkillType);
            int oldXPLevel = profile.getSkillXpLevel(primarySkillType);
            int totalOldXP = mcMMO.getFormulaManager().calculateTotalExperience(oldLevel, oldXPLevel, previousFormula);

            if (totalOldXP == 0) {
                continue;
            }

            int[] newExperienceValues = mcMMO.getFormulaManager().calculateNewLevel(primarySkillType, (int) Math.floor(totalOldXP / 1.0));
            int newLevel = newExperienceValues[0];
            int newXPlevel = newExperienceValues[1];

            mcMMO.p.debug("  Skill: " + primarySkillType.toString());

            mcMMO.p.debug("    OLD:");
            mcMMO.p.debug("      Level: " + oldLevel);
            mcMMO.p.debug("      XP " + oldXPLevel);
            mcMMO.p.debug("      Total XP " + totalOldXP);

            mcMMO.p.debug("    NEW:");
            mcMMO.p.debug("      Level " + newLevel);
            mcMMO.p.debug("      XP " + newXPlevel);
            mcMMO.p.debug("------------------------------------------------------------------------");

            profile.modifySkill(primarySkillType, newLevel);
            profile.setSkillXpLevel(primarySkillType, newXPlevel);
        }
    }
}
