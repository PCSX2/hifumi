// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.util;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Optional;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.utils.FileUpload;

public class AttachmentUtils {

    public static ArrayList<FileUpload> getMinifiedAttachments(Message message) {
        ArrayList<FileUpload> files = new ArrayList<FileUpload>();
        
        for (Attachment attachment : message.getAttachments()) {
            // For now, just do one image... If we have problems later and need them all, yank out this if.
            if (!files.isEmpty()) {
                break;
            }
            
            try {
                URL url = URL.of(URI.create(attachment.getProxyUrl()), null);
                
                try (BufferedInputStream stream = new BufferedInputStream(url.openStream())) {
                    FileUpload file = FileUpload.fromData(stream.readAllBytes(), attachment.getFileName()).asSpoiler();
                    files.add(file);
                }
            } catch (Exception e) {
                // Squelch
            }
        }
        
        return files;
    }
    
    public static Optional<String> generateImageSHA256(Attachment attachment) {
        try {
            URL url = URL.of(URI.create(attachment.getProxyUrl()), null);
            
            try (InputStream is = url.openStream()) {
                MessageDigest digest = MessageDigest.getInstance("SHA3-256");
                byte[] hashBytes = digest.digest(is.readAllBytes());
                StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
                
                for (int i = 0; i < hashBytes.length; i++) {
                    String hex = Integer.toHexString(0xff & hashBytes[i]);
                    
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    
                    hexString.append(hex);
                }
                
                return Optional.of(hexString.toString());
            }
        } catch (Exception e) {
            // Squelch
        }
        
        return Optional.empty();
    }
}
