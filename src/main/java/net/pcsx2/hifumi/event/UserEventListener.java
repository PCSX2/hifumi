// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.event;

import net.dv8tion.jda.api.events.user.update.UserUpdateGlobalNameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;

public class UserEventListener extends ListenerAdapter {

    @Override 
    public void onUserUpdateName(UserUpdateNameEvent event) {
        HifumiBot.getSelf().getScheduler().runOnce(() -> {
            Database.insertUsernameChangeEvent(event);
        });
    }

    @Override
    public void onUserUpdateGlobalName(UserUpdateGlobalNameEvent event) {
        HifumiBot.getSelf().getScheduler().runOnce(() -> {
            Database.insertDisplayNameChangeEvent(event);
        });
    }
}
