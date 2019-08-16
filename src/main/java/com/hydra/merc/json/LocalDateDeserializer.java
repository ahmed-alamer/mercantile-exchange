package com.hydra.merc.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.joda.time.LocalDate;

import java.io.IOException;


/**
 * Created By aalamer on 08-11-2019
 */
public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser parser,
                                 DeserializationContext context) throws IOException, JsonProcessingException {
        return LocalDate.parse(parser.readValueAs(String.class));
    }
}
