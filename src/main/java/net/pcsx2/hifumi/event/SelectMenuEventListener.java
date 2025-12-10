/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package net.pcsx2.hifumi.event;

import java.util.HashMap;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 *
 * @author pandubz
 */
public class SelectMenuEventListener extends ListenerAdapter {

    private final HashMap<String, AbstractSlashCommand> slashCommands = HifumiBot.getSelf().getCommandIndex().getSlashCommands();
    
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();
        String[] parts = componentId.split(":");

        if (parts.length < 3) {
            Messaging.logInfo("SelectMenuEventListener", "onStringSelectInteraction", "Received a string select menu event, but got a malformed string select menu ID. Received:\n```\n" + componentId + "\n```");
            event.reply("Something went wrong with this select menu. Admins have been notified.").setEphemeral(true).queue();
            return;
        }

        switch (parts[0]) {
            case "gameindex":
                event.deferEdit().queue();
                slashCommands.get("gameindex").handleStringSelectEvent(event);
                break;
            default:
                break;
        }
    }
}
