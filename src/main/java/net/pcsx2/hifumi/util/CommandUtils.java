/**
 * This file is part of HifumiBot, licensed under the MIT License (MIT)
 * 
 * Copyright (c) 2020 RedPanda4552 (https://github.com/RedPanda4552)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
