package com.act.json.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Config {


    private List<Event> events;

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }
    public static void main(String arg[]){


        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();

        Config config = new Config();

        Event event = new Event();
        config.setEvents(new ArrayList<>());
        {
            config.getEvents().add(event);
            event.setName("Non Client Billable Hrs Payment (Event-3)");
            event.setEventConfig(new EventConfig());
            event.getEventConfig().setValidFrom(LocalDate.now());
            event.getEventConfig().setValidTo(LocalDate.now());
            event.getEventConfig().setEventAction(new ArrayList<>());
            EventAction eventAction = new EventAction();
            event.getEventConfig().getEventAction().add(eventAction);
            eventAction.setFromLedgerName("Cash/Bank");
            eventAction.setToLedgerName("Manikandan Employee - AP");
            eventAction.setType("source");
            eventAction.setAmountRatePerHour(BigDecimal.ZERO);
        }

        {
            config.getEvents().add(event);
            event.setName("Release Payment");
            event.setEventConfig(new EventConfig());
            event.getEventConfig().setValidFrom(LocalDate.now());
            event.getEventConfig().setValidTo(LocalDate.now());
            event.getEventConfig().setEventAction(new ArrayList<>());
            EventAction eventAction = new EventAction();
            event.getEventConfig().getEventAction().add(eventAction);
            eventAction.setFromLedgerName("Manikandan Employee - AP");
            eventAction.setToLedgerName("Manikandan Employee");
            eventAction.setType("source");
            eventAction.setAmountRatePerHour(BigDecimal.ZERO);
        }

        String json = gson.toJson(config);
        System.out.println(json);

    }
}
