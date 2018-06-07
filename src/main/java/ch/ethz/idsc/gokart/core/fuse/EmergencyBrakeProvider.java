// code by jph
package ch.ethz.idsc.gokart.core.fuse;

import ch.ethz.idsc.gokart.core.AutoboxScheduledProvider;
import ch.ethz.idsc.gokart.gui.top.ChassisGeometry;
import ch.ethz.idsc.owl.math.state.ProviderRank;
import ch.ethz.idsc.retina.dev.linmot.LinmotConfig;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutEvent;
import ch.ethz.idsc.retina.dev.linmot.LinmotPutOperation;
import ch.ethz.idsc.retina.dev.rimo.RimoGetEvent;
import ch.ethz.idsc.retina.dev.rimo.RimoGetListener;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.sca.Clip;

public class EmergencyBrakeProvider extends AutoboxScheduledProvider<LinmotPutEvent> implements RimoGetListener {
  private static final Clip CLIP = Clip.function(Quantity.of(0, SI.VELOCITY), Quantity.of(6, SI.VELOCITY));
  private static final Scalar MIN_VELOCITY = Quantity.of(0.3, SI.VELOCITY); // TODO magic const
  // ---
  public static final EmergencyBrakeProvider INSTANCE = new EmergencyBrakeProvider();
  // ---
  private Scalar velocity = Quantity.of(0.0, SI.METER);

  private EmergencyBrakeProvider() {
  }

  @Override // from PutProvider
  public final ProviderRank getProviderRank() {
    return ProviderRank.EMERGENCY;
  }

  @Override // from AutoboxScheduledProvider
  protected void protected_schedule() {
    long timestamp = now_ms();
    EmergencyBrakeManeuver emergencyBrakeManeuver = LinmotConfig.GLOBAL.brakeDistance(velocity);
    long duration_ms = emergencyBrakeManeuver.getDuration_ms();
    System.out.println("brake duration=" + duration_ms);
    eventUntil( //
        timestamp += duration_ms, //
        () -> LinmotPutOperation.INSTANCE.toRelativePosition(RealScalar.ONE));
  }

  @Override // from RimoGetListener
  public void getEvent(RimoGetEvent rimoGetEvent) {
    velocity = CLIP.apply(ChassisGeometry.GLOBAL.odometryTangentSpeed(rimoGetEvent));
  }

  /** @param min without unit but with interpretation in meter from lidar */
  public void consider(Scalar min) {
    if (Scalars.lessEquals(MIN_VELOCITY, velocity) && isIdle()) {
      EmergencyBrakeManeuver emergencyBrakeManeuver = LinmotConfig.GLOBAL.brakeDistance(velocity);
      Scalar margin = DoubleScalar.of(1.6); // TODO magic const
      if (emergencyBrakeManeuver.isRequired(Quantity.of(min.subtract(margin), SI.METER)))
        schedule();
    }
  }
}
