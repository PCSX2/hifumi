// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.filter;

import java.awt.Color;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.Component.Type;
import net.dv8tion.jda.api.components.MessageTopLevelComponentUnion;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.entities.MessageReference.MessageReferenceType;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.Messaging;

public class AntiForwardHelper implements IFilterHelper {
    private final Message message;
    
    private String forwardedMessagePreview = "";
    private ArrayList<String> buttonRefs = new ArrayList<String>();
    
    public AntiForwardHelper(Message message) {
        this.message = message;
    }

    @Override
    public boolean run() {
        boolean isMaliciousForward = this.isMaliciousForward();
        
        if (isMaliciousForward) {
            User user = this.message.getAuthor();
            boolean timeoutRes = ModActions.timeoutAndNotifyUser(this.message.getGuild(), user.getIdLong());
            
            if (timeoutRes) {
                this.message.delete().queue();
                
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.YELLOW);
                eb.setTitle("Timed out user for forwarding message w/ button links");
                eb.setDescription(String.format("Forwarded message contained %d button link(s), and was possibly being used to conceal a scam link.", this.buttonRefs.size()));
                eb.addField("User (as mention)", user.getAsMention(), true);
                eb.addField("User ID", user.getId(), true);
                eb.addField("Forwarded Message (Truncated to 512 chars)", StringUtils.defaultIfBlank(this.forwardedMessagePreview, "<no content in message body>"), false);
                eb.addField("Button Links (Shortened to 60 chars each)", StringUtils.join(this.buttonRefs, ", "), false);
                
                MessageCreateBuilder mb = new MessageCreateBuilder();
                mb.addEmbeds(eb.build());
                mb.addComponents(ActionRow.of(
                    Button.of(
                            ButtonStyle.DANGER, 
                            "spamkick:execute:" + user.getId() + ":" + this.message.getId() + ":anti_forward",
                            "Looks like a bot scam, kick user"
                    ), 
                    Button.of(
                            ButtonStyle.SUCCESS, 
                            "spamkick:clear:" + user.getId() + ":" + this.message.getId() + ":anti_forward",
                            "Looks innocent, remove timeout"
                    )
                ));
                
                Messaging.sendMessage(HifumiBot.getSelf().getConfig().channels.systemOutputChannelId, mb.build());
            } else {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.RED);
                eb.setTitle("Failed to time out user for forwarding message w/ button links");
                eb.setDescription("Detected a potentially malicious forward, but was unable to issue a timeout command. Verify that the user still exists in the server and check if any further action is required.");
                eb.addField("User (as mention)", user.getAsMention(), true);
                eb.addField("User ID", user.getId(), true);
                eb.addField("Forwarded Message (Truncated to 512 chars)", StringUtils.defaultIfBlank(this.forwardedMessagePreview, "<no content in message body>"), false);
            }
        }
        
        return isMaliciousForward;
    }
    
    private boolean isMaliciousForward() {
        MessageReference ref = this.message.getMessageReference();
        
        if (ref != null && ref.getType() == MessageReferenceType.FORWARD) {
            Message forwardedMessage = ref.resolve().complete();
            
            for (MessageTopLevelComponentUnion component : forwardedMessage.getComponents()) {
                if (component.getType() == Type.ACTION_ROW) {
                    for (Button button : component.asActionRow().getButtons()) {
                        String url = button.getUrl();

                        if (url != null) {
                            this.buttonRefs.add(String.format("`%s`", StringUtils.abbreviate(url, 60)));
                        }
                    }
                }
            }
        }
        
        return !this.buttonRefs.isEmpty();
    }
}
