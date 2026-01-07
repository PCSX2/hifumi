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
package net.pcsx2.hifumi.util;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.pcsx2.hifumi.HifumiBot;

public class Messaging {
    
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("crash-[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{3}.txt");

    public static Message sendPrivateMessageEmbed(User user, MessageEmbed embed) {
        if (embed == null) {
            return null;
        }

        Message ret = null;
        
        try {
            PrivateChannel channel = user.openPrivateChannel().complete();
            ret = channel.sendMessageEmbeds(embed).complete();
        } catch (Exception e) {
            // Squelch
        }
         
        return ret;
    }

    public static Message sendPrivateMessage(User user, String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        
        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addContent(str);
        return sendPrivateMessage(user, mb.build());
    }
    
    public static Message sendPrivateMessage(User user, MessageCreateData msg) {
        if (msg == null) {
            return null;
        }
        
        Message ret = null;
        
        try {
            PrivateChannel channel = user.openPrivateChannel().complete();
            ret = channel.sendMessage(msg).complete();
        } catch (Exception e) {
            // Squelch
        }
         
        return ret;
    }

    public static Message sendMessage(String channelId, MessageCreateData msg) {
        return Messaging.sendMessage(HifumiBot.getSelf().getJDA().getTextChannelById(channelId), msg);
    }
    
    public static Message sendMessage(MessageChannel channel, String str) {
        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addContent(str);
        return Messaging.sendMessage(channel, mb.build(), null, null);
    }
    
    public static Message sendMessage(MessageChannel channel, String str, Message toReference, boolean pingReference) {
        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addContent(str);
        return Messaging.sendMessage(channel, mb.build(), null, null, null, null, toReference, pingReference);
    }

    public static Message sendMessage(MessageChannel channel, MessageCreateData msg) {
        return Messaging.sendMessage(channel, msg, null, null);
    }

    public static Message sendMessage(MessageChannel channel, String str, String fileName, String fileContents) {
        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addContent(str);
        return Messaging.sendMessage(channel, mb.build(), fileName, fileContents);
    }

    public static Message sendMessage(MessageChannel channel, MessageCreateData msg, String fileName, String fileContents) {
        return Messaging.sendMessage(channel, msg, fileName, fileContents, null, null);
    }
    
    public static Message sendMessage(MessageChannel channel, String str, String fileName, String fileContents, String linkLabel, String linkDestination) {
        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addContent(str);
        return Messaging.sendMessage(channel, mb.build(), fileName, fileContents, linkLabel, linkDestination); 
    }
    
    public static Message sendMessage(MessageChannel channel, MessageCreateData msg, String fileName, String fileContents, String linkLabel, String linkDestination) {
        return Messaging.sendMessage(channel, msg, fileName, fileContents, linkLabel, linkDestination, null, false);
    }
    
    public static Message sendMessage(MessageChannel channel, String str, String fileName, String fileContents, String linkLabel, String linkDestination, Message toReference, boolean pingReference) {
        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addContent(str);
        return Messaging.sendMessage(channel, mb.build(), fileName, fileContents, linkLabel, linkDestination, toReference, pingReference); 
    }
    
    public static Message sendMessage(MessageChannel channel, MessageCreateData msg, String fileName, String fileContents, String linkLabel, String linkDestination, Message toReference, boolean pingReference) {
        MessageCreateAction action = channel.sendMessage(msg);
        
        if (fileName != null && !fileName.isBlank() && fileContents != null && !fileContents.isBlank()) {
            FileUpload file = FileUpload.fromData(fileContents.getBytes(), fileName);
            action.addFiles(file);
        }
        
        if (linkLabel != null && !linkLabel.isBlank() && linkDestination != null && !linkDestination.isBlank()) {
            action.setComponents(ActionRow.of(
                Button.link(linkDestination, linkLabel)
            ));
        }
        
        if (toReference != null) {
            action.setMessageReference(toReference);
            action.mentionRepliedUser(pingReference);
        }
        
        return action.complete();
    }

    public static Message sendMessageEmbed(String channelId, MessageEmbed embed) {
        return Messaging.sendMessageEmbed(HifumiBot.getSelf().getJDA().getTextChannelById(channelId), embed);
    }

    public static Message sendMessageEmbed(MessageChannel channel, MessageEmbed embed) {
        return channel.sendMessageEmbeds(embed).complete();
    }

