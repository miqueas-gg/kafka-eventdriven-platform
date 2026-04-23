package com.kafkaeventdriven.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class EventSerializer {

   
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule()) 
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false) 
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); 

    public static String toJson(BaseEvent event) {
        try {
            return MAPPER.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando evento a JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializando JSON a evento", e);
        }
    }
}