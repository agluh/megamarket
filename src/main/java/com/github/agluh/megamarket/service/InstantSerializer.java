package com.github.agluh.megamarket.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * Helper serializer for dates.
 */
public class InstantSerializer extends JsonSerializer<Instant> {

    public static DateTimeFormatter ISO8601_DATE_TIME =
        new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider arg2)
            throws IOException {
        gen.writeString(ISO8601_DATE_TIME.format(value));
    }
}
