package net.pcsx2.hifumi.async;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.MessageObject;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.Messaging;
import net.pcsx2.hifumi.util.Strings;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

public class AntiBotRunnable implements Runnable {

    private static final int LINK_THRESHOLD = 3;
    private static final int DAYS_SINCE_LAST_MESSAGE = 30;
    private static final int AGE_MINUTES_TO_REMOVE_MESSAGES = 5;

    private final Message message;

    public AntiBotRunnable(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        isImageScam();
    }

    private boolean isImageScam() {
        String bodyContent = this.message.getContentRaw();
        ArrayList<String> links = Strings.extractUrls(bodyContent);

        if (links.size() >= LINK_THRESHOLD) {
            long authorIdLong = this.message.getAuthor().getIdLong();
            OffsetDateTime currentTime = OffsetDateTime.now();
            OffsetDateTime cutoffTime = currentTime.minusDays(DAYS_SINCE_LAST_MESSAGE);
            ArrayList<MessageObject> messagesSinceCutoffTime = Database.getAllMessagesSinceTime(authorIdLong, cutoffTime.toEpochSecond());

            // This list will always contain the message which triggered this scan.
            // So to check for "empty", we really need to check for size = 0 or 1.
            // If the list is "empty", then the user has been inactive for a long time,
            // or is a brand new user. In either case, this is their "first message in recent time".
            if (messagesSinceCutoffTime.size() <= 1) {
                boolean timeoutRes = ModActions.timeoutAndNotifyUser(this.message.getGuild(), authorIdLong);
                EmbedBuilder eb = new EmbedBuilder();
                User user = this.message.getAuthor();

                if (timeoutRes) {
                    // Since our timeout succeeded, now sweep up any other messages the bot might have
                    // blasted out while this runnable was going.
                    OffsetDateTime timeToRemoveMessagesSince = OffsetDateTime.now().minusMinutes(AGE_MINUTES_TO_REMOVE_MESSAGES);
                    ArrayList<MessageObject> otherMessages = Database.getAllMessagesSinceTime(this.message.getAuthor().getIdLong(), timeToRemoveMessagesSince.toEpochSecond());

                    for (MessageObject otherMessage : otherMessages) {
                        TextChannel channel = HifumiBot.getSelf().getJDA().getTextChannelById(otherMessage.getChannelId());
                        channel.deleteMessageById(otherMessage.getMessageId()).queue();
                    }

                    eb.setTitle("User timed out for suspected image scams");
                    eb.setDescription("User has not posted anything else in the last " + DAYS_SINCE_LAST_MESSAGE + " days, but posted at least " + LINK_THRESHOLD + " links in one message.\n\n");
                    eb.appendDescription("Any other messages they have sent in the last " + AGE_MINUTES_TO_REMOVE_MESSAGES + " minutes are also being deleted for safety.\n\n");
                    eb.appendDescription("You may review the links below. If they look safe, you may use the green button to remove the timeout. If they look malicious, use the red button to automatically run the /spamkick command.\n\n");
                    eb.addField("User ID", String.valueOf(authorIdLong), true);
                    eb.addField("Username", user.getName(), true);
                    eb.addField("Display Name (as mention)", user.getAsMention(), true);
                    eb.setColor(Color.YELLOW);

                    for (String link : links) {
                        eb.appendDescription(link + "\n");
                    }

                    MessageCreateBuilder mb = new MessageCreateBuilder();
                    mb.addEmbeds(eb.build());
                    mb.addComponents(ActionRow.of(
                        Button.of(ButtonStyle.DANGER, "imagescam:dospamkick:" + authorIdLong, "Looks like a bot scam, kick user"), 
                        Button.of(ButtonStyle.SUCCESS, "imagescam:clear:" + authorIdLong, "Looks innocent, remove timeout")
                    ));
                    Messaging.sendMessage(HifumiBot.getSelf().getConfig().channels.systemOutputChannelId, mb.build());
                    return true;
                } else {
                    eb.setTitle("Failed to timeout suspected bot");
                    eb.setDescription("Was unable to timeout and/or notify the user of the timeout. Please check if the suspected bot is still active in the server.");
                    eb.addField("User ID", String.valueOf(authorIdLong), true);
                    eb.addField("Username", user.getName(), true);
                    eb.addField("Display Name (as mention)", user.getAsMention(), true);
                    eb.setColor(Color.RED);
                    Messaging.logInfoEmbed(eb.build());
                    return false;
                }
            }
        }

        return false;
    }
}
