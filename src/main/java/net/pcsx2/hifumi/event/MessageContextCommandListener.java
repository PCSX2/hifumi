// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.event;

import java.util.HashMap;
import java.util.Optional;

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractMessageContextCommand;
import net.pcsx2.hifumi.command.AbstractUserContextCommand;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.CommandEventObject;
import net.pcsx2.hifumi.util.Messaging;

public class MessageContextCommandListener extends ListenerAdapter {

    private HashMap<String, AbstractMessageContextCommand> messageCommands = HifumiBot.getSelf().getCommandIndex().getMessageCommands();
    private HashMap<String, AbstractUserContextCommand> userCommands = HifumiBot.getSelf().getCommandIndex().getUserCommands();
    
    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        HifumiBot.getSelf().getScheduler().runOnce(() -> {
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
        });
    }
    
    @Override
    public void onUserContextInteraction(UserContextInteractionEvent event) {
        HifumiBot.getSelf().getScheduler().runOnce(() -> {
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
        });
    }
}
