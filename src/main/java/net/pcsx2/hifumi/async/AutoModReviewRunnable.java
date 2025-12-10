package net.pcsx2.hifumi.async;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.AutoModEventObject;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.Log;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public class AutoModReviewRunnable implements Runnable {

    private final Guild server;
    private final long userId;
    private final OffsetDateTime time;

    public AutoModReviewRunnable(Guild server, long userId, OffsetDateTime time) {
        this.server = server;
        this.userId = userId;
        this.time = time;
    }

    @Override
    public void run() {
        Log.info("AutoMod review runnable started");

        if (this.reviewAutoMod()) {
            Log.info("AutoMod event passed review step");
            User usr = HifumiBot.getSelf().getJDA().getUserById(this.userId);
            Log.info("User retrieved (ID = " + usr.getId() + " )");

            // Kick the user first
            Log.info("Kick attempt starting");
            boolean wasKicked = ModActions.kickAndNotifyUser(this.server, this.userId);
            Log.info("Kick attempt returned " + wasKicked);

            // Now report to staff
            if (wasKicked) {
                Log.info("Sending embed for admins");
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("User Automatically Kicked");
                eb.setDescription("User was automatically kicked from the server for repeated filter incidents.");
                eb.addField("User (As Mention)", usr.getAsMention(), true);
                eb.addField("Username", usr.getName(), true);
                eb.addField("User ID", usr.getId(), true);
                eb.setColor(Color.ORANGE);
                Log.info("Embed ready");
                Messaging.logInfoEmbed(eb.build());
                Log.info("Embed sent");
            }
        }
    }

    private boolean reviewAutoMod() {
        long cooldownSeconds = HifumiBot.getSelf().getConfig().autoModOptions.cooldownSeconds;
        OffsetDateTime cooldownSubtracted = this.time.minusSeconds(cooldownSeconds);
        ArrayList<AutoModEventObject> autoModEvents = Database.getAutoModEventsSinceTime(this.userId, cooldownSubtracted);
    
        if (autoModEvents == null || autoModEvents.isEmpty()) {
            Log.info("AutoMod review found nothing, false");
            return false;
        }

        if (autoModEvents.size() >= HifumiBot.getSelf().getConfig().autoModOptions.maxMessages) {
            Log.info("AutoMod review found exceeded threshold for AutoMod events, true");
            return true;
        }

        Log.info("AutoMod review exiting without matching either criteria, false");
        return false;
    }
}
