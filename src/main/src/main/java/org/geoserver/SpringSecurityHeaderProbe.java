package org.geoserver;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class SpringSecurityHeaderProbe implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        String[] names = event.getApplicationContext()
                .getBeanNamesForType(org.springframework.security.web.header.HeaderWriterFilter.class);

        System.out.println("HeaderWriterFilter beans: " + java.util.Arrays.toString(names));

        for (String name : names) {
            Object bean = event.getApplicationContext().getBean(name);
            System.out.println(name + " -> " + bean);
        }
    }
}
