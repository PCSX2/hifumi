package net.pcsx2.hifumi.parse;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.toMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.WordUtils;
import org.ini4j.Ini;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.config.SettingsIniParserConfig.Rule;
import net.pcsx2.hifumi.config.SettingsIniParserConfig.Section;
import net.pcsx2.hifumi.config.SettingsIniParserConfig.Setting;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;

public class SettingsIniParser extends AbstractParser {
    
    private static final String GLOBAL_SETTINGS_FILE_NAME = "PCSX2.ini";
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[A-Za-z]{4}-[0-9]{5}_[A-Za-z0-9]{8}.ini");

    public static void init(Message message) {
        int iniCounter = 0;

        for (Attachment att : message.getAttachments()) {
            if (iniCounter >= 2) {
                // Stop after the second file as not to spam the server
                break;
            }

            if (att.getFileName().equals(GLOBAL_SETTINGS_FILE_NAME)) {
                SettingsIniParser globalParser = new SettingsIniParser(message, att);
                HifumiBot.getSelf().getScheduler().runOnce(globalParser);
                iniCounter++;
                continue;
            }

            Matcher m = FILE_NAME_PATTERN.matcher(att.getFileName());
            
            if (m.matches()) {
                SettingsIniParser propertiesParser = new SettingsIniParser(message, att);
                HifumiBot.getSelf().getScheduler().runOnce(propertiesParser);
                iniCounter++;
            }
        }
    }
    
    private final Message message;
    private final Attachment attachment;

    private Map<String, Map<String, String>> ini;
    private HashMap<String, HashMap<String, ArrayList<String>>> errors;

    public SettingsIniParser(final Message message, final Attachment attachment) {
        this.message = message;
        this.attachment = attachment;
        this.errors = new HashMap<String, HashMap<String, ArrayList<String>>>();
    }

    public void run() {
        if (this.message == null || this.attachment == null) {
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
            Ini iniFile = new Ini(url);
            this.ini = iniFile.entrySet().stream().collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            this.safetyCheck();
            this.evaluate();
            this.displayErrors();
        } catch (IOException e) {
            Messaging.sendMessage(message.getChannel(), ":x: An I/O error occurred while processing " + attachment.getFileName());
        }
    }

    private void safetyCheck() {
        // Delete the original message if the ini contains an achievements token
        if (ini.containsKey("Achievements") && ini.get("Achievements").containsKey("Token")) {
            Messaging.sendPrivateMessage(this.message.getAuthor(), "The ini file you posted in the PCSX2 server contained your RetroAchievements login token inside. For your safety, your message was deleted so no one else can see it.");
            this.message.delete().queue();
        }
    }

    private void evaluate() {
        for (Section section : HifumiBot.getSelf().getSettingsIniParserConfig().sections) {
            // Prep a map of settings for this section
            HashMap<String, ArrayList<String>> errorSettings;

            if (errors.containsKey(section.sectionName)) {
                errorSettings = errors.get(section.sectionName);
            } else {
                errorSettings = new HashMap<String, ArrayList<String>>();
            }

            for (Setting setting : section.settings) {
                // Prep a list of errors for this setting 
                ArrayList<String> errorList;

                if (errorSettings.containsKey(setting.settingName)) {
                    errorList = errorSettings.get(setting.settingName);
                } else {
                    errorList = new ArrayList<String>();
                }

                for (Rule rule : setting.rules) {
                    // If the ini does not contain the section, skip the rule
                    if (!ini.containsKey(section.sectionName)) {
                        continue;
                    }

                    Map<String, String> iniSection = ini.get(section.sectionName);

                    // If the ini does not contain the setting, skip the rule 
                    if (!iniSection.containsKey(setting.settingName)) {
                        continue;
                    }

                    String value = iniSection.get(setting.settingName);
                    boolean valueInList = rule.expectedValues.contains(value);

                    // If the value is one the rule expected, or the rule is inverted and NOT in the list, continue.
                    if (valueInList || (!valueInList && rule.invert)) {
                        continue;
                    }

                    // If this rule doesn't apply to this type of settings file, continue
                    if (this.attachment.getFileName().equals(GLOBAL_SETTINGS_FILE_NAME)) {
                        if ((rule.settingsType & 1) != 1) {
                            continue;
                        }
                    } else {
                        if ((rule.settingsType & 2) != 2) {
                            continue;
                        }
                    }

                    // Else, add an error
                    String wrapped = WordUtils.wrap(rule.message, 75, "\n        ", true);
                    errorList.add(wrapped);
                }

                if (!errorList.isEmpty()) {
                    errorSettings.put(setting.settingName, errorList);
                }
            }

            if (!errorSettings.isEmpty()) {
                errors.put(section.sectionName, errorSettings);
            }
        }
    }

    private void displayErrors() {
        if (errors.isEmpty()) {
            Messaging.sendMessage(this.message.getChannel(), ":white_check_mark: Nothing to report! Your settings appear to be clean.", this.message, true);
            return;
        }

        StringBuilder sb = new StringBuilder("Settings analysis for " + this.attachment.getFileName() + "\n");
        sb.append("================================================================================").append("\n");

        for (String sectionName : errors.keySet()) {
            sb.append("[").append(sectionName).append("]").append("\n");
            HashMap<String, ArrayList<String>> settingErrors = errors.get(sectionName);

            for (String settingName : settingErrors.keySet()) {
                sb.append(settingName).append("\n");
                ArrayList<String> errorMessages = settingErrors.get(settingName);
                
                for (String message : errorMessages) {
                    sb.append("    ").append(message).append("\n");
                }
            }
        }
        
        sb.append("================================================================================").append("\n");

        String body = sb.toString();

        if (body.getBytes().length <= HifumiBot.getSelf().getJDA().getSelfUser().getAllowedFileSize()) {
            Messaging.sendMessage(this.message.getChannel(), ":information_source: Found something! Results are in this text file!", this.attachment.getFileName() + "_" + message.getAuthor().getName() + ".txt", body, null, null, this.message, true);
        } else {
            Messaging.sendMessage(this.message.getChannel(), ":warning: Your settings generated such a large results file that I can't upload it here. A human will have to read through your settings manually.", this.message, true);
        }
    }
}
