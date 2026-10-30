package com.epet36.biblioteca.event;

import org.springframework.context.ApplicationEvent;

public class DataSincronizadaEvent extends ApplicationEvent {
    public DataSincronizadaEvent(Object source) {
        super(source);
    }
}