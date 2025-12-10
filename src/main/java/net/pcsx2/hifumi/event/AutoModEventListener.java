package net.pcsx2.hifumi.event;

import java.time.OffsetDateTime;
import java.util.Optional;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.async.AutoModReviewRunnable;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.permissions.PermissionLevel;
import net.pcsx2.hifumi.util.Log;
import net.pcsx2.hifumi.util.MemberUtils;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.automod.AutoModResponse;
import net.dv8tion.jda.api.events.automod.AutoModExecutionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AutoModEventListener extends ListenerAdapter {

    @Override
    public void onAutoModExecution(AutoModExecutionEvent event) {
        Log.info("AutoMod event");
        OffsetDateTime now = OffsetDateTime.now();
        Database.insertAutoModEvent(event, now);
        Log.info("AutoMod event inserted to db");

        // If user has elevated permissions, don't do review
        Optional<Member> memberOpt = MemberUtils.getOrRetrieveMember(event.getGuild(), event.getUserIdLong());
        Log.info("AutoMod event member retrieved");
        
        if (memberOpt.isPresent() && HifumiBot.getSelf().getPermissionManager().hasPermission(PermissionLevel.MOD, memberOpt.get())) {
            Log.info("AutoMod event subject elevated permissions abort");
            return;
        }

        // Start review of automod incidents
        if (event.getResponse().getType() == AutoModResponse.Type.BLOCK_MESSAGE) {
            Log.info("AutoMod event kicking off review runnable");
            HifumiBot.getSelf().getScheduler().runOnce(new AutoModReviewRunnable(event.getGuild(), event.getUserIdLong(), now));
        }
    }
}
