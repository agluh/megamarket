package com.github.agluh.megamarket.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.agluh.megamarket.dto.ShopUnit;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * Serializer of a catalog element.
 */
public class ShopUnitSerializer extends JsonSerializer<ShopUnit> {

    public static DateTimeFormatter ISO8601_DATE_TIME =
        new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

    @Override
    public void serialize(ShopUnit shopUnit, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeObjectField("id", shopUnit.getId());
        jsonGenerator.writeStringField("name", shopUnit.getName());
        jsonGenerator.writeObjectField("type", shopUnit.getType());
        jsonGenerator.writeObjectField("parentId", shopUnit.getParentId());
        jsonGenerator.writeStringField("date", ISO8601_DATE_TIME.format(shopUnit.getDate()));
        jsonGenerator.writeNumberField("price", shopUnit.getPrice());

        if (shopUnit.isCategory()) {
            jsonGenerator.writeArrayFieldStart("children");
            for (ShopUnit s : shopUnit.getChildren()) {
                jsonGenerator.writeObject(s);
            }
            jsonGenerator.writeEndArray();
        } else {
            jsonGenerator.writeStringField("children", null);
        }

        jsonGenerator.writeEndObject();
    }
}
