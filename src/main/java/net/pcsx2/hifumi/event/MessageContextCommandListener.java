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
import net.pcsx2.hifumi.command.AbstractMessageContextCommand;
import net.pcsx2.hifumi.command.AbstractUserContextCommand;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.CommandEventObject;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageContextCommandListener extends ListenerAdapter {

    private HashMap<String, AbstractMessageContextCommand> messageCommands = HifumiBot.getSelf().getCommandIndex().getMessageCommands();
    private HashMap<String, AbstractUserContextCommand> userCommands = HifumiBot.getSelf().getCommandIndex().getUserCommands();
    
    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("Message context commands are disabled in DMs.").setEphemeral(true).queue();
            return;
        }

        // Fetch the last occurrence of this command, in this channel,
        // from another user, within the ninja time, if available.
        Optional<CommandEventObject> recentCommandInstance = Database.getLatestCommandEventNotFromUser(
            event.getChannelIdLong(), 
            event.getCommandIdLong(), 
            event.getUser().getIdLong()
        );

        // Store this command event to database
        Database.insertCommandEvent(
            event.getCommandIdLong(), 
            "message", 
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
        
        if (messageCommands.containsKey(event.getName())) {
            try {
                messageCommands.get(event.getName()).executeIfPermission(event);
            } catch (Exception e) {
                Messaging.logException("MessageContextCommandListener", "onMessageContextInteraction", e);
                event.reply("An internal exception occurred and has been reported to admins.").setEphemeral(true).queue();
            }
        }
    }
    
    @Override
    public void onUserContextInteraction(UserContextInteractionEvent event) {
        if (!event.isFromGuild()) {
            event.reply("User context commands are disabled in DMs.").setEphemeral(true).queue();
            return;
        }

        // Fetch the last occurrence of this command, in this channel,
        // from another user, within the ninja time, if available.
        Optional<CommandEventObject> recentCommandInstance = Database.getLatestCommandEventNotFromUser(
            event.getChannelIdLong(), 
            event.getCommandIdLong(), 
            event.getUser().getIdLong()
        );

        // Store this command event to database
        Database.insertCommandEvent(
            event.getCommandIdLong(), 
            "user", 
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
        
        if (userCommands.containsKey(event.getName())) {
            try {
                userCommands.get(event.getName()).executeIfPermission(event);
            } catch (Exception e) {
                Messaging.logException("MessageContextCommandListener", "onUserContextInteraction", e);
                event.reply("An internal exception occurred and has been reported to admins.").setEphemeral(true).queue();
            }
        }
    }

}
