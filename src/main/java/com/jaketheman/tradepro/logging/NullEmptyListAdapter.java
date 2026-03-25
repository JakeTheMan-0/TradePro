package com.jaketheman.tradepro.logging;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

class NullEmptyListAdapter implements JsonSerializer<List<?>>, JsonDeserializer<List<?>> {

  @Override
  public JsonElement serialize(List<?> src, Type type, JsonSerializationContext context) {
    if (src == null || src.isEmpty()) return null;
    JsonArray array = new JsonArray();
    for (Object obj : src) {
      array.add(context.serialize(obj));
    }
    return array;
  }

  @Override
  public List<?> deserialize(JsonElement src, Type type, JsonDeserializationContext context)
          throws JsonParseException {
    if (src == null || src.isJsonNull()) return new ArrayList<>();
    if (!src.isJsonArray()) return new ArrayList<>();

    JsonArray array = src.getAsJsonArray();
    List<Object> list = new ArrayList<>();

    // We need to figure out the component type of the list
    Type innerType = Object.class;
    if (type instanceof java.lang.reflect.ParameterizedType) {
      innerType = ((java.lang.reflect.ParameterizedType) type).getActualTypeArguments()[0];
    }

    for (JsonElement elem : array) {
      list.add(context.deserialize(elem, innerType));
    }
    return list;
  }
}
