package asia.rgp.game.nagas.infrastructure.grpc;

import com.luigi.gaas.common.data.PuArrayList;
import com.luigi.gaas.common.data.PuObject;
import com.luigi.gaas.common.data.msgpkg.MarioBytesCodec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
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
      Object unpacked = MarioBytesCodec.unpack(data);
      if (unpacked instanceof PuObject puObject) {
        return puObject.toMap();
      }
      log.warn(
          "[decode] Unexpected PuElement root type: {}",
          unpacked == null ? "null" : unpacked.getClass().getName());
      return Collections.emptyMap();
    } catch (Exception e) {
      log.warn("[decode] Mario unpack failed ({} bytes): {}", data.length, e.getMessage());
      return Collections.emptyMap();
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
}
