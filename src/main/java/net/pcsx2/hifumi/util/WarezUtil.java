package net.pcsx2.hifumi.util;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.config.Config.SerializedEmbedField;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.WarezEventObject;
import net.pcsx2.hifumi.permissions.PermissionLevel;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

public class WarezUtil {

    public static void applyWarez(GenericCommandInteractionEvent event, User user, Optional<Member> memberOpt, Optional<Message> messageOpt) {
        try {
            // Sanity check against the user itself
            if (user == null) {
                event.getHook()
                    .sendMessage("Could not record warez; user was null (were they deleted from Discord already?)")
                    .setEphemeral(true)
                    .queue();
                return;
            }

            // Log the event
            WarezEventObject warezEvent = new WarezEventObject(
                OffsetDateTime.now().toEpochSecond(), 
                user.getIdLong(),
                WarezEventObject.Action.ADD,
                messageOpt.isPresent() ? messageOpt.get().getIdLong() : null
            );

            if (!Database.insertWarezEvent(warezEvent)) {
                event.getHook()
                    .sendMessage("Warez record could not be stored (SQL error occurred, check logs for details)")
                    .setEphemeral(true)
                    .queue();
                return;
            }

            // Check if the member left the server, and generate output as appropriate
            if (memberOpt.isEmpty()) {
                event.getHook().sendMessage("Warez record logged (User has already left the server)").queue();
            } else {
                Member member = memberOpt.get();
                MessageCreateBuilder mb = new MessageCreateBuilder();
                mb.setContent(member.getAsMention());
                EmbedBuilder eb = new EmbedBuilder();
                
                if (!HifumiBot.getSelf().getPermissionManager().hasPermission(PermissionLevel.MOD, member)) {
                    try {
                        // Add the role. The role event will then add to the database.
                        Role warezRole = event.getGuild().getRoleById(HifumiBot.getSelf().getConfig().roles.warezRoleId);
                        event.getGuild().addRoleToMember(member, warezRole).queue();
                    } catch (InsufficientPermissionException e) {
                        Messaging.logInfo("WarezUtil", "applyWarez", "Failed to assign role to " + member.getAsMention() + " (insufficient permissions)");                        
                    }
                }
                
                eb.setTitle(HifumiBot.getSelf().getConfig().warezPrompt.title);
                eb.setDescription(HifumiBot.getSelf().getConfig().warezPrompt.body);
                eb.setColor(Color.RED);
                
                for (SerializedEmbedField field : HifumiBot.getSelf().getConfig().warezPrompt.fields) {
                    eb.addField(field.name, field.value, field.inline);
                }

                mb.setEmbeds(eb.build());
                

                String appealsChannelId = HifumiBot.getSelf().getConfig().channels.appealsChannelId;
                TextChannel appealsChannel = HifumiBot.getSelf().getJDA().getTextChannelById(appealsChannelId);

                if (messageOpt.isPresent()) {
                    Message forwardedMsg = messageOpt.get().forwardTo(appealsChannel).complete();
                    Message warezPrompt = forwardedMsg.reply(mb.build()).complete();
                    event.getHook().sendMessage("Warez applied to user: " + warezPrompt.getJumpUrl()).queue();
                } else {
                    Message warezPrompt = appealsChannel.sendMessage(mb.build()).complete();
                    event.getHook().sendMessage("Warez applied to user: " + warezPrompt.getJumpUrl()).queue();
                }
            }
        } catch (Exception e) {
            event.getHook()
                .sendMessage("Warez assignment failed; check bot output channel for error log.")
                .setEphemeral(true)
                .queue();
            Messaging.logException("WarezUtil", "applyWarez", e);
        }
    }

    public static MessageEmbed createWarezHistoryEmbed(long userIdLong) {
        ArrayList<WarezEventObject> events = Database.getAllWarezActionsForUser(userIdLong);

        Optional<User> userOpt = UserUtils.getOrRetrieveUser(userIdLong);
        String userDisplayName = userOpt.isPresent() ? userOpt.get().getEffectiveName() : "<@" + String.valueOf(userIdLong) + ">";

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Warez History for " + userDisplayName);

        if (events.isEmpty()) {
            eb.setDescription("No warez events to report!");
        } else {
            eb.setDescription("Showing ");

            for (WarezEventObject event : events) {
                if (eb.getFields().size() >= MessageEmbed.MAX_FIELD_AMOUNT) {
                    break;
                }

                OffsetDateTime dateTime = DateTimeUtils.longToOffsetDateTime(event.getTimestamp());
                String titleStr = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String bodyStr = String.format("Action: %s\n",
                    event.getAction().toString()
                );

                if (event.getMessageAttachmentCount().isPresent() && event.getMessageAttachmentCount().get() > 0) {
                    bodyStr += String.format("Message Attachments: %d\n",
                        event.getMessageAttachmentCount().orElse(0L)
                    );
                }

                if (event.getMessageAction().isPresent()) {
                    bodyStr += String.format("Message Last Action: %s\n",
                        event.getMessageAction().get()
                    );
                    bodyStr += String.format("```\n%s\n```",
                        event.getMessageContent().isPresent() ? StringUtils.abbreviate(event.getMessageContent().get(), 256) : "<no message content>"
                    );
                }
                
                eb.addField(titleStr, bodyStr, false);
            }

            eb.appendDescription(eb.getFields().size() + " of " + events.size() + " warez events (all times UTC)");
        }

        return eb.build();
    }
}
