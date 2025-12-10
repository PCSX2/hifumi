package net.pcsx2.hifumi.event;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;

import net.pcsx2.hifumi.EventLogging;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.MemberEventObject;
import net.pcsx2.hifumi.database.objects.WarezEventObject;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MemberEventListener extends ListenerAdapter {
    
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        // Store user and join records, then check for the join-leave-join pattern
        Database.insertMemberJoinEvent(event);
        ArrayList<MemberEventObject> events = Database.getRecentMemberEvents(event.getMember().getIdLong());

        if (events.size() >= 3) {
            MemberEventObject latestEvent = events.get(0);
            MemberEventObject secondEvent = events.get(1);
            MemberEventObject thirdEvent = events.get(2);

            if (latestEvent.getAction().equals(MemberEventObject.Action.JOIN) && secondEvent.getAction().equals(MemberEventObject.Action.LEAVE) && thirdEvent.getAction().equals(MemberEventObject.Action.JOIN)) {
                Instant newestJoinTime = Instant.ofEpochSecond(latestEvent.getTimestamp());
                Instant olderJoinTime = Instant.ofEpochSecond(thirdEvent.getTimestamp());
                long minutesBetween = Duration.between(olderJoinTime, newestJoinTime).toMinutes();

                if (minutesBetween < 5) {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Fast Join-Leave-Join Detected");
                    eb.setDescription("A join-leave-join pattern was detected within less than five minutes. This is often a sign that someone is trying to remove the 'new here' badge given to new server members. Check the <#" + HifumiBot.getSelf().getConfig().channels.logging.memberJoin + "> channel for details.");
                    eb.addField("Username (As Mention)", event.getUser().getAsMention(), true);
                    eb.addField("Username (Plain Text)", event.getUser().getName(), true);
                    eb.addField("User ID", event.getUser().getId(), false);
                    Messaging.logInfoEmbed(eb.build());
                }
            }
        }
        
        // Reassign warez
        Optional<WarezEventObject> warezEventOpt = Database.getLatestWarezAction(event.getMember().getIdLong());

        if (warezEventOpt.isPresent()) {
            if (warezEventOpt.get().getAction().equals(WarezEventObject.Action.ADD)) {
                Role role = event.getGuild().getRoleById(HifumiBot.getSelf().getConfig().roles.warezRoleId);
                event.getGuild().addRoleToMember(event.getMember(), role).queue();
            }
        }
        
        EventLogging.logGuildMemberJoinEvent(event, warezEventOpt);
    }

    @Override 
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        OffsetDateTime time = OffsetDateTime.now();
        // Store user and leave records
        Database.insertMemberRemoveEvent(event, time);
        EventLogging.logGuildMemberRemoveEvent(event);
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        OffsetDateTime time = OffsetDateTime.now();
        // Store user and leave records
        Database.insertMemberBanEvent(event, time);
        EventLogging.logGuildBanEvent(event);
    }
}
