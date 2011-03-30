package org.jdesktop.core.animation.timing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jdesktop.core.animation.i18n.I18N;
import org.jdesktop.core.animation.timing.evaluators.KnownEvaluators;
import org.jdesktop.core.animation.timing.interpolators.LinearInterpolator;

import com.surelogic.NotThreadSafe;

@NotThreadSafe
public class KeyFramesBuilder<T> {

  private Evaluator<T> f_evaluator = null;
  private final List<T> f_values = new ArrayList<T>();
  private final LinkedList<Double> f_times = new LinkedList<Double>();
  private final List<Interpolator> f_interpolators = new ArrayList<Interpolator>();

  public KeyFramesBuilder() {
    // Nothing to do
  }

  public KeyFramesBuilder(T startValue) {
    f_values.add(startValue);
    f_times.add(Double.valueOf(0));
    f_interpolators.add(null);
  }

  public KeyFramesBuilder<T> addFrame(T value) {
    f_values.add(value);
    final double atTime = f_times.getLast();
    f_times.add(atTime + ((1.0 - atTime) / 2.0));
    f_interpolators.add(LinearInterpolator.getInstance());
    return this;
  }

  public KeyFramesBuilder<T> addFrame(T value, double atTime) {
    f_values.add(value);
    f_times.add(atTime);
    f_interpolators.add(LinearInterpolator.getInstance());
    return this;
  }

  public KeyFramesBuilder<T> addFrame(T value, double atTime, Interpolator interpolator) {
    f_values.add(value);
    f_times.add(atTime);
    f_interpolators.add(interpolator);
    return this;
  }

  public KeyFramesBuilder<T> setEvaluator(Evaluator<T> evaluator) {
    f_evaluator = evaluator;
    return this;
  }

  public KeyFrames<T> build() {
    final int frameCount = f_values.size();
    if (frameCount < 2)
      throw new IllegalArgumentException(I18N.err(20));
    if (f_times.size() != frameCount)
      throw new IllegalArgumentException(I18N.err(21, frameCount, f_times.size()));
    if (f_interpolators.size() != frameCount)
      throw new IllegalArgumentException(I18N.err(22, frameCount, f_interpolators.size()));
    /*
     * Check for nulls
     */
    int index = 0;
    for (T value : f_values) {
      if (value == null)
        throw new IllegalArgumentException(I18N.err(23, index));
      index++;
    }
    index = 0;
    final double[] times = new double[frameCount];
    for (Double time : f_times) {
      if (time == null)
        throw new IllegalArgumentException(I18N.err(24, index));
      times[index] = time;
      index++;
    }
    index = 0;
    final Interpolator[] interpolators = new Interpolator[frameCount];
    for (Interpolator interpolator : f_interpolators) {
      if (index != 0 && interpolator == null)
        throw new IllegalArgumentException(I18N.err(25, index));
      interpolators[index] = index == 0 ? null : interpolator;
      index++;
    }
    /*
     * Change the first key time to zero, unless it already is zero.
     */
    if (f_times.getFirst() != 0) {
      f_times.removeFirst();
      f_times.addFirst(Double.valueOf(0));
    }
    /*
     * Change the last key time to one, unless it already is one.
     */
    if (f_times.getLast() != 1) {
      f_times.removeLast();
      f_times.addLast(Double.valueOf(1));
    }
    /*
     * Check that key times are less than one and that they increase.
     */
    double prevTime = 0;
    for (double time : times) {
      if (time < prevTime)
        throw new IllegalArgumentException(I18N.err(26, f_times.toString()));
      prevTime = time;
    }
    /*
     * Try to find an evaluator unless one was set.
     */
    if (f_evaluator == null) {
      @SuppressWarnings("unchecked")
      final Class<T> c = (Class<T>) f_values.get(0).getClass();
      f_evaluator = KnownEvaluators.getInstance().getEvaluatorFor(c);
    }
    @SuppressWarnings("unchecked")
    final KeyFrames<T> result = new KeyFrames<T>((T[]) f_values.toArray(), times, interpolators, f_evaluator);
    return result;
  }
}
