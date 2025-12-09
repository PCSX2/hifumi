package net.pcsx2.hifumi.command.context;

import java.util.Optional;

import net.pcsx2.hifumi.command.AbstractMessageContextCommand;
import net.pcsx2.hifumi.util.MemberUtils;
import net.pcsx2.hifumi.util.WarezUtil;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandWarezMsg extends AbstractMessageContextCommand {

    @Override
    protected void onExecute(MessageContextInteractionEvent event) {
        event.deferReply().queue();

        Message msg = event.getTarget();
        User user = msg.getAuthor();
        Optional<Member> memberOpt = MemberUtils.getOrRetrieveMember(event.getGuild(), user.getIdLong());
        Optional<Message> messageOpt = Optional.of(msg);

        WarezUtil.applyWarez(event, user, memberOpt, messageOpt);
    }

    @Override
    protected CommandData defineMessageContextCommand() {
        return Commands.message("warez-msg")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES));
    }
}
