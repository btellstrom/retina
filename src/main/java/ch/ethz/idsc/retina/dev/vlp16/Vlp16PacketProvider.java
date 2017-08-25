// code by jph
package ch.ethz.idsc.retina.dev.vlp16;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import ch.ethz.idsc.retina.util.io.ByteArrayConsumer;

/** default packet distribution
 * 
 * implementation decides based on length of packet to
 * process the data either as firing packet or as GPS */
public class Vlp16PacketProvider implements ByteArrayConsumer {
  /** the answer to life the universe and everything
   * 
   * hdl32e user's manual refers to first 42 bytes as ethernet header
   * they are only present in pcap file, but not in upd packets from live sensor */
  public static final int _42 = 42;
  public static final int LASER_SIZE1 = 1248; // Hdl32eStatics.RAY_PACKET_LENGTH + 42
  public static final int LASER_SIZE2 = Vlp16Statics.RAY_PACKET_LENGTH;
  public static final int GPS_SIZE1 = 554;
  public static final int GPS_SIZE2 = Vlp16Statics.POS_PACKET_LENGTH;
  // ---
  // public final Vlp16RayDecoder hdl32eRayDecoder = new Vlp16RayDecoder();
  public final Vlp16PosDecoder vlp16PosDecoder = new Vlp16PosDecoder();

  @Override
  public void accept(byte[] packet_data, int length) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(packet_data);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    // if (length == LASER_SIZE1) {
    // byteBuffer.position(_42); // skip 42 bytes
    // hdl32eRayDecoder.lasers(byteBuffer);
    // } else //
    // if (length == LASER_SIZE2) {
    // hdl32eRayDecoder.lasers(byteBuffer);
    // }
    if (length == GPS_SIZE1) {
      byteBuffer.position(_42); // skip 42 bytes
      vlp16PosDecoder.positioning(byteBuffer);
    } else //
    if (length == GPS_SIZE2) {
      vlp16PosDecoder.positioning(byteBuffer);
    }
    // else {
    // System.err.println("unhandled packet");
    // }
  }
}
