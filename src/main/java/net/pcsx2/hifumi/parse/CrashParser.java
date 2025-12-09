package net.pcsx2.hifumi.parse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;

public class CrashParser extends AbstractParser {
    
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("crash-[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{3}.txt");

    private final Message message;
    private Attachment attachment;

    private ArrayList<String> errors;

    public CrashParser(final Message message) {
        this.message = message;

        for (Attachment att : message.getAttachments()) {
            Matcher m = FILE_NAME_PATTERN.matcher(att.getFileName());
            
            if (m.matches()) {
                this.attachment = att;
                break;
            }
        }
    }

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
            Messaging.sendMessage(message.getChannel(), ":hourglass: " + message.getAuthor().getAsMention() + " Checking your crash log for known problems...");
            String originalLine;
            String normalizedLine;
            this.errors = new ArrayList<String>();

            while ((originalLine = reader.readLine()) != null) {
                normalizedLine = originalLine.toLowerCase();
                
                if (normalizedLine.contains("fcvap32.dll") || normalizedLine.contains("fcvap64.dll") || normalizedLine.contains("ezfrd32.dll") || normalizedLine.contains("ezfrd64.dll") || normalizedLine.contains("ez6401.dll")) {
                    this.errors.add(
                        "[Fatal] Bad USB gamepad vibration driver. This driver is known to cause most 64 bit applications to crash without warning.\n" +
                        "Go to the file referenced below, and delete it from your PC:\n" +
                        normalizedLine
                    );
                }

                if (normalizedLine.contains("oldnewexplorer.dll") || normalizedLine.contains("oldnewexplorer32.dll") || normalizedLine.contains("oldnewexplorer64.dll")) {
                    this.errors.add(
                        "[Fatal] OldNewExplorer detected. OldNewExplorer causes many modern applications to crash, due to unsafe modifications to Windows' implementation of file and folder pickers.\n" +
                        "OldNewExplorer must be fully uninstalled and your system rebooted. PCSX2 will not function until the uninstall is fully complete and all Windows system files are restored to their proper states.\n" +
                        normalizedLine
                    );
                }

                if (normalizedLine.contains("vulkan_dzn.dll")) {
                    this.errors.add(
                        "[Fatal] Vulkan Compatibility Pack detected. This app package uses a Vulkan on DX12 implementation which does not actually conform to Vulkan specifications. Using the Vulkan renderer or opening the Settings menu in PCSX2 will likely crash.\n" +
                        "Open your Start menu, hit Settings, Apps, then find `OpenCL™, OpenGL®, and Vulkan® Compatibility Pack` in the list and uninstall. Once uninstalled, reboot your PC."
                    );
                }

                if (normalizedLine.contains("owclient.dll") || normalizedLine.contains("ow-graphics-vulkan.dll") || normalizedLine.contains("ow-graphics-hook64.dll")) {
                    this.errors.add(
                        "[Fatal] Overwolf detected. This program's overlay hooks often crash the GPU driver.\n" +
                        "To avoid crashing, uninstall Overwolf."
                    );
                }
            }

            reader.close();

            if (!errors.isEmpty()) {
                StringBuilder bodyBuilder = new StringBuilder();
                bodyBuilder.append("\n")
                        .append("============================= Crash Log Parse Results =============================")
                        .append("\n\n");

                for (String error : this.errors) {
                    bodyBuilder.append(error)
                            .append("\n\n")
                            .append("--------------------------------------------------------------------------------")
                            .append("\n\n");
                }

                bodyBuilder.append("\n\n")
                            .append("=========================== End Crash Log Parse Results ===========================")
                            .append("\n");
                
                if (bodyBuilder.toString().getBytes().length <= HifumiBot.getSelf().getJDA().getSelfUser().getAllowedFileSize()) {
                        Messaging.sendMessage(message.getChannel(), ":information_source: Found something! Results are in this text file!", "Crash_" + message.getAuthor().getName() + ".txt", bodyBuilder.toString());
                } else {
                    Messaging.sendMessage(message.getChannel(), ":warning: Your crash log generated such a large results file that I can't upload it. A human is gonna have to read through your log manually.");
                }
            } else {
                Messaging.sendMessage(message.getChannel(), ":white_check_mark: Crash log does not contain any DLL files known to be problematic.");
            }
        } catch (Exception e) {
            Messaging.sendMessage(message.getChannel(), ":x: Something went wrong... Try again?");
            Messaging.logException("CrashParser", "run", e);
            return;
        }
    }
}
