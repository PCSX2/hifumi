package net.pcsx2.hifumi.event;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.command.slash.CommandEmulog;
import net.pcsx2.hifumi.command.slash.CommandServerMetadata;
import net.pcsx2.hifumi.command.slash.CommandWhois;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.MemberUtils;
import net.pcsx2.hifumi.util.Messaging;
import net.pcsx2.hifumi.util.UserUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ButtonEventListener extends ListenerAdapter {

    private final HashMap<String, AbstractSlashCommand> slashCommands = HifumiBot.getSelf().getCommandIndex().getSlashCommands();
    
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        String[] parts = componentId.split(":");

        if (parts.length < 2) {
            Messaging.logInfo("ButtonEventListener", "onButtonInteraction", "Received a button click event, but got a malformed button ID. Received:\n```\n" + componentId + "\n```");
            event.reply("Something went wrong with this button. Admins have been notified.").setEphemeral(true).queue();
            return;
        }

        String reply = null;

        switch (parts[0]) {
            case "server-metadata":
                CommandServerMetadata commandServerMetadata = (CommandServerMetadata) slashCommands.get("server-metadata");
                event.deferEdit().queue();
                commandServerMetadata.handleButtonEvent(event);
                break;
            case "whois":
                CommandWhois commandWhois = (CommandWhois) slashCommands.get("whois");
                event.deferEdit().queue();
                commandWhois.handleButtonEvent(event);
                break;
            case "unwarez":
                event.deferEdit().queue();
                slashCommands.get("unwarez").handleButtonEvent(event);
                break;
            case "emulog_prev":
            case "emulog_next":
                CommandEmulog commandEmulog = (CommandEmulog) slashCommands.get("emulog");
                event.deferEdit().queue();
                commandEmulog.handleButtonEvent(event);
                break;
            case "timeout":
                try {
                    if (event.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
                        event.getGuild().retrieveMemberById(parts[1]).complete().timeoutFor(Duration.ofMinutes(60)).queue();
                        reply = "Member timed out successfully!";
                    } else {
                        reply = "You don't have permission to timeout members";
                    }    
                } catch (Exception e) {
                    reply = "An error occurred while attempting to timeout the member - are they still in the server?";
                    reply += "\nException message: " + e.getMessage();
                }

                event.reply(reply).queue();
                break;
            case "kick":
                try {
                    if (event.getMember().hasPermission(Permission.KICK_MEMBERS)) {
                        event.getGuild().retrieveMemberById(parts[1]).complete().kick().queue();
                        reply = "Member kicked successfully!";
                    } else {
                        reply = "You don't have permission to kick members";
                    }    
                } catch (Exception e) {
                    reply = "An error occurred while attempting to timeout the member - are they still in the server?";
                    reply += "\nException message: " + e.getMessage();
                }

                event.reply(reply).queue();
                break;
            case "ban":
                try {
                    if (event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
                        event.getGuild().retrieveMemberById(parts[1]).complete().ban(1, TimeUnit.HOURS).queue();
                        reply = "Member banned successfully!";
                    } else {
                        reply = "You don't have permission to ban members";
                    }    
                } catch (Exception e) {
                    reply = "An error occurred while attempting to ban the member - are they still in the server?";
                    reply += "\nException message: " + e.getMessage();
                }

                event.reply(reply).queue();
                break;
            case "imagescam":
                if (parts.length != 3) {
                    event.reply("Malformed button ID, please tell pandubz that `" + componentId + "` is garbage and he should feel bad.").setEphemeral(true).queue();
                    return;
                }

                Long userIdLong = Long.valueOf(parts[2]);
                Optional<User> userOpt = UserUtils.getOrRetrieveUser(userIdLong);

                if (userOpt.isEmpty()) {
                    event.reply("User ID referenced by the button could not be found (did the user already leave the server?)").setEphemeral(true).queue();
                    return;
                }

                switch (parts[1]) {
                    case "dospamkick" -> {
                        ModActions.kickAndNotifyUser(event.getGuild(), userIdLong);
                        event.reply("Messaged user telling them we think they are a bot, and kicked them from the server.").setComponents(new ArrayList<MessageTopLevelComponent>()).setEphemeral(true).queue();
                        Button button = Button.of(ButtonStyle.PRIMARY, "imagescam:resolved:" + userIdLong, "Resolved by " + event.getUser().getEffectiveName() + " (kicked user)");
                        ActionRow actionRow = ActionRow.of(button);
                        event.getHook().editMessageComponentsById(event.getMessageId(), actionRow).queue();
                    }
                    case "clear" -> {
                        Optional<Member> memberOpt = MemberUtils.getOrRetrieveMember(event.getGuild(), userIdLong);

                        if (memberOpt.isPresent()) {
                            Member member = memberOpt.get();
                            member.removeTimeout().queue();
                            event.reply("Timeout removed from user").setEphemeral(true).queue();
                            Button button = Button.of(ButtonStyle.PRIMARY, "imagescam:resolved:" + userIdLong, "Resolved by " + event.getUser().getEffectiveName() + " (removed timeout)");
                            ActionRow actionRow = ActionRow.of(button);
                            event.getHook().editMessageComponentsById(event.getMessageId(), actionRow).queue();
                        }
                    }
                    case "resolved" -> {
                        event.reply("This event has already been resolved.").setEphemeral(true).queue();
                    }
                    default -> {
                        event.reply("Invalid second component of button ID `" + parts[1] + "`, either you did something evil or I am breaking horribly.").setEphemeral(true).queue();
                    }
                }
                
                break;
        }
    }
}
