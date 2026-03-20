package asia.rgp.game.nagas.infrastructure.grpc;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Helper class for MessagePack encoding/decoding.
 * Used by gRPC adapter to decode incoming data and encode outgoing responses.
 */
public final class MessagePackHelper {

    private MessagePackHelper() {
    }

    /**
     * Decode MessagePack bytes to a Map.
     */
    public static Map<String, Object> decode(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return Collections.emptyMap();
        }
        try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
            Value value = unpacker.unpackValue();

            // Handle array format [type, {payload}] — extract map from index 1
            if (value.getValueType() == ValueType.ARRAY) {
                List<Value> array = value.asArrayValue().list();
                if (array.size() >= 2 && array.get(1).getValueType() == ValueType.MAP) {
                    return valueToMap(array.get(1));
                }
                // Single-element array or no map at index 1: try first map found
                for (Value v : array) {
                    if (v.getValueType() == ValueType.MAP) {
                        return valueToMap(v);
                    }
                }
                return Collections.emptyMap();
            }

            return valueToMap(value);
        }
    }

    /**
     * Encode a success response in format: [5, {cmd, c:0, ...data}]
     */
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

    /**
     * Encode an error response in format: [5, {cmd, c:errorCode, msg:errorMsg}]
     */
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

    @SuppressWarnings("unchecked")
    private static void packMap(MessageBufferPacker packer, Map<String, Object> map) throws IOException {
        packer.packMapHeader(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            packer.packString(entry.getKey());
            packValue(packer, entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static void packValue(MessageBufferPacker packer, Object value) throws IOException {
        if (value == null) {
            packer.packNil();
        } else if (value instanceof Integer i) {
            packer.packInt(i);
        } else if (value instanceof Long l) {
            packer.packLong(l);
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
