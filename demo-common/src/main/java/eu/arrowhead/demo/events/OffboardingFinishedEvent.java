package eu.arrowhead.demo.events;

import org.springframework.context.ApplicationEvent;

public class OffboardingFinishedEvent extends ApplicationEvent {

    public OffboardingFinishedEvent(final Object source) {
        super(source);
    }
}
