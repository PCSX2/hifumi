package net.pcsx2.hifumi.async;

import java.awt.Color;
import java.time.Duration;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.MessageObject;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.Messaging;
import net.pcsx2.hifumi.util.Strings;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class UrlChangeReviewRunnable implements Runnable {

    private final Message message;

    private String newestUrlsConcat;
    private String olderUrlsConcat;
    
    public UrlChangeReviewRunnable(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        if (this.applyUrlCheck()) {
            User usr = this.message.getAuthor();

            // Timeout the user first
            ModActions.timeoutAndNotifyUser(this.message.getGuild(), usr.getIdLong());
            // Next delete the offending message
            this.message.delete().complete();

            // Now report to staff
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Hyperlink Edit Event");
            eb.setDescription("**Message Content (Truncated to 3900 chars)**\n```\n" + StringUtils.truncate(this.message.getContentStripped().replaceAll("\s", " "), 3900) + "\n```");
            eb.addField("Old Hyperlinks", StringUtils.abbreviate(this.olderUrlsConcat, 1000), true);
            eb.addField("New Hyperlinks", StringUtils.abbreviate(this.newestUrlsConcat, 1000), true);
            eb.addField("Channel", this.message.getChannel().getAsMention(), true);
            eb.addField("User (As Mention)", usr.getAsMention(), true);
            eb.addField("Username", usr.getName(), true);
            eb.addField("User ID", usr.getId(), true);
            eb.setColor(Color.YELLOW);
            Messaging.logInfoEmbed(eb.build());

            // Finally, tell them why
            eb = new EmbedBuilder();
            eb.setTitle("Hyperlink Change Warning");
            eb.setDescription(HifumiBot.getSelf().getConfig().urlChangeOptions.message);
            eb.setColor(Color.YELLOW);
            Messaging.sendPrivateMessageEmbed(usr, eb.build());
        }

    }

    public boolean applyUrlCheck() {
        if (this.message.getTimeCreated() == null || this.message.getTimeEdited() == null) {
            return false;
        }

        Duration timeToEdit = Duration.between(this.message.getTimeCreated(), this.message.getTimeEdited());

        if (timeToEdit.toMinutes() > HifumiBot.getSelf().getConfig().urlChangeOptions.blockAfterMinutes) {
            ArrayList<MessageObject> allMessageRevisions = Database.getAllMessageRevisions(this.message.getIdLong());

            // We only care about the newest content and what came before it - check that at least two items exist,
            // and disregard the rest. The Database class will have ordered them from newest first.
            if (allMessageRevisions.size() < 2) {
                return false;
            }

            MessageObject newest = allMessageRevisions.get(0);
            MessageObject older = allMessageRevisions.get(1);

            ArrayList<String> newestUrls = Strings.extractUrls(newest.getBodyContent());
            ArrayList<String> olderUrls = Strings.extractUrls(older.getBodyContent());

            this.newestUrlsConcat = StringUtils.join(newestUrls, "\n");
            this.olderUrlsConcat = StringUtils.join(olderUrls, "\n");

            if (!newestUrlsConcat.equals(olderUrlsConcat)) {
                return true;
            }
        }

        return false;
    }
}
