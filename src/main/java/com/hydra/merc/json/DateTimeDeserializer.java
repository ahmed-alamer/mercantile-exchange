package com.hydra.merc.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;

/**
 * Created By aalamer on 08-11-2019
 */
public class DateTimeDeserializer extends JsonDeserializer<DateTime> {
    @Override
    public DateTime deserialize(JsonParser parser,
                                DeserializationContext context) throws IOException, JsonProcessingException {
        return new DateTime(parser.readValueAs(Long.class), DateTimeZone.UTC);
    }
}
