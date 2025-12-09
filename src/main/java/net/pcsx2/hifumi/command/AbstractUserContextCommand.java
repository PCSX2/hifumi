package net.pcsx2.hifumi.command;

import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class AbstractUserContextCommand {

    public void executeIfPermission(UserContextInteractionEvent event) {
        onExecute(event);
    }
    
    protected abstract void onExecute(UserContextInteractionEvent event);
    public void onButtonEvent(ButtonInteractionEvent event) { }
    public void onStringSelectEvent(StringSelectInteractionEvent event) { }
    protected abstract CommandData defineUserContextCommand();
}
