package asia.rgp.game.nagas.infrastructure.grpc;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.msgpack.core.ExtensionTypeHeader;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

/**
 * Helper class for MessagePack encoding/decoding. Used by gRPC adapter to decode incoming data and
 * encode outgoing responses.
 */
@Slf4j
public final class MessagePackHelper {

  private MessagePackHelper() {}

  /** Decode MessagePack bytes to a Map. */
  public static Map<String, Object> decode(byte[] data) throws IOException {
    if (data == null || data.length == 0) {
      return Collections.emptyMap();
    }

    Map<String, Object> decodedByMarioCodec = decodeWithMarioBytesCodec(data);
    if (!decodedByMarioCodec.isEmpty()) {
      log.info(
          "[MPack-Decode] Mario bytes decode success | size={} keys={}",
          data.length,
          decodedByMarioCodec.keySet());
      return decodedByMarioCodec;
    }

    decodedByMarioCodec = decodeWithMarioExtensionCodec(data);
    if (!decodedByMarioCodec.isEmpty()) {
      log.info(
          "[MPack-Decode] Mario extension decode success | size={} keys={}",
          data.length,
          decodedByMarioCodec.keySet());
      return decodedByMarioCodec;
    }

    try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
      Value value = unpacker.unpackValue();

      Map<String, Object> result = valueToMap(value);
      if (result.isEmpty()) {
        result = findMapInValue(value);
      }

      log.info(
          "[MPack-Decode] rootType={} size={} keys={}",
          value.getValueType(),
          data.length,
          result.keySet());

      if (result.isEmpty()) {
        log.warn(
            "[MPack-Decode] Empty decoded payload. rootType={} size={}",
            value.getValueType(),
            data.length);
      }

      return result;
    }
  }

  private static Map<String, Object> decodeWithMarioBytesCodec(byte[] data) {
    try {
      Class<?> codecClass = Class.forName("com.luigi.gaas.common.data.msgpkg.MarioBytesCodec");
      Method unpackMethod = codecClass.getMethod("unpack", byte[].class);
      Object unpacked = unpackMethod.invoke(null, data);
      return toMap(unpacked);
    } catch (ClassNotFoundException e) {
      log.warn("[MPack-Decode] Mario bytes codec not available on classpath");
    } catch (Exception e) {
      log.debug(
          "[MPack-Decode] Mario bytes decode failed | size={} error={}",
          data.length,
          e.getMessage());
    }
    return Collections.emptyMap();
  }

  private static Map<String, Object> decodeWithMarioExtensionCodec(byte[] data) {
    try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
      if (!unpacker.hasNext()) {
        return Collections.emptyMap();
      }

      if (unpacker.getNextFormat().getValueType() != ValueType.EXTENSION) {
        return Collections.emptyMap();
      }

      ExtensionTypeHeader header = unpacker.unpackExtensionTypeHeader();
      Object unpacked = decodeMarioExtension(header.getType(), unpacker);
      return toMap(unpacked);

    } catch (ClassNotFoundException e) {
      log.warn("[MPack-Decode] Mario extension codec manager not available on classpath");
    } catch (Exception e) {
      log.debug(
          "[MPack-Decode] Mario extension decode failed | type={} size={} error={}",
          "unknown",
          data.length,
          e.getMessage());
    }
    return Collections.emptyMap();
  }

  private static Object decodeMarioExtension(byte extensionType, MessageUnpacker unpacker)
      throws Exception {
    Class<?> managerClass =
        Class.forName("com.luigi.gaas.common.data.msgpkg.MarioPackerExtensionManager");
    Method getCodecMethod = managerClass.getMethod("getCodec", byte.class);
    Object codec = getCodecMethod.invoke(null, extensionType);
    if (codec == null) {
      return null;
    }

    Method decodeMethod = codec.getClass().getMethod("decode", MessageUnpacker.class);
    return decodeMethod.invoke(codec, unpacker);
  }

  private static Map<String, Object> toMap(Object unpacked) throws Exception {
    if (unpacked == null) {
      return Collections.emptyMap();
    }

    if (unpacked instanceof Map<?, ?> map) {
      return copyMap(map);
    }

    if ("com.luigi.gaas.common.data.PuObject".equals(unpacked.getClass().getName())) {
      Method toMapMethod = unpacked.getClass().getMethod("toMap");
      Object mapObject = toMapMethod.invoke(unpacked);
      if (mapObject instanceof Map<?, ?> map) {
        return copyMap(map);
      }
    }

    return Collections.emptyMap();
  }

  private static Map<String, Object> copyMap(Map<?, ?> map) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      result.put(String.valueOf(entry.getKey()), entry.getValue());
    }
    return result;
  }

  /** Encode a success response in format: [5, {cmd, c:0, ...data}] */
  public static byte[] encodeResponse(int cmd, Map<String, Object> data) throws IOException {
    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      packer.packArrayHeader(2);
      packer.packInt(5); // message type = response

      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("cmd", cmd);
      payload.put("c", 0); // success code
      if (data != null) {
        payload.putAll(data);
      }

      packMap(packer, payload);
      return packer.toByteArray();
    }
  }

  /** Encode an error response in format: [5, {cmd, c:errorCode, msg:errorMsg}] */
  public static byte[] encodeError(int cmd, int errorCode, String errorMsg) throws IOException {
    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      packer.packArrayHeader(2);
      packer.packInt(5); // message type = response

      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("cmd", cmd);
      payload.put("c", errorCode);
      payload.put("msg", errorMsg);

      packMap(packer, payload);
      return packer.toByteArray();
    }
  }

  // ===== Internal helpers =====

  private static void packMap(MessageBufferPacker packer, Map<String, Object> map)
      throws IOException {
    packer.packMapHeader(map.size());
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      packer.packString(entry.getKey());
      packValue(packer, entry.getValue());
    }
  }

  /**
   * Pack integer using signed MessagePack types only. Avoids uint8/uint16/uint32/uint64 (0xcc–0xcf)
   * which PuElementSerializer (Luigi GaaS) in be-wsproxy does not support. Uses fixint for
   * -32..127, int64 (0xd3) for all other values.
   */
  private static void packSignedLong(MessageBufferPacker packer, long v) throws IOException {
    if (v >= -32 && v <= 127) {
      packer.packLong(v); // fixint / negative fixint — always 1 byte, universally safe
    } else {
      packer.addPayload(
          new byte[] {
            (byte) 0xd3,
            (byte) (v >> 56),
            (byte) (v >> 48),
            (byte) (v >> 40),
            (byte) (v >> 32),
            (byte) (v >> 24),
            (byte) (v >> 16),
            (byte) (v >> 8),
            (byte) v
          });
    }
  }

  @SuppressWarnings("unchecked")
  private static void packValue(MessageBufferPacker packer, Object value) throws IOException {
    if (value == null) {
      packer.packNil();
    } else if (value instanceof Integer i) {
      packSignedLong(packer, i);
    } else if (value instanceof Long l) {
      packSignedLong(packer, l);
    } else if (value instanceof BigDecimal bd) {
      packer.packDouble(bd.doubleValue());
    } else if (value instanceof Double d) {
      packer.packDouble(d);
    } else if (value instanceof Float f) {
      packer.packFloat(f);
    } else if (value instanceof Boolean b) {
      packer.packBoolean(b);
    } else if (value instanceof String s) {
      packer.packString(s);
    } else if (value instanceof Map<?, ?> m) {
      packMap(packer, (Map<String, Object>) m);
    } else if (value instanceof List<?> list) {
      packer.packArrayHeader(list.size());
      for (Object item : list) {
        packValue(packer, item);
      }
    } else {
      packer.packString(value.toString());
    }
  }

  private static Map<String, Object> valueToMap(Value value) {
    if (value.getValueType() != ValueType.MAP) {
      return Collections.emptyMap();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<Value, Value> entry : value.asMapValue().entrySet()) {
      String key = entry.getKey().asStringValue().asString();
      result.put(key, valueToObject(entry.getValue()));
    }
    return result;
  }

  private static Map<String, Object> findMapInValue(Value value) throws IOException {
    if (value == null) {
      return Collections.emptyMap();
    }

    return switch (value.getValueType()) {
      case MAP -> valueToMap(value);
      case ARRAY -> {
        for (Value item : value.asArrayValue()) {
          Map<String, Object> nested = findMapInValue(item);
          if (!nested.isEmpty()) {
            yield nested;
          }
        }
        yield Collections.emptyMap();
      }
      case BINARY -> decodeNestedBinary(value.asBinaryValue().asByteArray());
      case EXTENSION -> decodeNestedBinary(value.asExtensionValue().getData());
      default -> Collections.emptyMap();
    };
  }

  private static Map<String, Object> decodeNestedBinary(byte[] nestedData) throws IOException {
    if (nestedData == null || nestedData.length == 0) {
      return Collections.emptyMap();
    }

    try (MessageUnpacker nestedUnpacker = MessagePack.newDefaultUnpacker(nestedData)) {
      Value nestedValue = nestedUnpacker.unpackValue();
      Map<String, Object> result = valueToMap(nestedValue);
      if (result.isEmpty()) {
        result = findMapInValue(nestedValue);
      }
      return result;
    } catch (Exception e) {
      log.debug("[MPack-Decode] Nested binary decode failed: {}", e.getMessage());
      return Collections.emptyMap();
    }
  }

  private static Object valueToObject(Value value) {
    return switch (value.getValueType()) {
      case NIL -> null;
      case BOOLEAN -> value.asBooleanValue().getBoolean();
      case INTEGER -> {
        long l = value.asIntegerValue().asLong();
        yield (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? (int) l : l;
      }
      case FLOAT -> value.asFloatValue().toDouble();
      case STRING -> value.asStringValue().asString();
      case BINARY -> value.asBinaryValue().asByteArray();
      case ARRAY -> {
        List<Object> list = new ArrayList<>();
        for (Value v : value.asArrayValue()) {
          list.add(valueToObject(v));
        }
        yield list;
      }
      case MAP -> valueToMap(value);
      default -> value.toString();
    };
  }
}
