package com.act.json.model;

public class Event {

    String name;


    EventConfig eventConfig ;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public EventConfig getEventConfig() {
        return eventConfig;
    }

    public void setEventConfig(EventConfig eventConfig) {
        this.eventConfig = eventConfig;
    }

}
