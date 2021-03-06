// code by jph
package ch.ethz.idsc.gokart.offline.tab;

import java.nio.ByteBuffer;
import java.util.Objects;

import ch.ethz.idsc.gokart.gui.GokartLcmChannel;
import ch.ethz.idsc.gokart.gui.GokartStatusEvent;
import ch.ethz.idsc.gokart.gui.top.ChassisGeometry;
import ch.ethz.idsc.gokart.lcm.autobox.RimoLcmServer;
import ch.ethz.idsc.gokart.offline.api.OfflineTableSupplier;
import ch.ethz.idsc.retina.dev.rimo.RimoGetEvent;
import ch.ethz.idsc.retina.dev.rimo.RimoPutEvent;
import ch.ethz.idsc.retina.dev.rimo.RimoPutHelper;
import ch.ethz.idsc.retina.dev.steer.SteerPutEvent;
import ch.ethz.idsc.retina.util.math.Magnitude;
import ch.ethz.idsc.retina.util.math.SI;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.io.TableBuilder;
import ch.ethz.idsc.tensor.qty.Quantity;

public class RimoRateTable implements OfflineTableSupplier {
  private final TableBuilder tableBuilder = new TableBuilder();
  private final Scalar delta;
  // ---
  private Scalar time_next = Quantity.of(0, SI.SECOND);
  private RimoGetEvent rge;
  private RimoPutEvent rpe;
  private GokartStatusEvent gse;

  public RimoRateTable(Scalar delta) {
    this.delta = delta;
  }

  @Override // from OfflineLogListener
  public void event(Scalar time, String channel, ByteBuffer byteBuffer) {
    if (channel.equals(RimoLcmServer.CHANNEL_GET)) {
      rge = new RimoGetEvent(byteBuffer);
    } else //
    if (channel.equals(RimoLcmServer.CHANNEL_PUT)) {
      rpe = RimoPutHelper.from(byteBuffer);
    } else //
    if (channel.equals(GokartLcmChannel.STATUS)) {
      gse = new GokartStatusEvent(byteBuffer);
    }
    if (Scalars.lessThan(time_next, time)) {
      if (Objects.nonNull(rge) && Objects.nonNull(rpe) && Objects.nonNull(gse) && gse.isSteerColumnCalibrated()) {
        // System.out.println("export " + time.number().doubleValue());
        time_next = time.add(delta);
        // ---
        Tensor rates = rge.getAngularRate_Y_pair();
        Scalar speed = ChassisGeometry.GLOBAL.odometryTangentSpeed(rge);
        Scalar rate = ChassisGeometry.GLOBAL.odometryTurningRate(rge);
        tableBuilder.appendRow( //
            time.map(Magnitude.SECOND), //
            rpe.getTorque_Y_pair().map(Magnitude.ARMS), // ARMS
            rates.map(Magnitude.PER_SECOND), // rad/s, or 1/s
            speed.map(Magnitude.VELOCITY), // m/s
            rate.map(Magnitude.PER_SECOND), //
            gse.getSteerColumnEncoderCentered().map(SteerPutEvent.ENCODER));
      }
    }
  }

  @Override // from OfflineTableSupplier
  public Tensor getTable() {
    return tableBuilder.toTable();
  }
}
