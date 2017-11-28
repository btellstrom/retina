// code by jph
package ch.ethz.idsc.retina.dev.steer;

import java.io.Serializable;

import ch.ethz.idsc.retina.sys.AppResources;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.qty.Quantity;
import ch.ethz.idsc.tensor.qty.UnitSystem;
import ch.ethz.idsc.tensor.sca.Clip;

/** parameters for PID controller of steering
 * 
 * there are 2 special units related to the manufacturer of the steering column:
 * "SCE" steer-column encoder
 * "SCT" steer-column torque */
public class SteerConfig implements Serializable {
  public static final SteerConfig GLOBAL = AppResources.load(new SteerConfig());

  private SteerConfig() {
  }

  /***************************************************/
  public Scalar voltageLo = Quantity.of(10.7, "V");
  public Scalar voltageHi = Quantity.of(13.0, "V");
  // ---
  public Scalar calibration = Quantity.of(1.0, "SCT");
  public Scalar Ki = Quantity.of(5.7, "SCE^-1*SCT*s^-1");
  public Scalar Kp = Quantity.of(7.2, "SCE^-1*SCT");
  public Scalar Kd = Quantity.of(0.82, "SCE^-1*SCT*s");
  public Scalar torqueLimit = Quantity.of(1.5, "SCT");
  // ---
  /** conversion factor from measured steer column angle to front wheel angle */
  public Scalar column2steer = Quantity.of(0.6, "rad*SCE^-1");
  /** 0.5 corresponds to 50% of torque limit */
  public Scalar stepOfLimit = RealScalar.of(0.5);

  /***************************************************/
  /** @return voltage operating range of battery */
  public Clip operatingVoltageClip() {
    return Clip.function(voltageLo, voltageHi);
  }

  /** @return symmetric interval centered at zero that bounds the torque
   * applied to the steering wheel */
  public Clip torqueLimitClip() {
    return Clip.function(torqueLimit.negate(), torqueLimit);
  }

  // TODO code redundant to GlobalStatusEvent...
  public static Scalar getAngleFromSCE(Scalar steerColumnEncoder) {
    return UnitSystem.SI().apply( //
        Quantity.of(steerColumnEncoder, SteerPutEvent.UNIT_ENCODER) //
            .multiply(SteerConfig.GLOBAL.column2steer));
  }
}
