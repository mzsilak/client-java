package eu.arrowhead.demo.events;

import org.springframework.context.ApplicationEvent;

public class OnboardingFinishedEvent extends ApplicationEvent {

    public OnboardingFinishedEvent(final Object source) {
        super(source);
    }
}
