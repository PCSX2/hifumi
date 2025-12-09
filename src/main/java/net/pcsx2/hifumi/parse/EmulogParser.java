/**
809 * This file is part of HifumiBot, licensed under the MIT License (MIT)
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
package net.pcsx2.hifumi.parse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.config.EmulogParserConfig.Rule;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;

public class EmulogParser extends AbstractParser {

    private final Message message;
    private Attachment attachment;

    private HashMap<Rule, Pattern> patterns;
    private HashMap<Rule, ArrayList<String>> lines;

    public EmulogParser(final Message message) {
        this.message = message;

        for (Attachment att : message.getAttachments()) {
            if (att.getFileName().equalsIgnoreCase("emulog.txt")) {
                this.attachment = att;
                break;
            }
        }

        patterns = new HashMap<Rule, Pattern>();
        lines = new HashMap<Rule, ArrayList<String>>();
        
        for (Rule rule : HifumiBot.getSelf().getEmulogParserConfig().rules) {
            patterns.put(rule, Pattern.compile(rule.toMatch.toLowerCase()));
            lines.put(rule, new ArrayList<String>());
        }
    }

    @Override
    public void run() {
        if (attachment == null) {
            return;
        }

        URL url = null;

        try {
            url = new URI(attachment.getUrl()).toURL();
        } catch (Exception e) {
            Messaging.sendMessage(message.getChannel(), ":x: The URL to your attachment was bad... Try uploading again or changing the file name?");
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            Messaging.sendMessage(message.getChannel(), ":hourglass: " + message.getAuthor().getAsMention() + " Checking your emulog.txt for information/errors...");
            String originalLine;
            String normalizedLine;

            while ((originalLine = reader.readLine()) != null) {
                normalizedLine = originalLine.toLowerCase();
                
                for (Rule rule : patterns.keySet()) {
                    Pattern p = patterns.get(rule);
                    Matcher m = p.matcher(normalizedLine);
                    
                    if (m.matches()) {
                        addError(rule, originalLine);
                    }
                }
            }

            reader.close();

            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("\n")
                    .append("============================= Emulog Parse Results =============================")
                    .append("\n");
            bodyBuilder.append("(*) = Information (!) = Warning (X) = Critical").append("\n\n");
            boolean hasLines = false;

            for (Rule rule : lines.keySet()) {
                ArrayList<String> arr = lines.get(rule);

                if (arr.size() > 0) {
                    hasLines = true;
                    bodyBuilder
                            .append("--------------------------------------------------------------------------------")
                            .append("\n");
                    
                    switch (rule.severity) {
                    case 0:
                        bodyBuilder.append("(*) ");
                        break;
                    case 1:
                        bodyBuilder.append("(!) ");
                        break;
                    case 2:
                        bodyBuilder.append("(X) ");
                        break;
                    default:
                        bodyBuilder.append("(?) ");
                        break;
                    }
                    
                    bodyBuilder.append(rule.message).append("\n\n");

                    for (String str : arr) {
                        bodyBuilder.append(str).append("\n");
                    }
                }
            }

            if (hasLines) {
                bodyBuilder.append("\n\n")
                        .append("=========================== End Emulog Parse Results ===========================")
                        .append("\n");

                if (bodyBuilder.toString().getBytes().length <= HifumiBot.getSelf().getJDA().getSelfUser().getAllowedFileSize()) {
                    Messaging.sendMessage(message.getChannel(), ":information_source: Found something! Results are in this text file!", "Emulog_" + message.getAuthor().getName() + ".txt", bodyBuilder.toString());
                } else {
                    Messaging.sendMessage(message.getChannel(), ":warning: Your emulog generated such a large results file that I can't upload it. A human is gonna have to read through your log manually.");
                }
            } else {
                Messaging.sendMessage(message.getChannel(), ":white_check_mark: Nothing to report! Either this emulog is empty, or things just went really well!");
            }
        } catch (Exception e) {
            Messaging.sendMessage(message.getChannel(), ":x: Something went wrong... Try again?");
            Messaging.logException("EmulogParser", "run", e);
            return;
        }
    }

    private void addError(Rule rule, String line) {
        ArrayList<String> arr = lines.get(rule);
        arr.add(line);
        lines.put(rule, arr);
    }
}
