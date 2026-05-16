package net.pcsx2.hifumi.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;

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
                BufferedImage img = ImageIO.read(url);
                int width = img.getWidth() / 2, height = img.getHeight() / 2;
                Image scaled = img.getScaledInstance(width, height, Image.SCALE_FAST);
                BufferedImage bufImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graph = bufImg.createGraphics();
                graph.drawImage(scaled, 0, 0, null);
                graph.dispose();
                
                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    ImageIO.write(bufImg, "png", os);
                    
                    try (ByteArrayInputStream imgStream = new ByteArrayInputStream(os.toByteArray())) {
                        FileUpload file = FileUpload.fromData(imgStream, attachment.getFileName()).asSpoiler();
                        files.add(file);
                    }
                }
            } catch (Exception e) {
                // Squelch
            }
        }
        
        return files;
	}
}
