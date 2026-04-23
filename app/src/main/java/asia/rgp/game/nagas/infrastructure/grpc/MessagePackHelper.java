package asia.rgp.game.nagas.infrastructure.grpc;

import com.luigi.gaas.common.data.PuArrayList;
import com.luigi.gaas.common.data.PuElement;
import com.luigi.gaas.common.data.PuObject;
import com.luigi.gaas.common.data.msgpkg.MarioBytesCodec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * MessagePack/PuElement codec for the GaaS gRPC adapter. Both inbound and outbound payloads are
 * Mario PuElement (not plain MessagePack) — wsproxy's frame decoder expects this format on the ZMQ
 * wire, and the {@code [5, map]} response envelope is encoded as a {@link PuArrayList}.
 */
@Slf4j
public final class MessagePackHelper {

  private MessagePackHelper() {}

  /** Decode a Mario PuElement payload (from wsproxy) into a plain map. */
  public static Map<String, Object> decode(byte[] data) {
    if (data == null || data.length == 0) {
      return Collections.emptyMap();
    }
    try {
      Map<String, Object> decoded = toMap(MarioBytesCodec.unpack(data));
      if (!decoded.isEmpty()) {
        return decoded;
      }
      log.warn(
          "[decode] Unexpected PuElement root type: {}",
          describeType(MarioBytesCodec.unpack(data)));
      return Collections.emptyMap();
    } catch (Exception e) {
      log.warn("[decode] Mario unpack failed ({} bytes): {}", data.length, e.getMessage());
      return Collections.emptyMap();
    }
  }

  public static PuElement unpackPuElement(byte[] data) throws IOException {
    if (data == null || data.length == 0) {
      throw new IOException("Empty payload");
    }
    try {
      Object unpacked = MarioBytesCodec.unpack(data);
      if (unpacked instanceof PuElement puElement) {
        return puElement;
      }
      if (unpacked instanceof Map<?, ?> map) {
        return PuObject.fromObject(copyMap(map));
      }
      throw new IOException("Unexpected PuElement root type: " + describeType(unpacked));
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Mario unpack failed: " + e.getMessage(), e);
    }
  }

  /** Encode a success response as PuElement bytes: {@code [5, {cmd, c:0, ...data}]}. */
  public static byte[] encodeResponse(int cmd, Map<String, Object> data) throws IOException {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("cmd", cmd);
    payload.put("c", 0);
    if (data != null) {
      payload.putAll(data);
    }
    return packEnvelope(payload);
  }

  /** Encode an error response as PuElement bytes: {@code [5, {cmd, c:errorCode, msg:errorMsg}]}. */
  public static byte[] encodeError(int cmd, int errorCode, String errorMsg) throws IOException {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("cmd", cmd);
    payload.put("c", errorCode);
    payload.put("msg", errorMsg);
    return packEnvelope(payload);
  }

  private static byte[] packEnvelope(Map<String, Object> payload) throws IOException {
    PuArrayList array = new PuArrayList();
    array.addFrom(5, PuObject.fromObject(payload));
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      MarioBytesCodec.pack(baos, array);
      return baos.toByteArray();
    }
  }

  private static Map<String, Object> toMap(Object unpacked) {
    if (unpacked == null) {
      return Collections.emptyMap();
    }
    if (unpacked instanceof PuObject puObject) {
      return copyMap(puObject.toMap());
    }
    if (unpacked instanceof Map<?, ?> map) {
      return copyMap(map);
    }
    if (unpacked instanceof Iterable<?> iterable) {
      Map<String, Object> firstMap = findFirstMap(iterable);
      if (!firstMap.isEmpty()) {
        return firstMap;
      }
      return mapFromIterable(iterable);
    }

    Map<String, Object> reflectedMap = invokeMapMethod(unpacked, "toMap");
    if (!reflectedMap.isEmpty()) {
      return reflectedMap;
    }

    return invokeMapMethod(unpacked, "toObject");
  }

  private static Map<String, Object> invokeMapMethod(Object target, String methodName) {
    try {
      Method method = target.getClass().getMethod(methodName);
      return toMap(method.invoke(target));
    } catch (Exception ignored) {
      return Collections.emptyMap();
    }
  }

  private static Map<String, Object> findFirstMap(Iterable<?> iterable) {
    for (Object item : iterable) {
      Map<String, Object> nested = toMap(item);
      if (!nested.isEmpty()) {
        return nested;
      }
    }
    return Collections.emptyMap();
  }

  private static Map<String, Object> mapFromIterable(Iterable<?> iterable) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("_type", "array");
    List<Object> items = new ArrayList<>();
    for (Object item : iterable) {
      Object normalized = normalizeValue(item);
      items.add(normalized);
    }
    result.put("items", items);
    return result;
  }

  private static Map<String, Object> copyMap(Map<?, ?> map) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      result.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
    }
    return result;
  }

  private static Object normalizeValue(Object value) {
    if (value instanceof PuObject puObject) {
      return copyMap(puObject.toMap());
    }
    if (value instanceof Map<?, ?> map) {
      return copyMap(map);
    }
    if (value instanceof Iterable<?> iterable) {
      List<Object> items = new ArrayList<>();
      for (Object item : iterable) {
        items.add(normalizeValue(item));
      }
      return items;
    }
    return value;
  }

  private static String describeType(Object value) {
    return value == null ? "null" : value.getClass().getName();
  }
}
