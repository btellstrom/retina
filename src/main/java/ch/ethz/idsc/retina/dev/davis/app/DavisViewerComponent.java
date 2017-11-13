// code by jph
package ch.ethz.idsc.retina.dev.davis.app;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.JComponent;

import ch.ethz.idsc.retina.dev.davis.data.DavisImuFrame;
import ch.ethz.idsc.retina.dev.davis.data.DavisImuFrameListener;
import ch.ethz.idsc.retina.util.ColumnTimedImage;
import ch.ethz.idsc.retina.util.ColumnTimedImageListener;
import ch.ethz.idsc.retina.util.IntRange;
import ch.ethz.idsc.retina.util.IntervalClock;
import ch.ethz.idsc.retina.util.TimedImageEvent;
import ch.ethz.idsc.retina.util.TimedImageListener;
import ch.ethz.idsc.retina.util.img.ImageCopy;
import ch.ethz.idsc.retina.util.img.ImageHistogram;
import ch.ethz.idsc.tensor.sca.Round;

public class DavisViewerComponent implements DavisImuFrameListener {
  // private static final Font FONT = new Font(Font.DIALOG, Font.PLAIN, 8);
  // ---
  BufferedImage sigImage = null;
  BufferedImage rstImage = null;
  BufferedImage difImage = null;
  private ImageCopy imageCopy = new ImageCopy();
  DavisImuFrame imuFrame = null;
  private final IntervalClock intervalClock = new IntervalClock();
  DavisTallyEvent davisTallyEvent;
  // Tensor displayEventCount = Array.zeros(3);
  public final ColumnTimedImageListener rstListener = new ColumnTimedImageListener() {
    @Override
    public void columnTimedImage(ColumnTimedImage columnTimedImage) { // TODO store reference
      if (!columnTimedImage.isComplete)
        System.err.println("rst incomplete");
      rstImage = columnTimedImage.bufferedImage;
    }
  };
  public final ColumnTimedImageListener sigListener = new ColumnTimedImageListener() {
    @Override
    public void columnTimedImage(ColumnTimedImage columnTimedImage) {
      if (!columnTimedImage.isComplete)
        System.err.println("sig incomplete");
      sigImage = columnTimedImage.bufferedImage;
    }
  };
  public final ColumnTimedImageListener difListener = new ColumnTimedImageListener() {
    @Override
    public void columnTimedImage(ColumnTimedImage columnTimedImage) {
      difImage = columnTimedImage.bufferedImage;
    }
  };
  public final TimedImageListener dvsImageListener = new TimedImageListener() {
    @Override
    public void timedImage(TimedImageEvent timedImageEvent) {
      imageCopy.update(timedImageEvent.bufferedImage);
    }
  };
  final JComponent jComponent = new JComponent() {
    @Override
    protected void paintComponent(Graphics graphics) {
      if (Objects.nonNull(rstImage))
        graphics.drawImage(rstImage, 0 * 240, 0, null);
      if (Objects.nonNull(sigImage))
        graphics.drawImage(sigImage, 1 * 240, 0, null);
      if (Objects.nonNull(difImage))
        graphics.drawImage(difImage, 2 * 240, 0, null);
      if (imageCopy.hasValue())
        graphics.drawImage(imageCopy.get(), 2 * 240, 180, null);
      // ---
      final int baseline_y = getSize().height - 20;
      if (Objects.nonNull(davisTallyEvent)) {
        DavisTallyEvent dte = davisTallyEvent;
        graphics.setColor(Color.LIGHT_GRAY);
        for (int h = 15; h < 100; h += 15) {
          double blub = Math.exp(h * 0.1) - 1;
          graphics.fillRect(0, baseline_y - h, dte.binLast, 1);
          graphics.drawString("" + Math.round(blub), dte.binLast, baseline_y - h);
        }
        graphics.setColor(Color.BLUE);
        for (int index = 0; index < dte.binLast; ++index) {
          int height = (int) Math.round(Math.log(dte.bin[index] + 1) * 10);
          graphics.fillRect(index, baseline_y - height, 1, height);
        }
        drawBar(graphics, baseline_y, dte.resetRange, Color.RED, "RST");
        drawBar(graphics, baseline_y, dte.imageRange, Color.GREEN, "SIG");
        graphics.setColor(Color.GRAY);
        graphics.drawString(dte.getDurationUs() + " [us]", dte.binLast, baseline_y);
      }
      if (Objects.nonNull(imuFrame)) {
        graphics.setColor(Color.GRAY);
        graphics.drawString( //
            String.format("%4.1f C", imuFrame.temperature), 70, 180 + 12 * 1);
        graphics.drawString( //
            imuFrame.accel().map(Round._2) + "[m*s^-2]", 0, 180 + 12 * 2);
        graphics.drawString( //
            imuFrame.gyro().map(Round._2) + "[rad*s^-1]", 0, 180 + 12 * 3);
      }
      if (Objects.nonNull(difImage)) {
        int[] bins = ImageHistogram.of(difImage);
        final int x_offset = 400;
        graphics.setColor(new Color(0, 128, 0));
        graphics.fillRect(x_offset, baseline_y, 256, 1);
        graphics.setColor(Color.GREEN);
        for (int index = 0; index < bins.length; ++index) {
          int height = (int) Math.round(Math.log(bins[index] + 1) * 10);
          graphics.fillRect(x_offset + index, baseline_y - height, 1, height);
        }
      }
      // ---
      graphics.setColor(Color.RED);
      graphics.drawString(String.format("%4.1f Hz", intervalClock.hertz()), 0, 190);
    }
  };

  private void drawBar(Graphics graphics, int y, IntRange intRange, Color color, String label) {
    if (Objects.nonNull(intRange)) {
      graphics.setColor(color);
      graphics.fillRect(intRange.min, y + 1, intRange.getWidth(), 2);
      graphics.setColor(Color.GRAY);
      graphics.drawString(label, intRange.min, y + 12);
    }
  }

  @Override // from DavisImuFrameListener
  public void imuFrame(DavisImuFrame davisImuFrame) {
    imuFrame = davisImuFrame;
  }
}
