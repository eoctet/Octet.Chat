package chat.octet.api.handler;


import chat.octet.api.CharacterModelBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class CleanupResourceListener implements ApplicationListener<ContextClosedEvent> {
    @Override
    public void onApplicationEvent(@NotNull ContextClosedEvent event) {
        CharacterModelBuilder.getInstance().close();
    }
}
