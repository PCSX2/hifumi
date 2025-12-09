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
package net.pcsx2.hifumi.event;

import java.time.OffsetDateTime;
import java.util.Optional;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.WarezEventObject;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RoleEventListener extends ListenerAdapter {

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
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
                    Database.insertWarezEvent(warezEvent);    
                }
                
                return;
            }
        }
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
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
                    Database.insertWarezEvent(warezEvent);    
                }
                
                return;
            }
        }
    }
}
