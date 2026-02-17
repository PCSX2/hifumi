package net.pcsx2.hifumi.async;

import java.awt.Color;

import org.apache.commons.lang3.StringUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.entities.MessageReference.MessageReferenceType;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.util.Messaging;

public class MaliciousForwardRunnable implements Runnable
{
	private final Message message;
	
	public MaliciousForwardRunnable(Message message)
	{
		this.message = message;
	}

	@Override
	public void run()
	{
		MessageReference ref = this.message.getMessageReference();
		
		if (ref != null && ref.getType() == MessageReferenceType.FORWARD)
		{
			Message forwardedMessage = ref.resolve().complete();
			int componentCount = forwardedMessage.getComponents().size();
			
			if (componentCount != 0)
			{
				this.message.delete().queue();
				User user = this.message.getAuthor();
				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.YELLOW);
				eb.setTitle("Removed forwarded message containing interaction elements");
				eb.setDescription(String.format("Forwarded message contained %d components, and was possibly being used to conceal a scam link.", componentCount));
				eb.addField("User (as mention)", user.getAsMention(), true);
				eb.addField("User ID", user.getId(), true);
				eb.addField("Forwarded Message (Truncated to 512 chars)", StringUtils.abbreviate(forwardedMessage.getContentDisplay(), 512), false);
				MessageCreateBuilder mb = new MessageCreateBuilder();
				mb.addEmbeds(eb.build());
				Messaging.sendMessage(HifumiBot.getSelf().getConfig().channels.systemOutputChannelId, mb.build());
				Messaging.sendPrivateMessage(user, "Forwarding messages which contain buttons or other interactive items is not allowed for security reasons.");
			}
		}
		
	}
}
