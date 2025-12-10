package net.pcsx2.hifumi.async;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import net.pcsx2.hifumi.HifumiBot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class MessageBulkDeleteRunnable implements Runnable {

    private String guildId;
    private String userId;

    public MessageBulkDeleteRunnable(String guildId, String userId) {
        this.guildId = guildId;
        this.userId = userId;
    }

    @Override
    public void run() {
        OffsetDateTime now = OffsetDateTime.now();
        Guild server = HifumiBot.getSelf().getJDA().getGuildById(guildId);
        
        try {
            for (TextChannel channel : server.getTextChannels()) {
                if (!server.getPublicRole().hasAccess(channel)) {
                    continue;
                }

                List<Message> messages = channel.getIterableHistory().takeAsync(1000).thenApply((list) -> 
                    list.stream().filter((m) -> 
                        m.getAuthor().getId().equals(userId)
                    ).filter((m) ->
                        Duration.between(m.getTimeCreated(), now).toHours() < 1
                    ).collect(Collectors.toList())
                ).get();

                for (Message msg : messages) {
                    msg.delete().queue();
                }
            }
        } catch (Exception e) {
            // Squelch
        }
    }
}
