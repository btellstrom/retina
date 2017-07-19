// code by jph
package ch.ethz.idsc.retina.dev.velodyne;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class AbstractHDL32EFiringPacketConsumer implements HDL32EFiringPacketConsumer {
  // private byte[] ethernet_header = new byte[42];
  @Override
  public final void lasers(byte[] laser_data) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(laser_data);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    {
      byteBuffer.position(42); // begin of firing data
      // 12 blocks of firing data
      for (int firing = 0; firing < 12; ++firing) {
        _assert(byteBuffer.position() == 42 + firing * 100);
        int blockId = byteBuffer.getShort() & 0xffff; // laser block ID, 61183 ?
        int rotational = byteBuffer.getShort() & 0xffff; // rotational [0, ..., 35999]
        // ---
        process(firing, rotational, byteBuffer);
      }
      _assert(byteBuffer.position() == 1242);
    }
    { // status data
      int gps_timestamp = byteBuffer.getInt();
      byte type = byteBuffer.get(); // 55
      byte value = byteBuffer.get(); // 33
    }
  }

  /** implementations have to advance byteBuffer by 96 bytes
   * 
   * @param firing ranges from [0, ..., 11]
   * @param rotational [0, ..., 35999]
   * @param byteBuffer */
  public abstract void process(int firing, int rotational, ByteBuffer byteBuffer);

  private static void _assert(boolean check) {
    if (!check)
      throw new RuntimeException();
  }
}
