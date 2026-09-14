// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.filter;

import java.awt.Color;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.MessageObject;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.AttachmentUtils;
import net.pcsx2.hifumi.util.EmbedUtil;
import net.pcsx2.hifumi.util.Messaging;
import net.pcsx2.hifumi.util.RoleUtils;

public class HoneypotHelper implements IFilterHelper {

    private static final int AGE_MINUTES_TO_REMOVE_MESSAGES = 5;
    
    // Yeah I'm hard coding this because I don't feel like serializing another file over this, sue me.
    // We trust our own bot name so before String.format, replace "<bot>" with the bot's display name,
    // then %s to substitute player name.
    private static final List<String> KILL_FEED_MESSAGES = List.of(
            "%s was pricked to death",
            "%s walked into a cactus while trying to escape <bot>",
            "%s experienced kinetic energy",
            "%s experienced kinetic energy while trying to escape <bot>",
            "%s blew up",
            "%s was blown up by <bot>",
            "%s was blown up by <bot> using <item>",
            "%s was killed by [Intentional Game Design]",
            "%s hit the ground too hard",
            "%s hit the ground too hard while trying to escape <bot>",
            "%s fell from a high place",
            "%s fell off a ladder",
            "%s fell off some vines",
            "%s fell off scaffolding",
            "%s fell while climbing",
            "%s fell out of water",
            "%s was doomed to fall",
            "%s was doomed to fall by <bot>",
            "%s was doomed to fall by <bot> using <item>",
            "%s was impaled on a stalagmite",
            "%s was impaled on a stalagmite while fighting <bot>",
            "%s was squashed by a falling anvil",
            "%s was squashed by a falling block",
            "%s was skewered by a falling stalactite",
            "%s went up in flames",
            "%s walked into fire while fighting <bot>",
            "%s burned to death",
            "%s was burned to a crisp while fighting <bot>",
            "%s tried to swim in lava",
            "%s tried to swim in lava to escape <bot>",
            "%s was struck by lightning",
            "%s was struck by lightning while fighting <bot>",
            "%s discovered the floor was lava",
            "%s walked into the danger zone due to <bot>",
            "%s died because not just the floor is lava",
            "%s was killed by magic",
            "%s was killed by magic while trying to escape <bot>",
            "%s was killed by <bot> using magic",
            "%s was killed by <bot> using <item>",
            "%s froze to death",
            "%s was frozen to death by <bot>",
            "%s was slain by <bot>",
            "%s was slain by <bot> using <item>",
            "%s was stung to death",
            "%s was stung to death by <bot> using <item>",
            "%s was obliterated by a sonically-charged shriek",
            "%s was obliterated by a sonically-charged shriek while trying to escape <bot> wielding <item>",
            "%s was smashed by <bot>",
            "%s was speared by <bot>",
            "%s was speared by <bot> using <item>",
            "%s was shot by <bot>",
            "%s was shot by <bot> using <item>",
            "%s was pummeled by <bot>",
            "%s was pummeled by <bot> using <item>",
            "%s was fireballed by <bot>",
            "%s was fireballed by <bot> using <item>",
            "%s was shot by a skull from <bot>",
            "%s was shot by a skull from <bot> using <item>",
            "%s starved to death",
            "%s starved to death while fighting <bot>",
            "%s suffocated in a wall",
            "%s suffocated in a wall while fighting <bot>",
            "%s was squished too much",
            "%s was squashed by <bot>",
            "%s left the confines of this world",
            "%s left the confines of this world while fighting <bot>",
            "%s was poked to death by a sweet berry bush",
            "%s was poked to death by a sweet berry bush while trying to escape <bot>",
            "%s was killed while trying to hurt <bot>",
            "%s was killed by <item> while trying to hurt <bot>",
            "%s was impaled by <bot>",
            "%s was impaled by <bot> with <item>",
            "%s fell out of the world",
            "%s didn't want to live in the same world as <bot>",
            "%s withered away",
            "%s withered away while fighting <bot>",
            "%s died",
            "%s died because of <bot>",
            "%s was killed",
            "%s was killed while fighting <bot>",
            "%s was killed by even more magic"
    );
    
