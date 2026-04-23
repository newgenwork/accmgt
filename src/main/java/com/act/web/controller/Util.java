package com.act.web.controller;

import com.act.json.model.Config;
import com.act.json.model.Event;
import com.act.json.model.LocalDateAdapter;
import com.act.model.Ledger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDate;
import java.util.Iterator;

public class Util {


    public static Event getConfigEvent(Ledger ledger, String type) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();

        Config config = gson.fromJson(ledger.getConfig(), Config.class);
        Event toApply = null;
        if (config.getEvents()!=null) {
            Iterator<Event> it = config.getEvents().iterator();


            while (it.hasNext()) {
                Event event = it.next();
                if (event.getName().equals(type)) {
                    LocalDate today = LocalDate.now();   // Current date
                    boolean isBetween = (!today.isBefore(event.getEventConfig().getValidFrom()))
                            && (!today.isAfter(event.getEventConfig().getValidTo()));
                    if (isBetween) {
                        toApply = event;
                        break;
                    }
                }
            }
        }
        return toApply;
    }

}
