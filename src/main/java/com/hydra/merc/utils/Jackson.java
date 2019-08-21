package com.hydra.merc.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created By aalamer on 08-20-2019
 */
public class Jackson {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static <T> T deserialize(String json, Class<T> type) throws IOException {
        return OBJECT_MAPPER.readValue(json, type);
    }

    public static String serialize(Object object) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(object);
    }
}