    private static final List<String> ITEMS_LIST = List.of(
            "Memory Card (8MB) for PlayStation®2",
            "a mint condition SCPH-39001",
            "a dying laser diode",
            "a spindle of bitrotted discs",
            "a PS1 controller pretending to be a Dualshock®2",
            "a guitar controller with a broken whammy bar",
            "PlayStation® Underground Holiday 2004 Demo Disc",
            "the GSCube",
            "the Nintendo 2",
            "a vibecoded piece of slop PR",
            "a copy of PCSX2 0.9.8",
            "PCSX2 1.6.0 on a compromised Windows 7 PC",
            "32 bit Windows",
            "a used toilet brush",
            "John McAfee's nose hair trimmer",
            "the EE executing an interlocked COP2 instruction while VU0 is infinitely looping",
            "Marvel Nemesis: Rise of the Imperfects",
            "a TLB miss",
            "a Chevy small block with reverse flow heads",
            "a FGM-148 Javelin Advanced Anti-Tank Weapon System-Medium",
            "a demon core",
            "a Scroll of Doom",
            "a 1983 Cadillac Coupe de Ville with Dayton wheels",
            "an ear of corn",
            "a sack of potatoes",
            "a spirit bomb",
            "a half filled NOS tank",
            "a Power Brick",
            "a nitro crate",
            "the Suck Cannon",
            "the RYNO",
            "a Pocket Crotchetizer that is still warm",
            "Zweihänder",
            "Excalibur",
            "a Keyblade",
            "the Wand of Gamelon"
    );
    
    private final Message message;
    
    private Random random;
    
    public HoneypotHelper(Message message) {
        this.message = message;
        this.random = new Random(System.currentTimeMillis());
    }
    
    @Override
    public boolean run() {
        String honeypotChannelId = HifumiBot.getSelf().getConfig().honeypotOptions.channelId;
        String honeypotRoleId = HifumiBot.getSelf().getConfig().honeypotOptions.roleId;
        Guild server = this.message.getGuild();
        Member member = this.message.getMember();
        
        // First of all, did they post in the honeypot        
        if (honeypotChannelId != null && this.message.getChannelId().equals(honeypotChannelId)) {
            // Check some elevated permissions for people we might want to exempt.
            if (member.hasPermission(Permission.MANAGE_SERVER, Permission.MANAGE_ROLES, Permission.MESSAGE_MANAGE)) {
                return false;
            }
            
            // Now actually do the role assignment
            if (honeypotRoleId != null) {
                if (RoleUtils.memberHasRole(member, honeypotRoleId)) {
                    return true;
                }
                
                Role honeypotRole = server.getRoleById(honeypotRoleId);
                // Block so we can be sure the event handler will be ready to sweep up new messages
                // and the below delete will not miss any in between actions 
                server.addRoleToMember(member, honeypotRole).complete();
                
                OffsetDateTime currentTime = OffsetDateTime.now();
                OffsetDateTime cutoffTime = currentTime.minusMinutes(AGE_MINUTES_TO_REMOVE_MESSAGES);
                ModActions.deleteAllMessageFromUserSinceExcept(member.getIdLong(), cutoffTime.toEpochSecond(), this.message.getIdLong());
                return true;
            }
        // If not the honeypot channel, but they have the role
        } else if (RoleUtils.memberHasRole(member, honeypotRoleId)) {
            // Smite them
            ModActions.timeoutAndNotifyUser(this.message.getGuild(), this.message.getAuthor().getId());
            ModActions.kickAndNotifyUser(server, member.getIdLong());
            OffsetDateTime currentTime = OffsetDateTime.now();
            Database.insertSpamkickEvent(currentTime.toEpochSecond(), member.getIdLong(), "honeypot", Optional.of(this.message.getIdLong()));
            this.notifyStaff();
            this.updateChannel(member);
            return true;
        }
        
        return false;
    }
    
    private void notifyStaff() {
        User user = this.message.getAuthor();
        
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Automatically fired /spamkick on user in honeypot");
        eb.setDescription("User has fallen into the honeypot and was automatically kicked after posting a message in another channel.\n\n");
        eb.appendDescription("Their messages sent within the last " + AGE_MINUTES_TO_REMOVE_MESSAGES + " minutes should be deleted or are in the process of being deleted.\n\n");
        eb.appendDescription("A short reference of what they sent is included below. No further action is required.\n\n");
        eb.addField("User ID", user.getId(), true);
        eb.addField("Username", user.getName(), true);
        eb.addField("Display Name (as mention)", user.getAsMention(), true);
        eb.setColor(Color.YELLOW);
        
        // Body content preview
        eb.addField("Body Content (raw, first 100 chars)", StringUtils.abbreviate(this.message.getContentRaw(), 100), false);
        
        // Attachments
        eb.addField(EmbedUtil.newAttachmentListField(this.message.getAttachments()));

        ArrayList<FileUpload> files = AttachmentUtils.getMinifiedAttachments(message);
        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addEmbeds(eb.build());
        mb.addFiles(files);
        Messaging.logInfoMessage(mb.build());
    }
    
