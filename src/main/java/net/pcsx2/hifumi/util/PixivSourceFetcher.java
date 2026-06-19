// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.util;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.pcsx2.hifumi.HifumiBot;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

public class PixivSourceFetcher {

    private static final String PIXIV_BASE_URL = "https://www.pixiv.net/artworks/";
    private static final String[] PIXIV_PATTERN = { ".*illust_([0-9]+)_.+", "[a-zA-Z_]*([0-9]+)_p[0-9]+.+" };
    private static final Pattern[] p = { Pattern.compile(PIXIV_PATTERN[0]), Pattern.compile(PIXIV_PATTERN[1]) };
    
    public static void getPixivLink(Message message) {
        if (!message.getChannel().getId().equals(HifumiBot.getSelf().getConfig().channels.pixivChannelId)) {
            return;
        }
        
        ArrayList<String> imageUrls = new ArrayList<String>();
        MessageCreateBuilder mb = new MessageCreateBuilder();
        
        for (Attachment attach : message.getAttachments()) {
            int pos = 0;
            Matcher m = p[pos].matcher(attach.getFileName());
            
            while (!m.matches()) {
                if (++pos >= p.length) {
                    return;
                }
                
                m = p[pos].matcher(attach.getFileName());
            }
            
            try {
                URL url = new URI(PIXIV_BASE_URL + m.group(1)).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                
                if (connection.getResponseCode() == 200) {
                    imageUrls.add(PIXIV_BASE_URL + m.group(1));
                    break;
                }
            } catch (Exception e) { }
        }
        
        for (int i = 0; i < imageUrls.size(); i++) {
            if (message.getContentDisplay().contains(imageUrls.get(i))) {
                imageUrls.remove(i--);
            }
        }
        
        if (!imageUrls.isEmpty()) {
            mb.addContent("Found the sauce! ");
            
            if (imageUrls.size() > 1) {
                mb.addContent("(button order matches image order)");
            }
            
            ArrayList<Button> buttons = new ArrayList<Button>();
            
            for (String imageUrl : imageUrls) {
                if (buttons.size() < 5) {
                    buttons.add(Button.link(imageUrl, "Go to Pixiv"));
                } else {
                    mb.addContent(" (max button count reached, other images will not be linked)");
                    break;
                }
            }
            
            mb.addComponents(ActionRow.of(buttons));
            message.reply(mb.build()).mentionRepliedUser(false).queue();
        }
    }
}
