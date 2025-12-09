/**
 * This file is part of HifumiBot, licensed under the MIT License (MIT)
 * 
 * Copyright (c) 2020 RedPanda4552 (https://github.com/RedPanda4552)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
