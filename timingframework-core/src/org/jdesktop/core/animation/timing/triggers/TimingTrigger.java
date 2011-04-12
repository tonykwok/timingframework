package org.jdesktop.core.animation.timing.triggers;

import org.jdesktop.core.animation.timing.Animator;
import org.jdesktop.core.animation.timing.TimingTarget;

import com.surelogic.ThreadSafe;

/**
 * A trigger that starts an animation when a timing event occurs. This class can
 * be useful in sequencing different animations. For example, one
 * {@link Animator} can be set to start when another ends using this trigger.
 * For example, to have <tt>anim2</tt> start when <tt>anim1</tt> ends, one might
 * write the following:
 * 
 * <pre>
 * TimingTrigger trigger = TimingTrigger.addTrigger(anim1, anim2, TimingTriggerEvent.STOP);
 * </pre>
 * 
 * @author Chet Haase
 */
@ThreadSafe
public class TimingTrigger extends Trigger implements TimingTarget {

  /**
   * Creates a non-auto-reversing TimingTrigger and adds it as a target to the
   * source Animator.
   * 
   * @param source
   *          the Animator that will be listened to for events to start the
   *          target Animator
   * @param target
   *          the Animator that will start when the event occurs
   * @param event
   *          the TimingTriggerEvent that will cause targetAnimator to start
   * @return TimingTrigger the resulting trigger
   * 
   * @see Animator#addTarget(TimingTarget)
   */
  public static TimingTrigger addTrigger(Animator source, Animator target, TimingTriggerEvent event) {
    return addTrigger(source, target, event, false);
  }

  /**
   * Creates a TimingTrigger and adds it as a target to the source Animator.
   * 
   * 
   * @param source
   *          the Animator that will be listened to for events to start the
   *          target Animator
   * @param target
   *          the Animator that will start when the event occurs
   * @param event
   *          the TimingTriggerEvent that will cause targetAnimator to start
   * @param autoReverse
   *          flag to determine whether the animator should stop and reverse
   *          based on opposite triggerEvents.
   * @return TimingTrigger the resulting trigger
   * 
   * @see Animator#addTarget(TimingTarget)
   */
  public static TimingTrigger addTrigger(Animator source, Animator target, TimingTriggerEvent event, boolean autoReverse) {
    TimingTrigger trigger = new TimingTrigger(target, event, autoReverse);
    source.addTarget(trigger);
    return trigger;
  }

  /**
   * Creates a non-auto-reversing TimingTrigger, which should be added to an
   * Animator which will generate the events sent to the trigger.
   */
  public TimingTrigger(Animator animator, TimingTriggerEvent event) {
    this(animator, event, false);
  }

  /**
   * Creates a TimingTrigger, which should be added to an Animator which will
   * generate the events sent to the trigger.
   */
  public TimingTrigger(Animator animator, TimingTriggerEvent event, boolean autoReverse) {
    super(animator, event, autoReverse);
  }

  //
  // TimingTarget implementation methods
  //

  @Override
  public void begin(Animator source) {
    fire(TimingTriggerEvent.START);
  }

  @Override
  public void end(Animator source) {
    fire(TimingTriggerEvent.STOP);
  }

  @Override
  public void repeat(Animator source) {
    fire(TimingTriggerEvent.REPEAT);
  }

  @Override
  public void reverse(Animator source) {
    fire(TimingTriggerEvent.REVERSE);
  }

  @Override
  public void timingEvent(Animator source, double fraction) {
    // Nothing to do
  }
}
