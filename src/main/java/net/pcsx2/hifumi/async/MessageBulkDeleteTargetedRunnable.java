package net.pcsx2.hifumi.async;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import net.pcsx2.hifumi.HifumiBot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class MessageBulkDeleteTargetedRunnable implements Runnable {

    private String guildId;
    private String userId;
    private String channelId;
    private String messageContent;
    private int hours;

    public MessageBulkDeleteTargetedRunnable(String guildId, String userId, String channelId, String messageContent, int hours) {
        this.guildId = guildId;
        this.userId = userId;
        this.channelId = channelId;
        this.messageContent = messageContent;
        this.hours = hours;
    }

    @Override
    public void run() {
        Guild server = HifumiBot.getSelf().getJDA().getGuildById(guildId);
        
        try {
            TextChannel channel = server.getTextChannelById(this.channelId);
            List<Message> messages = null;
                
            do {
                messages = channel.getIterableHistory().takeAsync(100).thenApply((list) -> 
                    list.stream().filter((m) -> 
                        m.getAuthor().getId().equals(userId)
                    ).filter((m) ->
                        Duration.between(m.getTimeCreated(), OffsetDateTime.now()).toHours() < this.hours
                    ).filter((m) ->
                        m.getContentDisplay().contains(this.messageContent) || (!m.getEmbeds().isEmpty() && m.getEmbeds().get(0).getDescription().contains(this.messageContent))
                    ).collect(Collectors.toList())
                ).get();

                for (Message msg : messages) {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {}
                    msg.delete().complete();
                }
            } while (!messages.isEmpty());
        } catch (Exception e) {
            // Squelch
        }
    }
}
