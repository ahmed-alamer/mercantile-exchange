package com.hydra.merc.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;

/**
 * Created By aalamer on 08-11-2019
 */
public class DateTimeSerializer extends JsonSerializer<DateTime> {
    @Override
    public void serialize(DateTime value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        generator.writeNumber(new DateTime(value, DateTimeZone.UTC).getMillis());
    }
}
