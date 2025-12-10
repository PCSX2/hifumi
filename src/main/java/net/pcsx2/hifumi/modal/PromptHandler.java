package net.pcsx2.hifumi.modal;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.permissions.PermissionLevel;
import net.pcsx2.hifumi.util.EmbedUtil;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public class PromptHandler {

    public static void handle(ModalInteractionEvent event) {
        if (!HifumiBot.getSelf().getPermissionManager().hasPermission(PermissionLevel.ADMIN, event.getMember())) {
            Messaging.logInfo("PromptHandler", "handle", "User " + event.getUser().getAsMention() + " tried to send a modal interaction for a prompt, but does not have permission.");
            event.reply("Permissions error, staff have been notified").setEphemeral(true).queue();
            return;
        }

        String title = event.getValue("title").getAsString();
        String body = event.getValue("body").getAsString();

        EmbedBuilder eb = EmbedUtil.newFootedEmbedBuilder(event.getMember());
        eb.setTitle(title);
        eb.setDescription(body);
        event.replyEmbeds(eb.build()).queue();
    }
}
