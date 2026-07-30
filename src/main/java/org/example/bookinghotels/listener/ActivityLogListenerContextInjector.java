package org.example.bookinghotels.listener;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ActivityLogListenerContextInjector implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        ActivityLogListener.setApplicationContext(applicationContext);
    }
}