    public static Message editMessageEmbed(Message msg, MessageEmbed embed) {
        return msg.editMessageEmbeds(embed).complete();
    }

    public static void logInfo(String className, String methodName, String msg) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Info from " + className + "." + methodName);
        eb.addField("Message", msg, false);
        Messaging.sendMessageEmbed(HifumiBot.getSelf().getConfig().channels.systemOutputChannelId, eb.build());
    }

    public static void logInfoMessage(MessageCreateData msg) {
        Messaging.sendMessage(HifumiBot.getSelf().getConfig().channels.systemOutputChannelId, msg);
    }

    public static void logInfoEmbed(MessageEmbed embed) {
        Messaging.sendMessageEmbed(HifumiBot.getSelf().getConfig().channels.systemOutputChannelId, embed);
    }

    public static void logException(Throwable e) {
        logException(null, null, e);
    }

    public static void logException(String className, String methodName, Throwable e) {
        if (HifumiBot.getSelf().getJDA() == null || HifumiBot.getSelf().getConfig() == null || HifumiBot.getSelf().getConfig().channels.systemOutputChannelId.isEmpty()) {
            e.printStackTrace();
            return;
        }

        String messageContent = "(exception was null)";
        if (e != null) {
            messageContent = e.getMessage();
            if (messageContent == null) {
                messageContent = "(message was null)";
            }
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.RED);
        
        if (className != null && methodName != null) {
            eb.setTitle("Exception caught in " + className + "." + methodName);
        } else {
            eb.setTitle("Exception caught");
        }
        
        eb.addField("Message", messageContent, false);
        StringBuilder sb = new StringBuilder();

        for (StackTraceElement ste : e.getStackTrace()) {
            sb.append(ste.toString()).append("\n");
        }

        eb.addField("Stack Trace", StringUtils.abbreviate(sb.toString(), 1024), false);

        if (e.getCause() != null) {
            String causeContent = e.getCause().getMessage();
            
            if (causeContent == null) {
                causeContent = "(message was null)";
            }
            
            eb.addField("Caused By", causeContent, false);
            sb = new StringBuilder();

            for (StackTraceElement ste : e.getCause().getStackTrace()) {
                sb.append(ste.toString()).append("\n");
            }

            eb.addField("Caused By Stack Trace", StringUtils.abbreviate(sb.toString(), 1024), false);
        }

        Messaging.sendMessageEmbed(HifumiBot.getSelf().getConfig().channels.systemOutputChannelId, eb.build());
    }

    public static boolean hasEmulog(Message msg) {
        for (Attachment attachment : msg.getAttachments()) {
            if (attachment.getFileName().equalsIgnoreCase("emulog.txt")) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasPnach(Message msg) {
        for (Attachment attachment : msg.getAttachments()) {
            if (attachment.getFileExtension().equalsIgnoreCase("pnach")) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasCrashLog(Message msg) {
        for (Attachment attachment : msg.getAttachments()) {
            Matcher m = FILE_NAME_PATTERN.matcher(attachment.getFileName());

            if (m.matches()) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasIni(Message msg) {
        for (Attachment attachment : msg.getAttachments()) {
            if (attachment.getFileExtension().equalsIgnoreCase("ini")) {
                return true;
            }
        }

        return false;
    }
    
    public static boolean hasBotPing(Message msg) {
        if (msg == null) {
            return false;
        }
        
        for (User usr : msg.getMentions().getUsers()) {
            if (usr.getId().equals(HifumiBot.getSelf().getJDA().getSelfUser().getId())) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean hasBotReply(Message msg) {
        if (msg == null) {
            return false;
        }
        
        Message ref = msg.getReferencedMessage();
        
        if (ref != null) {
            if (ref.getAuthor().getId().equals(HifumiBot.getSelf().getJDA().getSelfUser().getId())) {
                return true;
            }
        }
        
        return false;
    }

    public static boolean hasGhostPing(Message msg) {
        if (msg == null) {
            return false;
        }
        
        for (User usr : msg.getMentions().getUsers()) {
            try {
                if (msg.getGuild().retrieveMemberById(usr.getId()).complete() == null) {
                    return true;
                }
            } catch (ErrorResponseException e) {
                return true;
            }
        }
        
        return false;
    }
}
