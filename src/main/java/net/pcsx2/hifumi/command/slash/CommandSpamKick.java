// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command.slash;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.MessageObject;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandSpamKick extends AbstractSlashCommand {

    private static final int AGE_MINUTES_TO_REMOVE_MESSAGES = 5;
    
    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        OptionMapping opt = event.getOption("user");
        
        if (opt == null) {
            event.reply("Required option `user` missing").setEphemeral(true);
            return;
        }
        
        Member member = opt.getAsMember();

        if (member == null) {
            event.reply("User has already left the server.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(true).queue();
        
        try {
            long cooldownSeconds = HifumiBot.getSelf().getConfig().spamOptions.cooldownSeconds;
            OffsetDateTime cooldownSubtracted = OffsetDateTime.now().minusSeconds(cooldownSeconds);
            long cooldownEpochSeconds = cooldownSubtracted.toEpochSecond();

            // First, timeout the user to stop any spam
            member.timeoutFor(Duration.ofHours(1)).complete();

            // Now round up any messages and delete them. We have to do this first,
            // because we (probably) need the member to still be live in order to check member.hasAccess
            ArrayList<MessageObject> allMessages = Database.getAllMessagesSinceTime(member.getIdLong(), cooldownEpochSeconds);

            for (MessageObject message : allMessages) {
                try {
                    TextChannel channel = HifumiBot.getSelf().getJDA().getTextChannelById(message.getChannelId());

                    // Check hasAccess; should stop the automod notification messages from being deleted,
                    // since the user won't have access to that channel.
                    if (member.hasAccess(channel)) {
                        HifumiBot.getSelf().getJDA().getTextChannelById(message.getChannelId()).deleteMessageById(message.getMessageId()).queue();
                    }
                } catch (Exception e) {
                    // Squelch
                }
            }

            // Finally, DM and kick
            ModActions.kickAndNotifyUser(event.getGuild(), member.getIdLong());

            User usr = member.getUser();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Command /spamkick Used");
            eb.setDescription("Sent a private message to user warning them we think they are a bot and have been kicked from the server. Their recent messages are in the process of being deleted.");
            eb.addField("User (As Mention)", usr.getAsMention(), true);
            eb.addField("Username", usr.getName(), true);
            eb.addField("User ID", usr.getId(), true);
            eb.setFooter("This action was taken by " + event.getUser().getName() + ".");

            Messaging.logInfoEmbed(eb.build());
            event.getHook().editOriginal("Successfully messaged and kicked " + member.getUser().getAsMention()).queue();
            OffsetDateTime currentTime = OffsetDateTime.now();
            OffsetDateTime cutoffTime = currentTime.minusMinutes(AGE_MINUTES_TO_REMOVE_MESSAGES);
            ModActions.deleteAllMessageFromUserSince(usr.getIdLong(), cutoffTime.toEpochSecond());
        } catch (Exception e) {
            Messaging.logException("CommandSpamKick", "onExecute", e);
            event.getHook().editOriginal("An internal error occurred, check the bot logging channel").queue();
        }
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("spamkick", "Send a user a DM telling them their account is compromised and spamming, then kick the user")
                .addOption(OptionType.USER, "user", "User to DM and kick", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS));
    }

}
