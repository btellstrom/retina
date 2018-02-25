// code by jpg
package ch.ethz.idsc.retina.dev.davis.app;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import ch.ethz.idsc.owl.data.GlobalAssert;
import ch.ethz.idsc.retina.dev.davis.DavisDevice;
import ch.ethz.idsc.retina.dev.davis.DavisDvsListener;
import ch.ethz.idsc.retina.dev.davis._240c.DavisDvsEvent;
import ch.ethz.idsc.retina.util.TimedImageEvent;
import ch.ethz.idsc.retina.util.TimedImageListener;

/** synthesizes grayscale images based on incoming events during intervals of
 * fixed duration positive events appear in white color negative events appear
 * in black color */
public abstract class AbstractAccumulatedImage implements DavisDvsListener {
  /** default value 50 ms */
  public static final int INTERVAL_DEFAULT_US = 50_000;
  /** periods without events of length longer than max gap means the timer
   * will skip to the next event position. this is the case when the log file
   * skips to the future. */
  private static final int MAX_GAP_US = 10_000;
  // ---
  private static final byte CLEAR_BYTE = (byte) 128;
  // ---
  protected final int width;
  private final int height;
  private final List<TimedImageListener> listeners = new LinkedList<>();
  private final BufferedImage bufferedImage;
  protected final byte[] bytes;
  private int interval;
  private int max_gap;
  private Integer last = null;

  protected AbstractAccumulatedImage(DavisDevice davisDevice) {
    setInterval(INTERVAL_DEFAULT_US);
    width = davisDevice.getWidth();
    height = davisDevice.getHeight();
    bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
    DataBufferByte dataBufferByte = (DataBufferByte) bufferedImage.getRaster().getDataBuffer();
    bytes = dataBufferByte.getData();
    GlobalAssert.that(bytes.length == width * height);
    clearImage();
  }

  public final void addListener(TimedImageListener timedImageListener) {
    listeners.add(timedImageListener);
  }

  public final void setInterval(int interval) {
    this.interval = interval;
    max_gap = Math.max(2 * interval, MAX_GAP_US);
  }

  public final int getInterval() {
    return interval;
  }

  @Override // from DavisDvsListener
  public final void davisDvs(DavisDvsEvent dvsDavisEvent) {
    if (Objects.isNull(last))
      last = dvsDavisEvent.time;
    final int delta = dvsDavisEvent.time - last;
    if (0 <= delta && delta < interval) // nominal case
      assign(delta, dvsDavisEvent);
    else //
    if (max_gap <= delta) {
      System.err.println("dvs image clear due to forward timing");
      clearImage();
      last = dvsDavisEvent.time;
    } else //
    if (interval <= delta) {
      TimedImageEvent timedImageEvent = new TimedImageEvent(last, bufferedImage);
      listeners.forEach(listener -> listener.timedImage(timedImageEvent));
      clearImage();
      last += interval;
    } else //
    if (delta < 0) { // this case happens during davis log playback when skipping to the front
      System.err.println("dvs image clear due to reverse timing");
      clearImage();
      last = dvsDavisEvent.time;
    }
  }

  protected abstract void assign(int delta, DavisDvsEvent dvsDavisEvent);

  private void clearImage() {
    IntStream.range(0, bytes.length).forEach(i -> bytes[i] = CLEAR_BYTE);
  }
}
