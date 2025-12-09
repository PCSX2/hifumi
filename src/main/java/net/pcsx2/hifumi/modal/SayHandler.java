package net.pcsx2.hifumi.modal;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.permissions.PermissionLevel;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public class SayHandler {

    public static void handle(ModalInteractionEvent event) {
        if (!HifumiBot.getSelf().getPermissionManager().hasPermission(PermissionLevel.ADMIN, event.getMember())) {
            Messaging.logInfo("SayHandler", "handle", "User " + event.getUser().getAsMention() + " tried to send a modal interaction for /say, but does not have permission.");
            event.reply("Permissions error, staff have been notified").setEphemeral(true).queue();
            return;
        }

        String body = event.getValue("body").getAsString();
        Messaging.sendMessage(event.getChannel(), body);
        event.reply("Message posted!").setEphemeral(true).queue();
    }
}
