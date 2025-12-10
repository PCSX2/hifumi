package net.pcsx2.hifumi.event;

import net.pcsx2.hifumi.modal.BanHandler;
import net.pcsx2.hifumi.modal.DyncmdHandler;
import net.pcsx2.hifumi.modal.PromptHandler;
import net.pcsx2.hifumi.modal.SayHandler;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ModalEventListener extends ListenerAdapter {

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        switch (modalId) {
            case "say": {
                SayHandler.handle(event);
                break;
            }
            case "prompt": {
                PromptHandler.handle(event);
                break;
            }
            case "dyncmd": {
                DyncmdHandler.handle(event);
                break;
            }
            case "ban": {
                BanHandler.handle(event);
                break;
            }
            default: {
                Messaging.logInfo("ModalEventListener", "onModalInteraction", "Unexpected modal interaction event from user " + event.getUser().getAsMention() + " - are they a bot trying to mess with things?");
                break;
            }
        }
    }
}
