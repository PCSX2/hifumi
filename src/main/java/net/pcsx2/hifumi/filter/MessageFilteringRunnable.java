package net.pcsx2.hifumi.filter;

import java.util.List;
import java.util.Optional;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.pcsx2.hifumi.util.MemberUtils;

public class MessageFilteringRunnable implements Runnable {
    private final Message message;
    
    public MessageFilteringRunnable(Message message) {
        this.message = message;
    }
    
    @Override
    public void run() {
        Optional<Member> memberOpt = MemberUtils.getOrRetrieveMember(this.message.getGuild(), this.message.getAuthor().getIdLong());
        
        if (memberOpt.isEmpty()) {
            return;
        }
        
        Member member = memberOpt.get();
        
        if (member.isTimedOut()) {
            return;
        }
        
        List<IFilterHelper> helpers = List.of(
            new HoneypotHelper(this.message),
            new ScamHashHelper(this.message),
            new AntiSpamHelper(this.message),
            //new AntiForwardHelper(this.message),
            new AntiBotHelper(this.message)
        );
        
        for (IFilterHelper helper : helpers) {
            if (helper.run()) {
                break;
            }
        }
    }
}
