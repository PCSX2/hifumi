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
package net.pcsx2.hifumi.event;

import java.util.HashMap;
import java.util.Optional;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.command.dynamic.DynamicChoice;
import net.pcsx2.hifumi.command.dynamic.DynamicCommand;
import net.pcsx2.hifumi.command.dynamic.DynamicSubcommand;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.CommandEventObject;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class SlashCommandListener extends ListenerAdapter {

    private final HashMap<String, AbstractSlashCommand> slashCommands = HifumiBot.getSelf().getCommandIndex().getSlashCommands();
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // First, abort if not in a server
        if (!event.isFromGuild()) {
            event.reply("Slash commands are disabled in DMs.").setEphemeral(true).queue();
            return;
        }

        // Fetch the last occurrence of this command, in this channel,
        // from another user, within the ninja time, if available.
        Optional<CommandEventObject> recentCommandInstance = Database.getLatestCommandEventNotFromUser(event.getChannelIdLong(), event.getCommandIdLong(), event.getUser().getIdLong());

        // Store this command event to database
        Database.insertCommandEvent(
            event.getCommandIdLong(), 
            "slash", 
            event.getName(), 
            event.getSubcommandGroup(), 
            event.getSubcommandName(), 
            event.getIdLong(), 
            event.getUser(),
            event.getChannelIdLong(),
            event.getTimeCreated().toEpochSecond(), 
            recentCommandInstance.isPresent(),
            event.getOptions()
        );

        // Now abort if it was a ninja
        if (recentCommandInstance.isPresent()) {
            event.reply(":ninja:").setEphemeral(true).queue();
            return;
        }

        // Finally, try processing the command and pass along to the appropriate command to execute
        if (slashCommands.containsKey(event.getName())) {
            try {
                slashCommands.get(event.getName()).onExecute(event);
            } catch (Exception e) {
                e.printStackTrace();
                Messaging.logException("SlashCommandListener", "onSlashCommand", e);
                event.getHook().sendMessage("An internal exception occurred and has been reported to admins.").setEphemeral(true).queue();
            }
        } else if (HifumiBot.getSelf().getDynCmdConfig().dynamicCommands.containsKey(event.getName())) {
            DynamicCommand command = HifumiBot.getSelf().getDynCmdConfig().dynamicCommands.get(event.getName());
            
            if (command.getSubcommands().containsKey(event.getSubcommandGroup())) {
                DynamicSubcommand subcommand = command.getSubcommand(event.getSubcommandGroup());
                
                if (subcommand.getChoices().containsKey(event.getSubcommandName())) {
                    DynamicChoice choice = subcommand.getChoice(event.getSubcommandName());

                    OptionMapping mentionOpt = event.getOption("mention");

                    if (mentionOpt != null) {
                        choice.execute(event, mentionOpt.getAsMember());
                    } else {
                        choice.execute(event);
                    }
                }
            }
        } else {
            Messaging.logInfo("SlashCommandListener", "onSlashCommandInteraction", "Reveived slash command `" + event.getName() + "`, but we don't have any kind of handler for it!");
        }
    }
}
