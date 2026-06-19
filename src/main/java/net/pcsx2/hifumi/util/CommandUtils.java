// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.util;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class CommandUtils {

    /**
     * Checks if a SlashCommandInteractionEvent contains a subcommand specified
     * in a given list.
     * @param event - The SlashCommandInteractionEvent to check
     * @param subcommands - The list of possible subcommands to compare to
     * @return True if no subcommand matched and the event was replied to, false
     * otherwise.
     */
    public static boolean replyIfBadSubcommand(SlashCommandInteractionEvent event, String... subcommands) {
        if (event.getSubcommandName() != null) {
            for (String subcommand : subcommands) {
                if (event.getSubcommandName().equals(subcommand.toLowerCase())) {
                    return false;
                }
            }
        }
        
        event.getHook().sendMessage("Invalid subcommand `" + event.getSubcommandName() + "`").setEphemeral(true).queue();
        return true;
    }
    
    /**
     * Checks if a SlashCommandInteractionEvent contains defined OptionMappings
     * for the specified options.
     * @param event - SlashCommandInteractionEvent to check
     * @param options - Option names to check for
     * @return True if an option was missing and the event was replied to, false
     * otherwise.
     */
    public static boolean replyIfMissingOptions(SlashCommandInteractionEvent event, String... options) {
        StringBuilder sb = new StringBuilder();
        
        for (String opt : options) {
            OptionMapping mapping = event.getOption(opt);
            
            if (mapping == null) {
                sb.append("Missing required option `")
                    .append(opt)
                    .append("`");
            }
        }
        
        if (sb.length() > 0) {
            event.getHook().sendMessage(sb.toString()).setEphemeral(true).queue();
            return true;
        }
        
        return false;
    }
}
