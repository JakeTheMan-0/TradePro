package com.jaketheman.tradepro.logging;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.jaketheman.tradepro.TradePro;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class Logs implements List<TradeLog> {

  // Use hyphens instead of colons for Windows compatibility.
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

    // Create a Gson instance with our custom adapters and exclusion strategy.
    gson = new GsonBuilder()
            // Register adapters for our commonly used types.
            .registerTypeAdapter(UUID.class, new TypeAdapter<UUID>() {
              @Override
              public void write(JsonWriter out, UUID uuid) throws IOException {
                out.value(uuid.toString());
              }
              @Override
              public UUID read(JsonReader in) throws IOException {
                return UUID.fromString(in.nextString());
              }
            })
            .registerTypeAdapter(TimeZone.class, new TypeAdapter<TimeZone>() {
              @Override
              public void write(JsonWriter out, TimeZone value) throws IOException {
                if (value == null) out.nullValue(); else out.value(value.getID());
              }
              @Override
              public TimeZone read(JsonReader in) throws IOException {
                if ("NULL".equals(in.peek().toString())) { in.nextNull(); return null; }
                return TimeZone.getTimeZone(in.nextString());
              }
            })
            .registerTypeAdapter(Pattern.class, new TypeAdapter<Pattern>() {
              @Override
              public void write(JsonWriter out, Pattern value) throws IOException {
                if (value == null) out.nullValue(); else out.value(value.pattern());
              }
              @Override
              public Pattern read(JsonReader in) throws IOException {
                if ("NULL".equals(in.peek().toString())) { in.nextNull(); return null; }
                return Pattern.compile(in.nextString());
              }
            })
            .registerTypeAdapter(File.class, new TypeAdapter<File>() {
              @Override
              public void write(JsonWriter out, File value) throws IOException {
                if (value == null) out.nullValue(); else out.value(value.getPath());
              }
              @Override
              public File read(JsonReader in) throws IOException {
                if ("NULL".equals(in.peek().toString())) { in.nextNull(); return null; }
                return new File(in.nextString());
              }
            })
            .registerTypeAdapter(Instant.class, new TypeAdapter<Instant>() {
              @Override
              public void write(JsonWriter out, Instant value) throws IOException {
                if (value == null) out.nullValue(); else out.value(value.toString());
              }
              @Override
              public Instant read(JsonReader in) throws IOException {
                if ("NULL".equals(in.peek().toString())) { in.nextNull(); return null; }
                return Instant.parse(in.nextString());
              }
            })
            // Register a hierarchy adapter for CompletableFuture: always write null.
            .registerTypeHierarchyAdapter(CompletableFuture.class, new TypeAdapter<CompletableFuture<?>>() {
              @Override
              public void write(JsonWriter out, CompletableFuture<?> value) throws IOException {
                out.nullValue();
              }
              @Override
              public CompletableFuture<?> read(JsonReader in) throws IOException {
                in.skipValue();
                return null;
              }
            })
            // Register adapters for Optional and AtomicReference types.
            .registerTypeAdapterFactory(new OptionalTypeAdapterFactory())
            .registerTypeAdapterFactory(new AtomicReferenceAdapterFactory())
            // Register the post-processing adapter factory.
            .registerTypeAdapterFactory(new PostProcessingEnabler())
            // Register hierarchy adapters for List and Number.
            .registerTypeHierarchyAdapter(List.class, new NullEmptyListAdapter())
            .registerTypeHierarchyAdapter(Number.class, new NullZeroNumberAdapter())
            // Use an exclusion strategy that skips all fields from classes in problematic packages.
            .setExclusionStrategies(new ExclusionStrategy() {
              @Override
              public boolean shouldSkipField(FieldAttributes f) {
                // Skip any fields declared in java.util.logging
                if (f.getDeclaredClass().equals(java.util.logging.Logger.class)) return true;
                // Skip any fields whose declaring class is in a package starting with "java.nio.channels"
                Package p = f.getDeclaringClass().getPackage();
                if (p != null && p.getName().startsWith("java.nio.channels")) return true;
                return false;
              }
              @Override
              public boolean shouldSkipClass(Class<?> clazz) {
                Package p = clazz.getPackage();
                if (p != null && p.getName().startsWith("java.nio.channels")) return true;
                if (clazz.equals(java.util.logging.Logger.class)) return true;
                return false;
              }
            })
            .setPrettyPrinting()
            .create();
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
            File file = new File(
                    folder,
                    fileNameFormat.format(log.getTime())
                            .replace("{player1}", log.getPlayer1().getLastKnownName())
                            .replace("{player2}", log.getPlayer2().getLastKnownName())
            );
            if (!file.exists()) file.createNewFile();
            FileWriter writer = new FileWriter(file);
            gson.toJson(log, TradeLog.class, writer);
            writer.close();
          } catch (Exception | Error ex) {
            plugin.getLogger().warning(
                    "Failed to save trade log for trade between "
                            + log.getPlayer1().getLastKnownName() + " and "
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

  // ---------------- List Interface Methods ----------------

  @Override
  public int size() { return logs.size(); }
  @Override
  public boolean isEmpty() { return logs.isEmpty(); }
  @Override
  public boolean contains(Object o) { return logs.contains(o); }
  @Override
  public Iterator<TradeLog> iterator() { return logs.iterator(); }
  @Override
  public Object[] toArray() { return logs.toArray(); }
  @Override
  public <T> T[] toArray(T[] a) { return logs.toArray(a); }
  @Override
  public boolean add(TradeLog tradeLog) { return logs.add(tradeLog); }
  @Override
  public boolean remove(Object o) { return logs.remove(o); }
  @Override
  public boolean containsAll(Collection<?> c) { return logs.containsAll(c); }
  @Override
  public boolean addAll(Collection<? extends TradeLog> c) { return logs.addAll(c); }
  @Override
  public boolean addAll(int index, Collection<? extends TradeLog> c) { return logs.addAll(index, c); }
  @Override
  public boolean removeAll(Collection<?> c) { return logs.removeAll(c); }
  @Override
  public boolean retainAll(Collection<?> c) { return logs.retainAll(c); }
  @Override
  public void replaceAll(UnaryOperator<TradeLog> operator) { logs.replaceAll(operator); }
  @Override
  public void sort(Comparator<? super TradeLog> c) { logs.sort(c); }
  @Override
  public void clear() { logs.clear(); }
  @Override
  public boolean equals(Object o) { return logs.equals(o); }
  @Override
  public int hashCode() { return logs.hashCode(); }
  @Override
  public TradeLog get(int index) { return logs.get(index); }
  @Override
  public TradeLog set(int index, TradeLog element) { return logs.set(index, element); }
  @Override
  public void add(int index, TradeLog element) { logs.add(index, element); }
  @Override
  public TradeLog remove(int index) { return logs.remove(index); }
  @Override
  public int indexOf(Object o) { return logs.indexOf(o); }
  @Override
  public int lastIndexOf(Object o) { return logs.lastIndexOf(o); }
  @Override
  public ListIterator<TradeLog> listIterator() { return logs.listIterator(); }
  @Override
  public ListIterator<TradeLog> listIterator(int index) { return logs.listIterator(index); }
  @Override
  public List<TradeLog> subList(int fromIndex, int toIndex) { return logs.subList(fromIndex, toIndex); }
  @Override
  public Spliterator<TradeLog> spliterator() { return logs.spliterator(); }

  // ---------------- Optional Type Adapter Factory ----------------

  private static class OptionalTypeAdapterFactory implements com.google.gson.TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
      if (!Optional.class.isAssignableFrom(typeToken.getRawType())) return null;
      if (!(typeToken.getType() instanceof ParameterizedType)) return null;
      Type actualType = ((ParameterizedType) typeToken.getType()).getActualTypeArguments()[0];
      TypeAdapter<?> delegate = gson.getAdapter(TypeToken.get(actualType));
      @SuppressWarnings("unchecked")
      TypeAdapter<T> result = (TypeAdapter<T>) new OptionalTypeAdapter<>((TypeAdapter<Object>) delegate);
      return result;
    }
  }

  private static class OptionalTypeAdapter<T> extends TypeAdapter<Optional<T>> {
    private final TypeAdapter<T> delegate;
    public OptionalTypeAdapter(TypeAdapter<T> delegate) {
      this.delegate = delegate;
    }
    @Override
    public void write(JsonWriter out, Optional<T> value) throws IOException {
      if (value == null || !value.isPresent()) out.nullValue();
      else delegate.write(out, value.get());
    }
    @Override
    public Optional<T> read(JsonReader in) throws IOException {
      T value = delegate.read(in);
      return Optional.ofNullable(value);
    }
  }

  // ---------------- AtomicReference Adapter Factory ----------------

  private static class AtomicReferenceAdapterFactory implements com.google.gson.TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
      if (!AtomicReference.class.isAssignableFrom(typeToken.getRawType())) return null;
      if (!(typeToken.getType() instanceof ParameterizedType)) return null;
      Type actualType = ((ParameterizedType) typeToken.getType()).getActualTypeArguments()[0];
      TypeAdapter<?> delegate = gson.getAdapter(TypeToken.get(actualType));
      @SuppressWarnings("unchecked")
      TypeAdapter<T> result = (TypeAdapter<T>) new AtomicReferenceTypeAdapter<>((TypeAdapter<Object>) delegate);
      return result;
    }
  }

  private static class AtomicReferenceTypeAdapter<T> extends TypeAdapter<AtomicReference<T>> {
    private final TypeAdapter<T> delegate;
    public AtomicReferenceTypeAdapter(TypeAdapter<T> delegate) {
      this.delegate = delegate;
    }
    @Override
    public void write(JsonWriter out, AtomicReference<T> value) throws IOException {
      if (value == null || value.get() == null) out.nullValue();
      else delegate.write(out, value.get());
    }
    @Override
    public AtomicReference<T> read(JsonReader in) throws IOException {
      T t = delegate.read(in);
      return new AtomicReference<>(t);
    }
  }

  // ---------------- CompletableFuture Adapter Factory ----------------

  private static class CompletableFutureAdapterFactory implements com.google.gson.TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
      if (!CompletableFuture.class.isAssignableFrom(typeToken.getRawType())) return null;
      return (TypeAdapter<T>) new CompletableFutureTypeAdapter<>();
    }
  }

  private static class CompletableFutureTypeAdapter<T> extends TypeAdapter<CompletableFuture<T>> {
    @Override
    public void write(JsonWriter out, CompletableFuture<T> value) throws IOException {
      out.nullValue();
    }
    @Override
    public CompletableFuture<T> read(JsonReader in) throws IOException {
      in.skipValue();
      return null;
    }
  }
}
