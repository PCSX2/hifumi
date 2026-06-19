// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.event;

import java.time.OffsetDateTime;
import java.util.Optional;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.WarezEventObject;

public class RoleEventListener extends ListenerAdapter {

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        HifumiBot.getSelf().getScheduler().runOnce(() -> {
            OffsetDateTime now = OffsetDateTime.now();

            for (Role role : event.getRoles()) {
                if (role.getId().equals(HifumiBot.getSelf().getConfig().roles.warezRoleId)) {
                    // Check the latest warez event for the user
                    Optional<WarezEventObject> lastWarezOpt = Database.getLatestWarezAction(event.getUser().getIdLong());

                    // If the warez role was given manually, there will not be a corresponding event in the database yet.
                    // If it was given using the command, then there will be.
                    // Check if the user has never been warez'd, or if their last event was a removal.
                    // If either are true, then this was done manually and should be stored.
                    // Else, it was a command usage and should not be stored again.
                    if (lastWarezOpt.isEmpty() || lastWarezOpt.get().getAction().equals(WarezEventObject.Action.REMOVE)) {
                        WarezEventObject warezEvent = new WarezEventObject(
                            now.toEpochSecond(), 
                            event.getUser().getIdLong(), 
                            WarezEventObject.Action.ADD,
                            null
                        );
                        Database.insertWarezEvent(warezEvent, event.getUser());
                    }
                    
                    return;
                }
            }
        });
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        HifumiBot.getSelf().getScheduler().runOnce(() -> {
            OffsetDateTime now = OffsetDateTime.now();

            for (Role role : event.getRoles()) {
                if (role.getId().equals(HifumiBot.getSelf().getConfig().roles.warezRoleId)) {
                    // Check the latest warez event for the user
                    Optional<WarezEventObject> lastWarezOpt = Database.getLatestWarezAction(event.getUser().getIdLong());

                    // Same criteria as above, just this time for the removal.
                    if (lastWarezOpt.isEmpty() || lastWarezOpt.get().getAction().equals(WarezEventObject.Action.ADD)) {
                        WarezEventObject warezEvent = new WarezEventObject(
                            now.toEpochSecond(), 
                            event.getUser().getIdLong(), 
                            WarezEventObject.Action.REMOVE,
                            null
                        );
                        Database.insertWarezEvent(warezEvent, event.getUser());
                    }
                    
                    return;
                }
            }
        });
    }
}
