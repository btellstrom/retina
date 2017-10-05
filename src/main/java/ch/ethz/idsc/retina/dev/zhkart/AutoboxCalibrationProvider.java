// code by ej
package ch.ethz.idsc.retina.dev.zhkart;

import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

// TODO NRJ still need to test
public abstract class AutoboxCalibrationProvider<PE extends DataEvent> implements PutProvider<PE> {
  private final Queue<TimedPutEvent<PE>> queue = new PriorityQueue<>();

  protected AutoboxCalibrationProvider() {
  }

  private synchronized void removeOld() {
    TimedPutEvent<PE> timedPutEvent = queue.peek();
    while (Objects.nonNull(timedPutEvent)) {
      if (timedPutEvent.time_ms < System.currentTimeMillis()) {
        queue.poll();
        timedPutEvent = queue.peek();
      } else
        break;
    }
  }

  public final boolean isIdle() {
    removeOld();
    // ---
    return queue.isEmpty();
  }

  protected synchronized final void doUntil(long time_ms, PE putEvent) {
    queue.add(new TimedPutEvent<>(time_ms, putEvent));
  }

  @Override
  public final ProviderRank getProviderRank() {
    return ProviderRank.CALIBRATION;
  }

  @Override
  public final Optional<PE> putEvent() {
    removeOld(); // <- mandatory
    // ---
    TimedPutEvent<PE> timedPutEvent = queue.peek();
    if (Objects.nonNull(timedPutEvent))
      return Optional.of(timedPutEvent.putEvent);
    return Optional.empty();
  }
}