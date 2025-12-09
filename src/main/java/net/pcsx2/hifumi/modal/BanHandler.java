package net.pcsx2.hifumi.modal;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.async.MessageBulkDeleteRunnable;
import net.pcsx2.hifumi.util.Messaging;
import net.pcsx2.hifumi.util.UserUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public class BanHandler {

    public static void handle(ModalInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            Messaging.logInfo("BanHandler", "handle", "User " + event.getUser().getAsMention() + " tried to send a modal interaction for a ban, but does not have permission.");
            event.reply("Permissions error, staff have been notified").setEphemeral(true).queue();
            return;
        }

        String userId = event.getValue("userid").getAsString();
        Optional<User> userOpt = UserUtils.getOrRetrieveUser(userId);
        
        try {
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                event.getGuild().ban(user, 0, TimeUnit.SECONDS).complete();
                MessageBulkDeleteRunnable runnable = new MessageBulkDeleteRunnable(event.getGuild().getId(), user.getId());
                HifumiBot.getSelf().getScheduler().runOnce(runnable);
    
                event.reply("User " + user.getAsMention() + "(" + user.getName() + ") has been banned").setEphemeral(true).queue();   
            } else {
                event.reply("User could not be retrieved; Discord might have already deleted them.").setEphemeral(true).queue();
            }
        } catch (Exception e) {
            Messaging.logException("BanHandler", "handle", e);
        }
    }
}
