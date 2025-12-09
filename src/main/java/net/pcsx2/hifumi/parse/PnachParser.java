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
package net.pcsx2.hifumi.parse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;

public class PnachParser extends AbstractParser {

    private static final String CRC_FILE_NAME_PATTERN = "[A-Z]{4}-[0-9]{5}_[0-9a-fA-F]{8}.*\\.pnach";
    private static final List<String> KEYWORDS = Arrays.asList("author", "description", "gametitle", "comment", "gsaspectratio");

    private final Message message;
    private Attachment attachment;

    private HashMap<PnachParserError, ArrayList<String>> errorMap;

    public PnachParser(final Message message) {
        this.message = message;

        for (Attachment att : message.getAttachments()) {
            if (att.getFileExtension().equalsIgnoreCase("pnach")) {
                this.attachment = att;
                break;
            }
        }

        this.errorMap = new HashMap<PnachParserError, ArrayList<String>>();

        for (PnachParserError ppe : PnachParserError.values()) {
            this.errorMap.put(ppe, new ArrayList<String>());
        }
    }

    @Override
    public void run() {
        URL url = null;

        try {
            url = new URI(attachment.getUrl()).toURL();
        } catch (Exception e) {
            Messaging.sendMessage(message.getChannel(), ":x: The URL to your attachment was bad... Try uploading again or changing the file name?");
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            Messaging.sendMessage(message.getChannel(), ":hourglass: " + message.getAuthor().getAsMention() + " Testing your PNACH ( " + attachment.getFileName() + " )");
            Pattern p = Pattern.compile(CRC_FILE_NAME_PATTERN, Pattern.CASE_INSENSITIVE);

            if (!p.matcher(attachment.getFileName()).matches()) {
                addError(PnachParserError.FILE_NAME, "(filename)");
            }

            String line;

            while ((line = reader.readLine()) != null) {
                // Drop any comments
                line = line.replaceAll("//.*", "");

                // Test the start of the line. Is it one of the accepted tags, or blank?
                if (line.isBlank()) {
                    continue;
                } else if ((line.startsWith("["))) {
                    if (!line.endsWith("]")) {
                        addError(PnachParserError.BAD_BRACKET, line);
                    }

                    if (line.contains("[ ") || line.contains(" ]")) {
                        addError(PnachParserError.BRACKET_WHITESPACE, line);
                    }
                } else if (line.contains("=")) {
                    int firstEquals = line.indexOf('=');
                    String lineStart = line.substring(0, firstEquals).toLowerCase();

                    if (KEYWORDS.contains(lineStart)) {
                        continue;
                    } else if (lineStart.equals("patch")) {
                        int lastEquals = line.lastIndexOf('=');

                        if (firstEquals == lastEquals) {
                            try {
                                int firstInlineComment = line.contains("//") ? line.indexOf("//") : -1;
                                String paramStr = line.substring(firstEquals + 1);

                                if (firstInlineComment != -1) {
                                    paramStr = paramStr.substring(0, firstInlineComment);
                                }

                                String[] params = paramStr.split(",");

                                if (params.length == 5) {
                                    // Param 0
                                    try {
                                        int mode = Integer.parseInt(params[0]);

                                        if (mode < 0 || mode > 2) {
                                            addError(PnachParserError.FIRST_RANGE, line);
                                        }
                                    } catch (NumberFormatException e) {
                                        addError(PnachParserError.FIRST_NAN, line);
                                    }

                                    // Param 1
                                    if (params[1].equals("EE") || params[1].equals("IOP")) {
                                        // Do nothing
                                    } else if (params[1].equalsIgnoreCase("EE") || params[1].equalsIgnoreCase("IOP")) {
                                        addError(PnachParserError.SECOND_CAPS, line);
                                    } else {
                                        addError(PnachParserError.SECOND_CPU, line);
                                    }

                                    // Param 2
                                    Integer addr = -1;
                                    Integer leading = -1;
                                    try {
                                        addr = Integer.parseUnsignedInt(params[2], 16);
                                        leading = (addr & 0xf0000000) >> 28;

                                        if (params[3].equals("extended")) {
                                            if (leading < 0 || leading > 2) {
                                                addError(PnachParserError.THIRD_LEAD_UNCHECKED, line);
                                            }
                                        } else {
                                            if (leading != 0) {
                                                addError(PnachParserError.THIRD_LEAD_NOT_ALLOWED, line);
                                            } else if (addr >= 0x02000000) {
                                                addError(PnachParserError.THIRD_RANGE, line);
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                        addError(PnachParserError.THIRD_ADDRESS, line);
                                    }
                                    // Param 3
                                    if (params[3].equals("byte") || params[3].equals("short")
                                            || params[3].equals("word") || params[3].equals("double")
                                            || params[3].equals("extended")) {
                                        // do nothing
                                    } else if (params[3].equalsIgnoreCase("byte") || params[3].equalsIgnoreCase("short")
                                            || params[3].equalsIgnoreCase("word")
                                            || params[3].equalsIgnoreCase("double")
                                            || params[3].equalsIgnoreCase("extended")) {
                                        addError(PnachParserError.FOURTH_LOWERCASE, line);
                                    } else {
                                        addError(PnachParserError.FOURTH_TYPE, line);
                                    }
                                    // Param 4
                                    try {
                                        String param4 = params[4].split("/")[0].trim();
                                        Integer value = Integer.parseUnsignedInt(param4.toUpperCase(), 16);

                                        if (params[3].equals("byte") || (params[3].equals("extended") && leading == 0)) {
                                            if (Integer.compareUnsigned(value, 0xff) > 0) {
                                                addError(PnachParserError.FIFTH_SCOPE, line);
                                            }
                                        } else if (params[3].equals("short") || (params[3].equals("extended") && leading == 1)) {
                                            if (Integer.compareUnsigned(value, 0xffff) > 0) {
                                                addError(PnachParserError.FIFTH_SCOPE, line);
                                            }
                                        } else if (params[3].equals("word") || (params[3].equals("extended") && leading == 2)) {
                                            // Nothing to report on
                                        } else if (params[3].equals("double")) {
                                            // Nothing to report on
                                        }
                                    } catch (NumberFormatException e) {
                                        addError(PnachParserError.FIFTH_VALUE, line);
                                    }
                                } else {
                                    addError(PnachParserError.PARAM_COUNT, line);
                                }
                            } catch (IndexOutOfBoundsException e) {
                                addError(PnachParserError.MISSING_RIGHT, line);
                            }
                        } else {
                            addError(PnachParserError.SECOND_EQUALS, line);
                        }
                    } else {
                        addError(PnachParserError.START_KEYWORD, line);
                    }
                } else {
                    addError(PnachParserError.NO_EQUALS, line);
                }
            }

            reader.close();

            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("\n")
                    .append("============================== Pnach Parse Results =============================")
                    .append("\n")
                    .append("(*) = Information (!) = Warning (X) = Critical").append("\n\n");
            boolean hasLines = false;

            for (PnachParserError epe : errorMap.keySet()) {
                ArrayList<String> lines = errorMap.get(epe);

                if (lines.size() > 0) {
                    hasLines = true;
                    bodyBuilder.append("--------------------------------------------------------------------------------")
                            .append("\n")
                            .append(epe.getDisplayString()).append("\n\n");

                    for (String str : lines) {
                        bodyBuilder.append(str).append("\n");
                    }
                }
            }

            if (hasLines) {
                bodyBuilder.append("\n\n")
                        .append("============================ End Pnach Parse Results ===========================")
                        .append("\n");

                if (bodyBuilder.toString().getBytes().length <= HifumiBot.getSelf().getJDA().getSelfUser().getAllowedFileSize()) {
                    Messaging.sendMessage(message.getChannel(), ":information_source: Found something! Results are in this text file!", "Pnach_" + message.getAuthor().getName() + ".txt", bodyBuilder.toString());
                } else {
                    Messaging.sendMessage(message.getChannel(), ":warning: Your pnach generated such a large results file that I can't upload it. A human is gonna have to read through your pnach manually.");
                }
            } else {
                Messaging.sendMessage(message.getChannel(), ":white_check_mark: All good, nothing to report!");
            }
        } catch (IOException e) {
            Messaging.sendMessage(message.getChannel(), ":x: Something went wrong... Try again?");
            Messaging.logException("EmulogParser", "run", e);
            return;
        }
    }

    private void addError(PnachParserError ppe, String line) {
        ArrayList<String> lines = errorMap.get(ppe);
        lines.add(line);
        errorMap.put(ppe, lines);
    }
}