    private void updateChannel(Member member) {
        OffsetDateTime currentTime = OffsetDateTime.now(ZoneOffset.UTC);
        StringBuilder sb = new StringBuilder();
        sb.append(this.getPrimaryKillFeedMessage(member));
        //sb.append(this.getMultiKillMessage(currentTime));
        //sb.append(this.getKillingSpreeMessage(currentTime));
        
        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.setContent(sb.toString());
        Messaging.sendMessage(HifumiBot.getSelf().getConfig().honeypotOptions.channelId, mb.build());
        
        // Check if we should post another warning message
        Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        ArrayList<MessageObject> previousWarnings = Database.getIdenticalMessagesSinceTime(HifumiBot.getSelf().getJDA().getSelfUser().getIdLong(), HifumiBot.getSelf().getConfig().honeypotOptions.warningMessage, weekAgo.getEpochSecond());
        boolean sendWarning = false;
        
        if (previousWarnings.isEmpty()) {
            sendWarning = true;
        } else {
            OffsetDateTime lastWarningTime = previousWarnings.get(0).getCreatedTime();
            ArrayList<MessageObject> messagesSinceWarning = Database.getAllMessagesSinceTimeInChannel(HifumiBot.getSelf().getJDA().getSelfUser().getIdLong(), lastWarningTime.toEpochSecond(), Long.valueOf(HifumiBot.getSelf().getConfig().honeypotOptions.channelId));
            
            if (messagesSinceWarning.size() > 12) {
                sendWarning = true;
            }
        }
        
        if (sendWarning) {
            mb = new MessageCreateBuilder();
            mb.setContent(HifumiBot.getSelf().getConfig().honeypotOptions.warningMessage);
            Messaging.sendMessage(HifumiBot.getSelf().getConfig().honeypotOptions.channelId, mb.build());
        }
    }
    
    private int getRandomBounded(int upperBound) {
        int i = this.random.nextInt();
        
        if (i < 0) {
            i *= -1;
        }
        
        if (i > upperBound) {
            i %= upperBound;
        }
        
        return i;
    }
    
    private String getPrimaryKillFeedMessage(Member member) {
        int msgPos = this.getRandomBounded(KILL_FEED_MESSAGES.size() - 1);
        String message = KILL_FEED_MESSAGES.get(msgPos).replace("<bot>", HifumiBot.getSelf().getJDA().getSelfUser().getEffectiveName());
        
        if (message.contains("<item>")) {
            int itemPos = this.getRandomBounded(ITEMS_LIST.size() - 1);
            String item = ITEMS_LIST.get(itemPos);
            message = message.replace("<item>", item);
        }
        
        return String.format(message, member.getEffectiveName());
    }
    
    private String getMultiKillMessage(OffsetDateTime currentTime) {
        OffsetDateTime startOfDay = currentTime.truncatedTo(ChronoUnit.DAYS);
        Optional<Integer> multiEventCountOpt = Database.getHoneypotEventCountSince(startOfDay.toEpochSecond());
        
        if (multiEventCountOpt.isPresent()) {
            Integer eventCount = multiEventCountOpt.get();
            
            switch (eventCount) {
                case 2 -> {
                    return "\nDouble kill!";
                }
                case 3 -> {
                    return "\nTriple kill!";
                }
                case 4 -> {
                    return "\nOverkill!";
                }
                case 5 -> {
                    return "\nKilltacular!";
                }
                case 6 -> {
                    return "\nKilltrocity!";
                }
                case 7 -> {
                    return "\nKillimanjaro!";
                }
                case 8 -> {
                    return "\nKilltastrophe!";
                }
                case 9 -> {
                    return "\nKillpocalypse!";
                }
                case 10 -> {
                    return "\nKillionaire!";
                }
                default -> { }
            }
        }
        
        return "";
    }
    
    private String getKillingSpreeMessage(OffsetDateTime currentTime) {
        OffsetDateTime startOfWeek = currentTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
        Optional<Integer> spreeEventCountOpt = Database.getHoneypotEventCountSince(startOfWeek.toEpochSecond());
        
        if (spreeEventCountOpt.isPresent()) {
            Integer eventCount = spreeEventCountOpt.get();
            
            switch (eventCount) {
                case 5 -> {
                    return "\nKilling spree!";
                }
                case 10 -> {
                    return "\nKilling frenzy!";
                }
                case 15 -> {
                    return "\nRunning riot!";
                }
                case 20 -> {
                    return "\nRampage!";
                }
                case 25 -> {
                    return "\nUntouchable!";
                }
                case 30 -> {
                    return "\nInvincible!";
                }
                case 35 -> {
                    return "\nInconceivable!";
                }
                default -> { }
            }
        }
        
        return "";
    }
}
