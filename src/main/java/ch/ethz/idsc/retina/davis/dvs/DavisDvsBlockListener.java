// code by jph
package ch.ethz.idsc.retina.davis.dvs;

import java.nio.ByteBuffer;

/** notifies that block of aps columns is completed */
public interface DavisDvsBlockListener {
  void dvsBlockReady(int length, ByteBuffer byteBuffer);
}