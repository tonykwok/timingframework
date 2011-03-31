package org.jdesktop.core.animation.timing;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.jdesktop.core.animation.i18n.I18N;
import org.jdesktop.core.animation.timing.Animator.Direction;
import org.jdesktop.core.animation.timing.interpolators.LinearInterpolator;

import com.surelogic.ThreadSafe;

/**
 * A {@link TimingTarget} that enables automating animation of object
 * properties. Instances of this class should be added as a target of timing
 * events from an {@link Animator}. These events will be used to change a
 * specified property over time, according to how the PropertySetter is
 * constructed.
 * <p>
 * For example, here is an animation of the "background" property of some object
 * {@code obj} from blue to red over a period of one second:
 * 
 * <pre>
 * PropertySetter ps = new PropertySetter(obj, &quot;background&quot;, Color.BLUE, Color.RED);
 * Animator anim = new AnimatorBuilder().setDuration(1, TimeUnit.SECONDS).addTarget(ps).build();
 * anim.start();
 * </pre>
 * 
 * More complex animations can be created by passing in multiple values for the
 * property to take on, for example:
 * 
 * <pre>
 * PropertySetter ps = new PropertySetter(obj, &quot;background&quot;, Color.BLUE, Color.RED, Color.GREEN);
 * Animator anim = new AnimatorBuilder().setDuration(1, TimeUnit.SECONDS).addTarget(ps).build();
 * anim.start();
 * </pre>
 * 
 * It is also possible to define more involved and tightly-controlled steps in
 * the animation, including the times between the values and how the values are
 * interpolated by using the constructor that takes a {@link KeyFrames} object.
 * KeyFrames defines the fractional times at which an object takes on specific
 * values, the values to assume at those times, and the method of interpolation
 * between those values. For example, here is the same animation as above,
 * specified through KeyFrames, where the RED color will be set 10% of the way
 * through the animation (note that we are not setting an Interpolator, so the
 * timing intervals will use the default LinearInterpolator):
 * 
 * <pre>
 * KeyValues vals = KeyValues.create(Color.BLUE, Color.RED, Color.GREEN);
 * KeyTimes times = new KeyTimes(0.0f, .1f, 1.0f);
 * KeyFrames frames = new KeyFrames(vals, times);
 * PropertySetter ps = new PropertySetter(obj, &quot;background&quot;, frames);
 * Animator anim = new AnimatorBuilder().setDuration(1, TimeUnit.SECONDS).addTarget(ps).build();
 * anim.start();
 * </pre>
 * 
 * @author Chet Haase
 * @author Tim Halloran
 */
@ThreadSafe
public class PropertySetter extends TimingTargetAdapter {

  public static PropertySetter build(Object object, String propertyName, KeyFrames<?> keyFrames) {
    return new PropertySetter(object, propertyName, keyFrames, null, null);
  }

  public static <T> PropertySetter build(Object object, String propertyName, T... values) {
    return new PropertySetter(object, propertyName, new KeyFramesBuilder<T>().addFrames(values).build(), null, null);
  }

  public static <T> PropertySetter buildTo(Object object, String propertyName, T... values) {
    return new PropertySetter(object, propertyName, null, null, values);
  }

  public static <T> PropertySetter buildTo(Object object, String propertyName, Interpolator interpolator, T... values) {
    return new PropertySetter(object, propertyName, null, interpolator, values);
  }

  private final Object f_object;
  private final String f_propertyName;
  private final AtomicReference<KeyFrames<?>> f_keyFrames = new AtomicReference<KeyFrames<?>>();
  private final Object[] f_toValues;
  private final Interpolator f_toInterpolator;
  private final Method f_propertySetter;
  private final Method f_propertyGetter;

  private PropertySetter(Object object, String propertyName, KeyFrames<?> keyFrames, Interpolator toInterpolator, Object[] toValues) {
    if (object == null)
      throw new IllegalArgumentException(I18N.err(1, "object"));
    f_object = object;
    if (propertyName == null)
      throw new IllegalArgumentException(I18N.err(1, "propertyName"));
    f_propertyName = propertyName;

    if ((keyFrames == null && toValues == null) || (keyFrames != null && toValues != null))
      throw new IllegalArgumentException(I18N.err(31));
    if (keyFrames != null) {
      f_keyFrames.set(keyFrames);
      f_toInterpolator = null;
      f_toValues = null;
    } else {
      f_toInterpolator = toInterpolator == null ? LinearInterpolator.getInstance() : toInterpolator;
      f_toValues = toValues;
    }

    /*
     * Find the setter method.
     */
    final String firstChar = f_propertyName.substring(0, 1);
    final String remainder = f_propertyName.substring(1);
    final String propertySetterName = "set" + firstChar.toUpperCase(Locale.ENGLISH) + remainder;
    try {
      final PropertyDescriptor pd = new PropertyDescriptor(f_propertyName, f_object.getClass(), null, propertySetterName);
      f_propertySetter = pd.getWriteMethod();
    } catch (IntrospectionException e) {
      throw new IllegalArgumentException(I18N.err(30, propertySetterName, propertyName, object.toString()), e);
    }
    /*
     * Find the getter method, but we only need it for "to" animations
     */
    if (isToAnimation()) {
      final String propertyGetterName = "get" + firstChar.toUpperCase(Locale.ENGLISH) + remainder;
      try {
        final PropertyDescriptor pd = new PropertyDescriptor(f_propertyName, f_object.getClass(), propertyGetterName, null);
        f_propertyGetter = pd.getReadMethod();
      } catch (IntrospectionException e) {
        throw new IllegalArgumentException(I18N.err(30, propertyGetterName, propertyName, object.toString()), e);
      }
    } else {
      f_propertyGetter = null;
    }
  }

  /**
   * This method is sets an initial value for the animation if appropriate; this
   * accounts for "to" animations, which need to start from the current value.
   * <p>
   * This method is not intended for use by application code.
   */
  @Override
  public void begin(Animator source) {
    if (isToAnimation()) {
      try {
        final Object startValue = f_propertyGetter.invoke(f_object);
        f_keyFrames.set(new KeyFramesBuilder<Object>(startValue).setInterpolator(f_toInterpolator).addFrames(f_toValues).build());
      } catch (Exception e) {
        throw new IllegalStateException(I18N.err(32, f_propertyGetter.getName(), f_object.toString()), e);
      }
    }
  }

  /**
   * This invokes the property-setting method (as specified by the
   * {@code propertyName} passed to the constructor) with the appropriate value
   * of the property given the range of values in the {@link KeyValues} object
   * and the fraction of the timing cycle that has elapsed.
   * <p>
   * This method is not intended for use by application code.
   */
  @Override
  public void timingEvent(double fraction, Direction direction, Animator source) {
    try {
      f_propertySetter.invoke(f_object, f_keyFrames.get().getInterpolatedValueAt(fraction));
    } catch (Exception e) {
      throw new IllegalStateException(I18N.err(32, f_propertySetter.getName(), f_object.toString()), e);
    }
  }

  /**
   * Utility method for determining whether this is a "to" animation.
   */
  private boolean isToAnimation() {
    return (f_toValues != null);
  }
}
