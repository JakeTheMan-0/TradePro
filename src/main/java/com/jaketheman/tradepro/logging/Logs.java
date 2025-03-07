package com.jaketheman.tradepro.logging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.jaketheman.tradepro.TradePro;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.UnaryOperator;

public class Logs implements List<TradeLog> {

  private static final DateFormat folderNameFormat =
          new SimpleDateFormat("'session_'yyyy-MM-dd'_'HH-mm-ss");
  private static final DateFormat fileNameFormat =
          new SimpleDateFormat("'{player1}-{player2}_'HH-mm-ss'.json'");

  private TradePro plugin;

  private File folder;
  private List<TradeLog> logs = new ArrayList<>();

  private Gson gson;

  public Logs(TradePro plugin, File parent, String file) {
    this.plugin = plugin;
    if (!parent.exists()) {
      parent.mkdirs();
    }
    folder = new File(parent, file);
    gson =
            new GsonBuilder()
                    .registerTypeAdapter(
                            UUID.class,
                            new TypeAdapter<UUID>() {
                              @Override
                              public void write(JsonWriter jsonWriter, UUID uuid) throws IOException {
                                jsonWriter.value(uuid.toString());
                              }

                              @Override
                              public UUID read(JsonReader jsonReader) throws IOException {
                                return UUID.fromString(jsonReader.nextString());
                              }
                            })
                    .registerTypeAdapter(
                            Optional.class, // Register the Optional TypeAdapter
                            new TypeAdapter<Optional<?>>() {
                              @Override
                              public void write(JsonWriter out, Optional<?> value) throws IOException {
                                if (value == null || !value.isPresent()) {
                                  out.nullValue(); // Serialize as null if empty
                                } else {
                                  Gson gson = new Gson();
                                  gson.toJson(value.get(), value.get().getClass(), out);

                                }
                              }

                              @Override
                              public Optional<?> read(JsonReader in) throws IOException {
                                // Handle null values in JSON
                                if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                                  in.nextNull();
                                  return Optional.empty(); // Return an empty Optional
                                } else {

                                  Gson gson = new Gson();
                                  Object value = gson.fromJson(in, Object.class);
                                  return Optional.ofNullable(value);
                                }
                              }
                            })
                    .excludeFieldsWithoutExposeAnnotation() // Add this line
                    .registerTypeAdapterFactory(new PostProcessingEnabler())
                    .registerTypeHierarchyAdapter(List.class, new NullEmptyListAdapter())
                    .registerTypeHierarchyAdapter(Number.class, new NullZeroNumberAdapter())
                    .setPrettyPrinting()
                    .create();
    //    File[] contents;
    //    if (folder.exists() && (contents = folder.listFiles()) != null) {
    //      for (File child : contents) {
    //        FileReader reader = new FileReader(child);
    //        add(gson.fromJson(reader, TradeLog.class));
    //        reader.close();
    //      }
    //    }
  }

  public Logs(TradePro plugin, File parent) {
    this(plugin, parent, folderNameFormat.format(new Date()));
  }

  public void log(TradeLog log) {
    logs.add(log);
  }

  public void save() {
    try {
      if (!logs.isEmpty()) {
        if (!folder.exists()) folder.mkdirs();
        Iterator<TradeLog> iter = iterator();
        while (iter.hasNext()) {
          TradeLog log = iter.next();
          try {
            File file =
                    new File(
                            folder,
                            fileNameFormat
                                    .format(log.getTime())
                                    .replace("{player1}", log.getPlayer1().getLastKnownName())
                                    .replace("{player2}", log.getPlayer2().getLastKnownName()));
            if (!file.exists()) file.createNewFile();
            FileWriter writer = new FileWriter(file);
            gson.toJson(log, TradeLog.class, writer);
            writer.close();
          } catch (Exception | Error ex) {
            plugin.getLogger()
                    .warning(
                            "Failed to save trade log for trade between "
                                    + log.getPlayer1().getLastKnownName()
                                    + " and "
                                    + log.getPlayer2().getLastKnownName());
            plugin.getLogger().warning(ex.getLocalizedMessage());
          }
          iter.remove();
        }
      }
    } catch (Exception | Error ex) {
      plugin.getLogger().warning("Failed to save trade logs.");
      logs.clear();
    }
  }

  @Override
  public int size() {
    return logs.size();
  }

  @Override
  public boolean isEmpty() {
    return logs.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return logs.contains(o);
  }

  @Override
  public Iterator<TradeLog> iterator() {
    return logs.iterator();
  }

  @Override
  public Object[] toArray() {
    return logs.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return logs.toArray(a);
  }

  @Override
  public boolean add(TradeLog tradeLog) {
    return logs.add(tradeLog);
  }

  @Override
  public boolean remove(Object o) {
    return logs.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return logs.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends TradeLog> c) {
    return logs.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends TradeLog> c) {
    return logs.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return logs.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return logs.retainAll(c);
  }

  @Override
  public void replaceAll(UnaryOperator<TradeLog> operator) {
    logs.replaceAll(operator);
  }

  @Override
  public void sort(Comparator<? super TradeLog> c) {
    logs.sort(c);
  }

  @Override
  public void clear() {
    logs.clear();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return logs.equals(o);
  }

  @Override
  public int hashCode() {
    return logs.hashCode();
  }

  @Override
  public TradeLog get(int index) {
    return logs.get(index);
  }

  @Override
  public TradeLog set(int index, TradeLog element) {
    return logs.set(index, element);
  }

  @Override
  public void add(int index, TradeLog element) {
    logs.add(index, element);
  }

  @Override
  public TradeLog remove(int index) {
    return logs.remove(index);
  }

  @Override
  public int indexOf(Object o) {
    return logs.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return logs.lastIndexOf(o);
  }

  @Override
  public ListIterator<TradeLog> listIterator() {
    return logs.listIterator();
  }

  @Override
  public ListIterator<TradeLog> listIterator(int index) {
    return logs.listIterator(index);
  }

  @Override
  public List<TradeLog> subList(int fromIndex, int toIndex) {
    return logs.subList(fromIndex, toIndex);
  }

  @Override
  public Spliterator<TradeLog> spliterator() {
    return logs.spliterator();
  }
}